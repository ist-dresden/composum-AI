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

    private AutoTranslateConfig config;

    @Activate
    @Modified
    public void activate(AutoTranslateConfig config) {
        this.config = config;
        deniedResourceTypes.clear();
        for (final String rule : config.deniedResourceTypes()) {
            if (StringUtils.isNotBlank(rule)) {
                deniedResourceTypes.add(Pattern.compile(rule));
            }
        }
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
        if (resource == null) {
            return Collections.emptyList();
        }
        ValueMap valueMap = resource.getValueMap();
        return valueMap.entrySet().stream()
                .filter(entry -> isTranslatableProperty(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Checks whether the property is one of jcr:title, jcr:description, title, alt, cq:panelTitle, shortDescription,
     * actionText, accessibilityLabel, pretitle, displayPopupTitle, helpMessage , or alternatively don't have a colon
     * in the name, have a String value, don't start with /{content,apps,libs,mnt}/ in the value and the value has
     * a whitespace and at least one 4 letter sequence.
     */
    protected static boolean isTranslatableProperty(String name, Object value) {
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
