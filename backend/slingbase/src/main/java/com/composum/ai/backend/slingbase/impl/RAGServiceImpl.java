package com.composum.ai.backend.slingbase.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
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
        String statement = "SELECT [jcr:path], [jcr:score] FROM [nt:base] AS content WHERE " +
                "ISDESCENDANTNODE(content, '" + root.getPath() + "') " +
                "AND NAME(content) = 'jcr:content' " +
                "AND CONTAINS(content.*, $queryText) " +
                "ORDER BY [jcr:score] DESC";
        // equivalent Composum Nodes query template for testing
        // SELECT [jcr:path], [jcr:score] FROM [nt:base] AS content WHERE ISDESCENDANTNODE(content, '${root_path.path}')  AND NAME(content) = 'jcr:content' AND CONTAINS(content.*, '${text.3}') ORDER BY [jcr:score] DESC
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
            String path = row.getValue("jcr:path").getString();
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
        Map<String, String> texts = new TreeMap<>();
        for (Resource resource : resources) {
            texts.put(resource.getPath(), markdownService.approximateMarkdown(resource, request, response));
        }
        List<String> paths = new ArrayList<>(texts.keySet());
        GPTConfiguration config = aiConfigurationService.getGPTConfiguration(rootResource.getResourceResolver(), rootResource.getPath());
        List<String> markdownTexts = paths.stream().map(texts::get).collect(Collectors.toList());
        markdownTexts.add(0, querytext);
        List<float[]> embeddings = embeddingService.getEmbeddings(markdownTexts, config);
        float[] queryEmbedding = embeddings.get(0);
        embeddings.remove(0);
        // now markdown of paths.get(i) has embedding embeddings.get(i)
        // we want to order by cosine similarity to query embedding
        Map<String, Double> pathToSimilarity = new HashMap<>();
        for (int i = 0; i < embeddings.size(); i++) {
            pathToSimilarity.put(paths.get(i), cosineSimilarity(queryEmbedding, embeddings.get(i)));
        }
        return pathToSimilarity.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(e -> resources.get(paths.indexOf(e.getKey())))
                .collect(Collectors.toList());
    }

    protected double cosineSimilarity(float[] embedding1, float[] embedding2) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
