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

import com.composum.ai.backend.slingbase.RAGService;
import com.composum.ai.backend.slingbase.experimential.AITool;
import com.google.gson.Gson;

@Component(service = AITool.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SearchPageAITool.Config.class)
public class SearchPageAITool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(SearchPageAITool.class);
    private Config config;
    private Gson gson = new Gson();

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
                "    \"description\": \"Search for a page that best matches the given query\",\n" +
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
                                   @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        try {
            Map parsedArguments = gson.fromJson(arguments, Map.class);
            String query = (String) parsedArguments.get("query");
            if (query == null || query.isEmpty()) {
                return "Missing query parameter";
            }
            ResourceResolver resolver = resource.getResourceResolver();
            Resource rootResource = resolver.getResource(config.rootPath());
            List<String> paths = ragService.searchRelated(rootResource, query, 20);
            List<Resource> resources = paths.stream().map(resolver::getResource).collect(Collectors.toList());
            List<Resource> ordered = ragService.orderByEmbedding(query, resources, request, response, rootResource);
            List<String> result = ordered.stream().map(Resource::getPath).collect(Collectors.toList());
            LOG.debug("Search page AI tool found for '{}' : {}", query, result);
            return gson.toJson(result);
        } catch (Exception e) {
            LOG.error("Error in search page AI tool", e);
            return "Error in search page AI tool: " + e;
        }
    }

    // activate and deactivate methods
    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @ObjectClassDefinition(name = "Composum AI Tool Search Pageq",
            description = "Provides the AI with a tool to search for page paths. Needs a lucene index for all pages.")
    public @interface Config {

        @AttributeDefinition(name = "Root path", description = "The root path to search for pages. Default is /content.")
        String rootPath() default "/content";

    }
}
