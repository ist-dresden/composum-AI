package com.composum.ai.aem.core.impl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * Implements AEM specific methods of {@link com.composum.ai.backend.slingbase.AIConfigurationPlugin}.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=5000"
)
public class AemAIConfigurationPlugin implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AemAIConfigurationPlugin.class);

    /**
     * Possible attributes we use for the prompt name.
     */
    public static final List<String> TITLE_ATTRIBS = Arrays.asList("jcr:title", "title", "subtitle");

    /**
     * Possible atttributes we use for the prompt text.
     */
    public static final List<String> TEXT_ATTRIBS = Arrays.asList("jcr:description", "description", "text");

    @Nullable
    @Override
    public GPTPromptLibrary getGPTPromptLibraryPathsDefault() {
        return new GPTPromptLibrary() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return GPTPromptLibrary.class;
            }

            @Override
            public String contentCreationPromptsPath() {
                return "/conf/composum-ai/settings/dialogs/contentcreation/predefinedprompts";
            }

            @Override
            public String sidePanelPromptsPath() {
                return "/conf/composum-ai/settings/dialogs/sidepanel-ai/predefinedprompts";
            }
        };
    }

    @Nullable
    @Override
    public Map<String, String> getGPTConfigurationMap(@Nonnull SlingHttpServletRequest request, @Nullable String mapPath, @Nullable String languageKey) {
        if (StringUtils.isBlank(mapPath) || mapPath.toLowerCase().contains(".json")) {
            return null;
        }
        Resource promptsResource = request.getResourceResolver().getResource(mapPath);
        promptsResource = promptsResource != null ? determineLanguageResource(promptsResource, languageKey) : null;

        // search for the child resource that has the most children - that's the most likely container for the prompts.
        Optional<Pair<Resource, List<Resource>>> promptContainer = descendantsStream(promptsResource)
                .map(r -> Pair.of(r, IteratorUtils.toList(r.listChildren())))
                .min((a, b) -> Integer.compare(b.getRight().size(), a.getRight().size()));
        if (!promptContainer.isPresent()) {
            return null;
        }

        // we don't know exactly which component is used, and which attributes. So we try several attribute names.
        Map<String, String> prompts = new LinkedHashMap<>();
        for (Resource prompt : promptContainer.get().getRight()) {
            String title = TITLE_ATTRIBS.stream()
                    .map(a -> prompt.getValueMap().get(a, String.class))
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);
            String text = TEXT_ATTRIBS.stream()
                    .map(a -> prompt.getValueMap().get(a, String.class))
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);
            if (StringUtils.isNoneBlank(title, text)) {
                prompts.put(text, title);
            }
        }
        if (prompts.isEmpty()) {
            LOG.warn("No prompts found for {}", mapPath);
            return null;
        }

        return prompts;
    }

    protected Resource determineLanguageResource(Resource resource, String languageKey) {
        Resource langResource = null;
        while (languageKey != null) {
            if (resource.getChild(languageKey + "/" + JcrConstants.JCR_CONTENT) != null) {
                langResource = resource.getChild(languageKey + "/" + JcrConstants.JCR_CONTENT);
                break;
            }
            if (languageKey.contains("_")) {
                languageKey = StringUtils.substringBeforeLast(languageKey, "_");
            } else {
                break;
            }
        }
        if (langResource == null && resource.getChild("en/" + JcrConstants.JCR_CONTENT) != null) {
            langResource = resource.getChild("en/" + JcrConstants.JCR_CONTENT);
        }
        if (langResource == null) { // if there are subnodes with jcr:content node we take the first one
            for (Resource child : resource.getChildren()) {
                if (child.getChild(JcrConstants.JCR_CONTENT) != null) {
                    langResource = child.getChild(JcrConstants.JCR_CONTENT);
                    break;
                }
            }
        }
        if (langResource == null && resource.getChild(JcrConstants.JCR_CONTENT) != null) {
            langResource = resource.getChild(JcrConstants.JCR_CONTENT);
        }
        return langResource;
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
                        .flatMap(AemAIConfigurationPlugin::descendantsStream));
    }

}
