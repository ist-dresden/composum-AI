package com.composum.ai.aem.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    protected static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}([_-][a-zA-Z]{2,3})?$");

    protected static Set<String> LOCALE_NAMES =
            Stream.concat(Stream.of(Locale.getAvailableLocales())
                                    .map(Locale::toString),
                            Stream.of(Locale.getAvailableLocales())
                                    .map(Locale::toLanguageTag))
                    .filter(LOCALE_PATTERN.asPredicate())
                    .map(t -> t.replace('-', '_').toLowerCase())
                    .collect(Collectors.toSet());

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
     * Determines the language this resource is in by searching for an ancestor resource with jcr:language set,
     * or for an ancestor that looks like a language code.
     */
    public static String findLanguage(Resource pageResource) {
        String language = null;
        Resource candidate = pageResource;
        while (candidate != null && language == null) {
            language = candidate.getValueMap().get(JcrConstants.JCR_LANGUAGE, String.class);
            if (language == null) {
                language = candidate.getValueMap().get(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_LANGUAGE, String.class);
            }
            candidate = candidate.getParent();
        }
        if (language == null) {
            String[] pathElements = pageResource.getPath().split("/");
            for (String element : pathElements) {
                if (isLocaleName(element)) {
                    language = element;
                    break;
                }
            }
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

    public static boolean isLocaleName(String name) {
        return name != null && LOCALE_NAMES.contains(name.replace('-', '_').toLowerCase());
    }

    /**
     * We look in the resource path for a path element that {@link #isLocaleName(String)} and try to replace it
     * by language as it is or language with a country suffix ([-_][a-zA-Z]{2}) removed. Resources that
     * can be found like that are returned.
     */
    public static List<Resource> getLanguageSiblings(@Nullable Resource resource, @Nullable String language) {
        if (resource == null || language == null) {
            return Collections.emptyList();
        }
        List<Resource> result = new ArrayList<>();
        String path = resource.getPath();
        String[] pathElements = path.split("/");
        Set<String> paths = new java.util.HashSet<>();
        paths.add(path);
        for (int i = 0; i < pathElements.length; i++) {
            String element = pathElements[i];
            if (isLocaleName(element)) {
                pathElements[i] = language;
                Resource candidate = resource.getResourceResolver().getResource(String.join("/", pathElements));
                if (candidate != null && !paths.contains(candidate.getPath())) {
                    result.add(candidate);
                    paths.add(candidate.getPath());
                }
                pathElements[i] = language.split("[-_]", 2)[0];
                candidate = resource.getResourceResolver().getResource(String.join("/", pathElements));
                if (candidate != null && !paths.contains(candidate.getPath())) {
                    result.add(candidate);
                    paths.add(candidate.getPath());
                }
            }
            pathElements[i] = element;
        }
        return result;
    }

}
