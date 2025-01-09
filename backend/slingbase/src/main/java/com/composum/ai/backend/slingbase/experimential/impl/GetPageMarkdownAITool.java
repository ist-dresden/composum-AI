package com.composum.ai.backend.slingbase.experimential.impl;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
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
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.experimential.AITool;
import com.composum.ai.backend.slingbase.model.SlingGPTExecutionContext;
import com.google.gson.Gson;

@Component(service = AITool.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GetPageMarkdownAITool.Config.class)
public class GetPageMarkdownAITool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(GetPageMarkdownAITool.class);
    private Config config;
    private Gson gson = new Gson();

    @Reference
    private ApproximateMarkdownService markdownService;

    @Override
    public @Nonnull String getName(@Nullable Locale locale) {
        return "Get Text of Page";
    }

    @Override
    public @Nonnull String getDescription(@Nullable Locale locale) {
        return "Returns a markdown representation of the text of a given page";
    }

    @Override
    public @Nonnull String getToolName() {
        return "get_pagetext";
    }

    @Override
    public @Nonnull String getToolDeclaration() {
        return "{\n" +
                "  \"type\": \"function\",\n" +
                "  \"function\": {\n" +
                "    \"name\": \"get_pagetext\",\n" +
                "    \"description\": \"Get the text of a page with a given path.\",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"path\": {\n" +
                "          \"type\": \"string\",\n" +
                "          \"description\": \"The path of the page to get the text of.\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\"path\"],\n" +
                "      \"additionalProperties\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"strict\": true\n" +
                "}";
    }

    @Override
    public boolean isAllowedFor(@Nonnull Resource resource) {
        return config != null && config.allowedPathsRegex() != null &&
                resource.getPath().matches(config.allowedPathsRegex());
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
            String path = (String) parsedArguments.get("path");
            if (path == null || path.isEmpty()) {
                return "Missing path parameter";
            }
            if (!path.matches(config.allowedPathsRegex())) {
                return "Path not allowed";
            }
            ResourceResolver resolver = request.getResourceResolver();
            Resource pathResource = resolver.getResource(path);
            String markdown = markdownService.approximateMarkdown(pathResource, request, response);
            LOG.debug("Markdown of {} : {}", path, StringUtils.abbreviate(markdown, 80));
            return markdown;
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

    @ObjectClassDefinition(name = "Composum AI Tool Get Page Markdown",
            description = "Provides the AI with a tool to search for page paths. Needs a lucene index for all pages." +
                    "If there is no configuration, the tool is not active.")
    public @interface Config {

        @AttributeDefinition(name = "Allowed paths regex",
                description = "A regex to match the paths that this tool is allowed to be used on. Default: /content/.*")
        String allowedPathsRegex() default "/content/.*";

    }
}
