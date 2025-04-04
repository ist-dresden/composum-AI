package com.composum.ai.backend.slingbase.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.PageCachedValueService;
import com.composum.ai.backend.slingbase.RAGService;

/**
 * Basic services for retrieval augmented generation (RAG).
 */
@Component(service = RAGService.class)
public class RAGServiceImpl implements RAGService {

    private static final Logger LOG = LoggerFactory.getLogger(RAGServiceImpl.class);

    @Reference
    protected ApproximateMarkdownService markdownService;

    @Reference
    protected GPTEmbeddingService embeddingService;

    @Reference
    protected AIConfigurationService aiConfigurationService;

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected PageCachedValueService pageCachedValueService;

    protected final AtomicLong requestCounter = new AtomicLong(System.currentTimeMillis() / 2);

    @Override
    @Nonnull
    public List<String> searchRelated(@Nullable Resource root, @Nullable String querytext, int limit) {
        if (root == null || querytext == null || limit <= 0) {
            return Collections.emptyList();
        }
        int restOfLimit = limit * 5 / 4 + 3; // a little larger since there might be exact and inexact matches

        String exactQuery = "\"" + querytext.replaceAll("\"", "") + "\"";
        String normalizedQuery = normalize(querytext);

        @NotNull List<String> exactResult = Collections.emptyList();
        try {
            exactResult = containsQuery(root, exactQuery, restOfLimit);
        } catch (RepositoryException e) {
            LOG.error("Error searching for exact query {}", exactQuery, e);
        }
        restOfLimit -= exactResult.size();
        LOG.trace("Exact query result: {}", exactResult);

        @NotNull List<String> normalizedResult = Collections.emptyList();
        try {
            normalizedResult = containsQuery(root, normalizedQuery, restOfLimit);
        } catch (RepositoryException e) {
            LOG.error("Error searching for normalized query {}", normalizedQuery, e);
        }
        LOG.trace("Normalized query result: {}", normalizedResult);

        List<String> result = new ArrayList<>(exactResult);
        result.addAll(normalizedResult);
        result = result.stream().distinct().limit(limit).collect(Collectors.toList());
        return result;
    }

