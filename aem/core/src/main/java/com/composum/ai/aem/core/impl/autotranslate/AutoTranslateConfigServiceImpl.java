package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;

/**
 * Serves the configurations for the automatic translation service.
 */
@Component(service = AutoTranslateConfigService.class, scope = ServiceScope.SINGLETON,
// pid: backwards compatibility
        configurationPid = "com.composum.ai.aem.core.impl.autotranslate.AutoTranslateServiceImpl")
@Designate(ocd = AutoTranslateConfig.class)
public class AutoTranslateConfigServiceImpl implements AutoTranslateConfigService {

    /**
     * List of properties that should always be translated.
     */
    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title", "alt", "cq:panelTitle", "shortDescription",
                    "actionText", "accessibilityLabel", "pretitle", "helpMessage",
                    "dc:title", "dc:description");


    protected static final Pattern PATTERN_HAS_WHITESPACE = Pattern.compile("\\s");

    /**
     * As additional heuristic - the text should have at least one word with >= 4 letters.
     * That will break down very different languages, I know, but this is a POC. :-)
     */
    protected static final Pattern PATTERN_HAS_WORD = Pattern.compile("\\p{L}{4}");

    protected static final Pattern PATTERN_HAS_LETTER = Pattern.compile("\\p{L}");

    protected List<Pattern> deniedResourceTypes = new ArrayList<>();
    protected List<Pattern> allowedAttributeRegexes = new ArrayList<>();
    protected List<Pattern> deniedAttributesRegexes = new ArrayList<>();

    protected AutoTranslateConfig config;

    @Activate
    @Modified
    public void activate(AutoTranslateConfig config) {
        this.config = config;
        deniedResourceTypes = stringArrayToRegexes(config.deniedResourceTypes());
        allowedAttributeRegexes = stringArrayToRegexes(config.allowedAttributeRegexes());
        deniedAttributesRegexes = stringArrayToRegexes(config.deniedAttributesRegexes());
    }

    private static List<Pattern> stringArrayToRegexes(String[] array) {
        return array == null ? Collections.emptyList() :
                Arrays.stream(array)
                        .filter(StringUtils::isNotBlank)
                        .map(s -> Pattern.compile(s))
                        .collect(Collectors.toList());
    }

    @Deactivate
    public void deactivate() {
        this.config = null;
    }

    @Override
    public boolean isPocUiEnabled() {
        return config != null && config.pocUiEnabled();
    }

    @Override
    public boolean isEnabled() {
        return config != null && !config.disabled();
    }

    @Override
    public boolean isTranslatableResource(@Nullable final Resource resource) {
        if (!isEnabled() || resource == null) {
            return false;
        }
        final String resourceType = resource.getResourceType();
        for (final Pattern pattern : deniedResourceTypes) {
            if (pattern.matcher(resourceType).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> translateableAttributes(@Nullable Resource resource) {
        if (resource == null || !isTranslatableResource(resource)) {
            return Collections.emptyList();
        }
        ValueMap valueMap = resource.getValueMap();

        String slingResourceType = null;
        String attributeAdditionalPath = "";
        Resource searchResource = resource;
        while (searchResource != null && slingResourceType == null) {
            slingResourceType = searchResource.getValueMap().get("sling:resourceType", String.class);
            if (slingResourceType == null) {
                attributeAdditionalPath = searchResource.getName() + "/" + attributeAdditionalPath;
            }
            searchResource = searchResource.getParent();
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String attributeDescription = slingResourceType + "%" + attributeAdditionalPath + entry.getKey();
            boolean allowed = allowedAttributeRegexes.stream().anyMatch(p -> p.matcher(attributeDescription).matches());
            if (!allowed) {
                boolean denied = deniedAttributesRegexes.stream().anyMatch(p -> p.matcher(attributeDescription).matches());
                if (denied || !isHeuristicallyTranslatableProperty(entry.getKey(), entry.getValue())) {
                    continue;
                }
            }
            result.add(entry.getKey());
        }
        return result;
    }

    /**
     * Checks whether the property is one of jcr:title, jcr:description, title, alt, cq:panelTitle, shortDescription,
     * actionText, accessibilityLabel, pretitle, displayPopupTitle, helpMessage , or alternatively don't have a colon
     * in the name, have a String value, don't start with /{content,apps,libs,mnt}/ in the value and the value has
     * a whitespace and at least one 4 letter sequence.
     */
    protected static boolean isHeuristicallyTranslatableProperty(String name, Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.startsWith("/content/") || stringValue.startsWith("/apps/") ||
                    stringValue.startsWith("/libs/") || stringValue.startsWith("/mnt/") ||
                    stringValue.equals("true") || stringValue.equals("false")) {
                return false; // looks like path or boolean
            }

            if (CERTAINLY_TRANSLATABLE_PROPERTIES.contains(name) &&
                    PATTERN_HAS_LETTER.matcher(stringValue).find()
            ) {
                return true;
            }
            if (name.contains(":")) {
                return false;
            }

            if (AITranslatePropertyWrapper.isAiTranslateProperty(name)) {
                return false;
            }
            return PATTERN_HAS_WHITESPACE.matcher(stringValue).find() &&
                    PATTERN_HAS_WORD.matcher(stringValue).find();
        }
        return false;
    }


}
