package com.composum.ai.backend.slingbase.experimential.impl;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
@Designate(ocd = ModifyPageWriteTool.Config.class)
public class ModifyPageWriteTool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(ModifyPageWriteTool.class);
    private Config config;
    private Gson gson = new Gson();

    @Override
    public @Nonnull String getName(@Nullable Locale locale) {
        return "Modify Page Write";
    }

    @Override
    public @Nonnull String getDescription(@Nullable Locale locale) {
        return "Writes properties to a page, including path to the component, property name, and value (text or richtext).";
    }

    @Override
    public @Nonnull String getToolName() {
        return "modify_page_write";
    }

    @Override
    public @Nonnull String getToolDeclaration() {
        return "{\n" +
                "  \"type\": \"function\",\n" +
                "  \"function\": {\n" +
                "    \"name\": \"modify_page_write\",\n" +
                "    \"description\": \"Writes properties to the current page, including path to the component, property name, and value. Can only write properties that already exist as determined by modify_page_read - use operation modify_page_read to determine the current structure of the page before using this. Always use well formed HTML richtext values for properties that contained HTML richtext when reading. Only use this tool if the user explicitly requested to modify the page! \",\n" +
                "    \"parameters\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"components\": {\n" +
                "          \"type\": \"array\",\n" +
                "          \"items\": {\n" +
                "            \"type\": \"object\",\n" +
                "            \"properties\": {\n" +
                "              \"componentPath\": { \"type\": \"string\" },\n" +
                "              \"properties\": { \"type\": \"object\" }\n" +
                "            },\n" +
                "            \"required\": [\"componentPath\"],\n" +
                "            \"additionalProperties\": false\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\"components\"],\n" +
                "      \"additionalProperties\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"strict\": true\n" +
                "}";
    }

    @Override
    public boolean isAllowedFor(@Nullable Resource resource) {
        return config != null && config.allowedPathsRegex() != null && resource != null &&
                resource.getPath().matches(config.allowedPathsRegex());
    }

    @Override
    public @Nonnull String execute(@Nullable String arguments, @Nonnull Resource resource,
                                   @Nullable GPTCompletionCallback.GPTToolExecutionContext context) {
        try {
            if (!resource.getPath().matches(config.allowedPathsRegex())) {
                return "Path not allowed";
            }

            ModifyPageReadTool.PageProperties pageProperties = gson.fromJson(arguments, ModifyPageReadTool.PageProperties.class);
            LOG.debug("Page properties for {}: {}", resource.getPath(), arguments);
            if (pageProperties == null || pageProperties.components.isEmpty()) {
                return "Invalid arguments: no 'components' found.";
            }
            SlingHttpServletRequest request = ((SlingGPTExecutionContext) context).getRequest();

            ResourceResolver resolver = request.getResourceResolver();
            Resource contentResource = request.getResourceResolver().getResource(resource.getPath());
            if (!contentResource.getPath().contains("/jcr:content")) {
                contentResource = contentResource.getChild("jcr:content");
                if (contentResource == null) {
                    return "No jcr:content child";
                }
            }
            if (!contentResource.getPath().contains("/jcr:content")) {
                return "Path does not contain jcr:content";
            }

            for (ModifyPageReadTool.ComponentProperties component : pageProperties.components) {
                String componentPath = removeStart(component.componentPath, "/");
                Resource componentResource = contentResource.getChild(componentPath);
                if (componentResource == null) {
                    return "Component not found: " + componentPath;
                }

                ModifiableValueMap valueMap = componentResource.adaptTo(ModifiableValueMap.class);
                if (valueMap == null) {
                    return "Cannot modify properties of: " + componentPath;
                }

                for (Map.Entry<String, String> entry : component.properties.entrySet()) {
                    // do not permit attributes that don't exist or are not texts
                    String key = entry.getKey();
                    Object value = valueMap.get(key);
                    if (value == null) {
                        return "Property not found: " + key + " in " + componentPath;
                    }
                    if (!(value instanceof String)) {
                        return "Property is not a text: " + key + " in " + componentPath;
                    }
                    String newValue = entry.getValue();
                    if (!ModifyPageReadTool.PATTERN_TWO_SEPARATE_WHITESPACE.matcher(newValue).find()) {
                        return "Property does not seem a text: " + key + " in " + componentPath;
                    }
                    if (startsWith((String) value, "<") && !startsWith(newValue, "<")) {
                        return "Property is HTML richtext, but the new value is not HTML richtext: use richtext for property: " + key + " in " + componentPath;
                    }
                    if (!startsWith((String) value, "<") && startsWith(newValue, "<")) {
                        return "Property is not HTML richtext, but the new value is HTML richtext: use plain text for property: " + key + " in " + componentPath;
                    }
                    valueMap.put(key, newValue);
                }
            }

            resolver.commit();
            return "Properties updated successfully. Notify the user that he / she needs to reload the page to see the changes.";
        } catch (Exception e) {
            LOG.error("Error in modify page write tool", e);
            return "CAUTION: Notify the user about a failure or retry once if you can fix the problem. No properties were changed at all in this request because the following error in modifying page write tool: " + e;
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

    @ObjectClassDefinition(name = "Composum AI Tool Modify Page Write",
            description = "Provides the AI with a tool to write properties to a page. Needs a lucene index for all pages." +
                    "If there is no configuration, the tool is not active.")
    public @interface Config {

        @AttributeDefinition(name = "Allowed paths regex",
                description = "A regex to match the paths that this tool is allowed to be used on. Default: /content/.*")
        String allowedPathsRegex() default "/content/.*";

    }
}
