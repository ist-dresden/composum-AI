package com.composum.ai.aem.core.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

/**
 * Some utility methods for this.
 */
public class SelectorUtils {

    /**
     * Parameter defining the path to the resource we work on.
     */
    public static final String PARAMETER_PATH = "path";

    /**
     * Special placeholder in prompts that is replaced by the language of the page.
     */
    public static final String PLACEHOLDER_TARGETLANGUAGE = "TARGETLANGUAGE";

    /**
     * The keys of the map are the displayed texts, the values the value for the selects.
     */
    static DataSource transformToDatasource(SlingHttpServletRequest request, Map<String, String> contentSelectors) {
        if (contentSelectors == null) {
            contentSelectors = Collections.emptyMap();
        }
        List<Resource> resourceList = contentSelectors.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("value", entry.getKey());
                    values.put("text", entry.getValue());
                    ValueMap valueMap = new ValueMapDecorator(values);
                    return new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured", valueMap);
                })
                .collect(Collectors.toList());
        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        return dataSource;
    }

    /**
     * Determines the langage this resource is in by searching for a ancestor resource with jcr:language set.
     */
    public static String findLanguage(Resource pageResource) {
        String language = null;
        while (pageResource != null && language == null) {
            language = pageResource.getValueMap().get(JcrConstants.JCR_LANGUAGE, String.class);
            if (language == null) {
                language = pageResource.getValueMap().get(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_LANGUAGE, String.class);
            }
            pageResource = pageResource.getParent();
        }
        return language;
    }

    @Nonnull
    static Map<String, String> replaceLanguagePlaceholder(Map<String, String> prompts, String language) {
        if (prompts == null) {
            return Collections.emptyMap();
        }
        language = language != null ? language : "en";
        String languageName = getLanguageName(language);

        Map<String, String> result = new LinkedHashMap<>();
        prompts.forEach((key, value) -> {
            result.put(key.replace(PLACEHOLDER_TARGETLANGUAGE, languageName), value);
        });
        return result;
    }

    /* Determine the actual language name of the page - language is the language code, not the name.
     * the name of the language needs to be the human-readable name of the language in the language itself. */
    public static String getLanguageName(String language) {
        language = StringUtils.replaceChars(language, "_", "-");
        Locale locale = Locale.forLanguageTag(language);
        String languageName = locale.getDisplayLanguage(locale);
        return languageName;
    }
}