    protected @NotNull List<String> containsQuery(@NotNull Resource root, @NotNull String querytext, int restOfLimit) throws RepositoryException {
        List<String> result = new ArrayList<>();
        ResourceResolver resolver = root.getResourceResolver();
        final Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        String statement = "SELECT [jcr:path], [jcr:score] FROM [cq:Page] AS page WHERE " +
                "ISDESCENDANTNODE(page, '" + root.getPath() + "') " +
                "AND CONTAINS(page.*, $queryText) " +
                "ORDER BY [jcr:score] DESC";
        // equivalent Composum Nodes query template for testing
        // SELECT [jcr:path], [jcr:score] FROM [cq:Page] AS page WHERE ISDESCENDANTNODE(page, '${root_path.path}') AND CONTAINS(page.*, '${text.3}') ORDER BY [jcr:score] DESC
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);
        query.bindValue("queryText", session.getValueFactory().createValue(querytext));
        query.setLimit(restOfLimit);
        LOG.trace("Executing query:\n{}\nwith\n{}", query.getStatement(), querytext);
        QueryResult queryResult = query.execute();
        for (RowIterator rowIterator = queryResult.getRows(); rowIterator.hasNext(); ) {
            if (restOfLimit-- <= 0) {
                return result;
            }
            Row row = rowIterator.nextRow();
            String path = row.getValue("jcr:path").getString() + "/jcr:content";
            LOG.trace("Found path {} with score {}", path, row.getValue("jcr:score").getDouble());
            if (!result.contains(path)) {
                result.add(path);
            }
        }
        return result;
    }

    /**
     * Turn it into a query for the words mentioned in there - that is, remove all meta characters for CONTAINS queries:
     * AND, OR, words prefixed with -, quotes, backslashes. We use an OR query to find pages with as many words as possible.
     */
    @Nonnull
    protected String normalize(@Nonnull String querytext) {
        return Arrays.stream(querytext.split("\\s+"))
                .map(s -> s.replaceAll("[\"\\\\']", ""))
                .map(s -> s.replaceAll("^-+", ""))
                .filter(s -> !s.equals("OR"))
                .filter(s -> !s.equals("AND"))
                .collect(Collectors.joining(" OR "));
    }

    /**
     * Finds the resources whose markdown approximation has embeddings that are the most similar to the querytext embedding.
     * Useable e.g. as filter after {@link #searchRelated(Resource, String, int)}.
     */
    @Override
    @Nonnull
    public List<Resource> orderByEmbedding(@Nullable String querytext, @Nonnull List<Resource> resources,
                                           @NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
                                           @NotNull Resource rootResource) {
        Map<String, String> textToPath = new TreeMap<>();
        Map<String, Resource> textToResource = new TreeMap<>();
        for (Resource resource : resources) {
            String markdown = markdownService.approximateMarkdown(resource, request, response);
            textToPath.put(markdown, resource.getPath());
            textToResource.put(markdown, resource);
        }
        GPTConfiguration config = aiConfigurationService.getGPTConfiguration(rootResource.getResourceResolver(), rootResource.getPath());
        List<String> relatedTexts = embeddingService.findMostRelated(querytext, new ArrayList<>(textToPath.keySet()),
                Integer.MAX_VALUE, config, getEmbeddingsCache(textToResource));
        Map<String, Resource> pathToResource = resources.stream().collect(Collectors.toMap(r -> r.getPath(), r -> r));
        List<Resource> result = relatedTexts.stream()
                .map(textToPath::get)
                .map(pathToResource::get)
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Answer a question with RAG from the given resources, e.g. found with {@link #searchRelated(Resource, String, int)}.
     *
     * @param querytext     the query text
     * @param resources     the list of resources to answer from
     * @param request       the request to use when determining the markdown approximation - not modified
     * @param response      the response to use when determining the markdown approximation - not modified
     * @param rootResource  the root resource to find GPT configuration from
     * @param limitRagTexts the maximum number of RAG texts to consider
     * @return the answer text
     */
    @Override
    public String ragAnswer(@Nullable String querytext, @Nonnull List<Resource> resources,
                            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response,
                            @NotNull Resource rootResource, int limitRagTexts) {
        long id = requestCounter.incrementAndGet();
        Map<String, String> textToPath = new TreeMap<>();
        Map<String, Resource> textToResource = new TreeMap<>();
        for (Resource resource : resources) {
            String markdown = markdownService.approximateMarkdown(resource, request, response);
            textToPath.put(markdown, resource.getPath());
            textToResource.put(markdown, resource);
        }
        GPTConfiguration config = aiConfigurationService.getGPTConfiguration(rootResource.getResourceResolver(), rootResource.getPath());
        List<String> bestMatches = embeddingService.findMostRelated(querytext,
                new ArrayList<>(textToPath.keySet()), limitRagTexts, config, getEmbeddingsCache(textToResource));
        LOG.debug("ragAnswer: query for {} is {}", id, request);
        GPTChatRequest chatRequest = new GPTChatRequest(config);
        Collections.reverse(bestMatches); // make the most relevant last, near the actual question
        int limit = bestMatches.size();
        while (limit >= 1) {
            try {
                for (String text : bestMatches.subList(0, limit)) {
                    String textPath = textToPath.get(text);
                    chatRequest.addMessage(GPTMessageRole.USER, "For answering my question later, retrieve the text of the possibly relevant page: "
                            + textPath.replaceAll("/jcr:content", ".html"));
                    chatRequest.addMessage(GPTMessageRole.ASSISTANT, text);
                    LOG.debug("ragAnswer: Using for {} path {}", id, textPath);
                }
                chatRequest.addMessage(GPTMessageRole.USER, "Considering this information, please answer the following as Markdown text without enumeration, including links to the relevant retrieved pages above:\n\n" + querytext);
                LOG.debug("ragAnswer: request {} : {}", id, request);
                String answer = chatCompletionService.getSingleChatCompletion(chatRequest);
                LOG.debug("ragAnswer: response {} : {}", id, answer);
                return answer;
            } catch (GPTException.GPTContextLengthExceededException e) {
                // retry with lower number of texts
                limit = limit * 2 / 3;
                LOG.info("ragAnswer: retrying with lower number of texts because of content length exceeded exception: {}", limit);
            }
        }
        if (limit == 0 && !bestMatches.isEmpty()) {
            return "(No answer: context length exceeded.)";
        }
        return "(No answer found).";
    }

    protected GPTEmbeddingService.EmbeddingsCache getEmbeddingsCache(final Map<String, Resource> textToResource) {
        if (pageCachedValueService == null) {
            return null;
        }
        final String key = "pagemarkdown-embedding-" + chatCompletionService.getEmbeddingsModel();
        return new GPTEmbeddingService.EmbeddingsCache() {

            @Override
            public String getCachedEmbedding(String text) {
                Resource resource = textToResource.get(text);
                return resource != null ? pageCachedValueService.getPageCachedValue(key, resource) : null;
            }

            @Override
            public void putCachedEmbedding(String text, String embedding) {
                Resource resource = textToResource.get(text);
                if (resource != null) {
                    pageCachedValueService.putPageCachedValue(key, resource, embedding);
                }
            }
        };
    }


    /**
     * Processes a query to have the AI suggest a couple of search keywords for use with the other methods that might find the most relevant results.
     *
     * @param querytext   the query text for which we find keywords
     * @param rootResource  the root resource to find GPT configuration from
     * @return a list of keywords
     * @throws RepositoryException
     */
    @Override
    @Nonnull
    public List<String> collectSearchKeywords(@Nullable String querytext, @Nonnull Resource rootResource) throws RepositoryException {
        GPTConfiguration config = aiConfigurationService.getGPTConfiguration(rootResource.getResourceResolver(), rootResource.getPath());
        GPTChatRequest request = new GPTChatRequest(config)
                .addMessage(GPTMessageRole.SYSTEM, "Print up to 7 keywords to search for in documents with a BM25 algorithm which are likely to appear in documents answering the users question, but not in documents irrelevant to that.\n" +
                        "The keywords should be selected to maximize the relevance of the retrieved high scoring documents, specifically aiming to answer the user's question.\n" +
                        "The keywords can be words from the users question, synonyms or other words you would expect to be present especially in a document answering the question.\n" +
                        "Print the keywords (single words) as comma separated list.")
                .addMessage(GPTMessageRole.USER, querytext);
        String result = chatCompletionService.getSingleChatCompletion(request);
        LOG.debug("collectSearchKeywords: for '{}' got '{}'", querytext, result);
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(result.trim().split("\\s*,\\s*"));
    }

}
