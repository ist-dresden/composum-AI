package com.composum.ai.backend.slingbase.experimential.impl;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.removeStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.slingbase.experimential.AITool;
import com.composum.ai.backend.slingbase.model.SlingGPTExecutionContext;
import com.google.gson.Gson;

@Component(service = AITool.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ModifyPageReadTool.Config.class)
public class ModifyPageReadTool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(ModifyPageReadTool.class);

    /**
     * Heuristic to detect text attributes.
     */
    public static final Pattern PATTERN_TWO_SEPARATE_WHITESPACE = Pattern.compile("\\s\\S+\\s+");

    private Config config;
    private Gson gson = new Gson();

    @Override
    public @Nonnull String getName(@Nullable Locale locale) {
        return "Modify Page Read";
    }

    @Override
    public @Nonnull String getDescription(@Nullable Locale locale) {
        return "Reads all properties of a page, including path to the component, property name, and value (text or richtext).";
    }

    @Override
    public @Nonnull String getToolName() {
        return "modify_page_read";
    }

    @Override
    public @Nonnull String getToolDeclaration() {
        return "{\n" +
                "  \"type\": \"function\",\n" +
                "  \"function\": {\n" +
                "    \"name\": \"modify_page_read\",\n" +
                "    \"description\": \"Read all properties of the current page, including path to the component, property name, and value.\",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {},\n" +
                "      \"required\": [],\n" +
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

    @Override
    public @Nonnull String execute(@Nullable String arguments, @Nonnull Resource resource,
                                   @Nullable GPTCompletionCallback.GPTToolExecutionContext context) {
        try {
            SlingHttpServletRequest request = ((SlingGPTExecutionContext) context).getRequest();
            Resource contentResource = request.getResourceResolver().getResource(resource.getPath());

            if (!contentResource.getPath().matches(config.allowedPathsRegex())) {
                return "Path not allowed";
            }
            if (!contentResource.getPath().contains("/jcr:content")) {
                contentResource = contentResource.getChild("jcr:content");
                if (contentResource == null) {
                    return "No jcr:content child";
                }
            }
            if (!contentResource.getPath().contains("/jcr:content")) {
                return "Path does not contain jcr:content";
            }


            // Collect properties
            PageProperties pageProperties = new PageProperties();
            String removePrefix = contentResource.getPath();
            descendantsStream(contentResource).forEach(r -> collectProperties(removePrefix,r, pageProperties));
            String json = gson.toJson(pageProperties);
            LOG.debug("Page properties for {}: {}", resource.getPath(), json);
            return json;
        } catch (Exception e) {
            LOG.error("Error in modify page read tool", e);
            return "Error in modify page read tool: " + e;
        }
    }

    private void collectProperties(String removePrefix, Resource resource, PageProperties pageProperties) {
        ComponentProperties component = new ComponentProperties();
        component.componentPath = resource.getPath().substring(removePrefix.length());
        component.componentPath = defaultString(removeStart(component.componentPath, "/"), ".");
        component.slingResourceType = resource.getResourceType();
        Resource componentResource = resource.getResourceResolver().getResource(component.slingResourceType);
        if (componentResource != null) {
            component.componentTitle = componentResource.getValueMap().get("jcr:title", "");
        }
        resource.getValueMap().forEach((key, value) -> {
            if (key.startsWith("ai_") || key.startsWith("lc_")) return;
            if (value instanceof String && PATTERN_TWO_SEPARATE_WHITESPACE.matcher((String) value).find()) {
                component.properties.put(key, (String) value);
            }
        });
        if (!component.properties.isEmpty()) {
            pageProperties.components.add(component);
        }
    }

    /**
     * Returns a stream that goes through all descendants of a resource, parents come before
     * their children.
     *
     * @param resource a resource or null
     * @return a stream running through the resource and it's the descendants, not null
     */
    @Nonnull
    public static Stream<Resource> descendantsStream(@Nullable Resource resource) {
        if (resource == null) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(resource),
                StreamSupport.stream(resource.getChildren().spliterator(), false)
                        .flatMap(ModifyPageReadTool::descendantsStream));
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

    @ObjectClassDefinition(name = "Composum AI Tool Modify Page Read",
            description = "Provides the AI with a tool to read properties of a page. Needs a lucene index for all pages." +
                    "If there is no configuration, the tool is not active.")
    public @interface Config {

        @AttributeDefinition(name = "Allowed paths regex",
                description = "A regex to match the paths that this tool is allowed to be used on. Default: /content/.*")
        String allowedPathsRegex() default "/content/.*";

    }

    protected static class PageProperties {
        List<ComponentProperties> components = new ArrayList<>();
    }

    protected static class ComponentProperties {
        String componentPath;
        String slingResourceType;
        String componentTitle;
        Map<String, String> properties = new java.util.HashMap<>();
    }
}
