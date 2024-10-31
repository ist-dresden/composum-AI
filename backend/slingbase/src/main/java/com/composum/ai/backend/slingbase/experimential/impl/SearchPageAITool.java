package com.composum.ai.backend.slingbase.experimential.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.slingbase.RAGService;
import com.composum.ai.backend.slingbase.experimential.AITool;
import com.composum.ai.backend.slingbase.model.SlingGPTExecutionContext;
import com.google.gson.Gson;

@Component(service = AITool.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SearchPageAITool.Config.class)
public class SearchPageAITool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(SearchPageAITool.class);
    private Config config;
    private final Gson gson = new Gson();

    @Reference
    private RAGService ragService;

    @Override
    public @Nonnull String getName(@Nullable Locale locale) {
        return "Search Page";
    }

    @Override
    public @Nonnull String getDescription(@Nullable Locale locale) {
        return "Search for a page";
    }

    @Override
    public @Nonnull String getToolName() {
        return "search_page";
    }

    @Override
    public @Nonnull String getToolDeclaration() {
        return "{\n" +
                "  \"type\": \"function\",\n" +
                "  \"function\": {\n" +
                "    \"name\": \"search_page\",\n" +
                "    \"description\": \"Search for titles and JCR paths for pages that best match the given query.\",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"query\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"description\": \"The search query\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\"query\"],\n" +
                "      \"additionalProperties\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"strict\": true\n" +
                "}";
    }

    @Override
    public boolean isAllowedFor(@Nonnull Resource resource) {
        return true;
    }

    /**
     * Does a query with lucene and then rates the results with the embedding.
     */
    @Override
    public @Nonnull String execute(@Nullable String arguments, @Nonnull Resource resource,
                                   @Nullable GPTCompletionCallback.GPTToolExecutionContext context) {
        try {
            SlingHttpServletRequest request = ((SlingGPTExecutionContext) context).getRequest();
            SlingHttpServletResponse response = ((SlingGPTExecutionContext) context).getResponse();
            Map parsedArguments = gson.fromJson(arguments, Map.class);
            String query = (String) parsedArguments.get("query");
            if (query == null || query.isEmpty()) {
                return "Missing query parameter";
            }
            ResourceResolver resolver = request.getResourceResolver();
            // go up to site resource starting from resource
            Resource rootResource = resolver.getResource(resource.getPath()); // original resource resolver is already closed.
            while (rootResource != null && rootResource.getPath().split("/").length > config.siteLevel() + 1) {
                rootResource = rootResource.getParent();
            }

            List<String> paths = ragService.searchRelated(rootResource, query, config.resultCount());
            List<Resource> resources = paths.stream().map(resolver::getResource).collect(Collectors.toList());
            List<Resource> ordered = ragService.orderByEmbedding(query, resources, request, response, rootResource);
            List<String> resultPaths = ordered.stream().map(Resource::getPath)
                    .map(path -> path.replaceAll("/jcr:content$", ""))
                    .collect(Collectors.toList());
            LOG.debug("Search page AI tool found for '{}' : {}", query, resultPaths);

            // collect titles (properties "jcr:title" / "title") of resource and make itemized list of markdown links
            StringBuilder result = new StringBuilder("Here are the JCR paths for the " + config.resultCount() +
                    " pages best matching the query.\n\n");
            for (String path : resultPaths) {
                Resource res = resolver.getResource(path);
                if (res != null) {
                    res = res.getChild("jcr:content") != null ? res.getChild("jcr:content") : res;
                    String title = res.getValueMap().get("jcr:title",
                            res.getValueMap().get("title", String.class));
                    if (title == null || title.startsWith("/")) {
                        result.append("- ").append(path).append("\n");
                    } else {
                        result.append("- ").append(title).append(": ").append(path).append("\n");
                    }
                } else {
                    result.append("- ").append(path).append("\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            LOG.error("Error in search page AI tool", e);
            return "Error in search page AI tool: " + e;
        }
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @ObjectClassDefinition(name = "Composum AI Tool Search Pages",
            description = "Provides the AI with a tool to search for page paths. Needs a lucene index for all pages." +
                    "If there is no configuration the tool is not active.")
    public @interface Config {

        @AttributeDefinition(name = "Result count", description = "The number of results to return. Default is 20.")
        int resultCount() default 20;

        @AttributeDefinition(name = "Site level", description = "The number of path segments a site has, used to identify the site root. Default is 2, for sites like /content/my-site.")
        int siteLevel() default 2;

    }
}
