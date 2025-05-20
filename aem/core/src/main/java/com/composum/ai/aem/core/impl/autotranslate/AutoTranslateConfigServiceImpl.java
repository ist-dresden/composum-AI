package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;

/**
 * Serves the configurations for the automatic translation service.
 */
@Component(service = AutoTranslateConfigService.class, scope = ServiceScope.SINGLETON)
@Designate(ocd = AutoTranslateConfig.class)
public class AutoTranslateConfigServiceImpl implements AutoTranslateConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateConfigServiceImpl.class);

    /**
     * List of properties that should always be translated.
     */
    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title", "alt", "cq:panelTitle", "shortDescription",
                    "actionText", "accessibilityLabel", "pretitle", "helpMessage",
                    "dc:title", "dc:description");


    protected static final Pattern PATTERN_HAS_WHITESPACE = Pattern.compile("\\s");

    /**
     * As additional heuristic - the text should have at least one word with >= 5 letters.
     * That will break source languages very different from english, I know, but this is a POC. :-)
     */
    protected static final Pattern PATTERN_HAS_WORD = Pattern.compile("\\p{L}{5}");

    protected static final Pattern PATTERN_HAS_LETTER = Pattern.compile("\\p{L}");

    /**
     * Matches a HTML tag or endtag or HTML comment.
     */
    public static final Pattern HTML_TAG_OR_COMMENT_PATTERN =
            Pattern.compile("<\\w+(\\s+[a-zA-Z0-9_-]*+(=\"[^\"]*\"|=\\S+)?)*/?>|</[a-zA-Z0-9_-]*+>|<!--\\s.*?\\s-->");

    /**
     * Somewhat arbitrary treshold: if a single property is more than that number of tokens we don't try
     * to translate it. That'll likely fail and block the translation, and there is very likely something
     * that shouldn't be translated, anyway, like a large HTML fragment.
     */
    protected static final int HUGE_TRESHOLD = 3000;

    protected List<Pattern> deniedResourceTypes = new ArrayList<>();
    protected List<Pattern> allowedAttributeRegexes = new ArrayList<>();
    protected List<Pattern> deniedAttributesRegexes = new ArrayList<>();

    protected AutoTranslateConfig config;

    @Reference
    protected GPTChatCompletionService gptChatCompletionService;

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
    public String getDefaultModel() {
        if (config != null && StringUtils.isNotBlank(config.defaultModel())) {
            return config.defaultModel();
        }
        return null;
    }

    @Override
    public boolean isTranslatableResource(@Nullable final Resource resource) {
        if (!isEnabled() || resource == null) {
            return false;
        }
        final String resourceType = resource.getResourceType();
        if (resourceType.equals("dam:AssetContent") && config.ignoreAssetsExceptContentFragments()
                && !Boolean.TRUE.equals(resource.getValueMap().get("contentFragment", Boolean.class))) {
            LOG.debug("Ignoring asset that is not content fragment: ", resource.getPath());
            return false;
        }
        for (final Pattern pattern : deniedResourceTypes) {
            if (pattern.matcher(resourceType).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nonnull
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
            if (!(entry.getValue() instanceof String) || AITranslatePropertyWrapper.isAiTranslateProperty(entry.getKey())) {
                continue;
            }
            String attributeDescription = slingResourceType + "%" + attributeAdditionalPath + entry.getKey();
            boolean allowed = allowedAttributeRegexes.stream().anyMatch(p -> p.matcher(attributeDescription).matches());
            if (!allowed) {
                boolean denied = deniedAttributesRegexes.stream().anyMatch(p -> p.matcher(attributeDescription).matches());
                if (denied || !isHeuristicallyTranslatableProperty(entry.getKey(), entry.getValue())
                        || isHuge(entry.getKey(), entry.getValue())) {
                    continue;
                }
            }
            result.add(entry.getKey());
        }
        return result;
    }

    @Override
    public boolean includeFullPageInRetranslation() {
        return config == null || config.includeFullPageInRetranslation();
    }

    @Override
    public boolean includeExistingTranslationsInRetranslation() {
        return config == null || config.includeExistingTranslationsInRetranslation();
    }

    /**
     * Checks whether the property is one of jcr:title, jcr:description, title, alt, cq:panelTitle, shortDescription,
     * actionText, accessibilityLabel, pretitle, displayPopupTitle, helpMessage , or alternatively don't have a colon
     * in the name, have a String value, don't start with /{content,apps,libs,mnt}/ in the value and the value has
     * a whitespace and at least one 4 letter sequence. We also exclude something that is {@link #isHtmlButNotRichtext(String, Object)}.
     */
    public static boolean isHeuristicallyTranslatableProperty(String name, Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.startsWith("/content/") || stringValue.startsWith("/apps/") ||
                    stringValue.startsWith("/libs/") || stringValue.startsWith("/mnt/") ||
                    stringValue.equals("true") || stringValue.equals("false") ||
                    stringValue.startsWith("https:") || stringValue.startsWith("http:")
            ) {
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
                    PATTERN_HAS_WORD.matcher(stringValue).find() &&
                    !isHtmlButNotRichtext(name, value);
        }
        return false;
    }

    /**
     * Heuristic check whether this is actually HTML that shouldn't be translated.
     * Richtext is acceptable for translating, but HTML with lots of attributes not.
     * We recognize text that has a very significant amount of text in HTML tags / comments and try to
     * err on the save side - it should be "very obviously" HTMl, which often has
     * many attributes in it's HTML tags.
     */
    public static boolean isHtmlButNotRichtext(String name, Object value) {
        if (value == null || !(value instanceof String) || ((String) value).length() < 50) {
            return false;
        }
        String text = (String) value;
        if (!text.contains("<") && !text.contains("&lt;") ||
                !text.contains(">") && !text.contains("&gt;") || !text.contains("/")) {
            return false;
        }
        text = StringEscapeUtils.unescapeHtml(text);
        int textLength = text.length();
        int tagcount = 0;
        int textInTags = 0;
        int startTagCount = 0;
        int endTagCount = 0;
        // count characters within nontrivial HTML tags / HTML comments
        Matcher matcher = HTML_TAG_OR_COMMENT_PATTERN.matcher(text);
        while (matcher.find()) {
            tagcount++;
            int length = matcher.end() - matcher.start();
            if (length > 10) {
                textInTags += length;
            }
            String matchedText = matcher.group();
            if (matchedText.startsWith("</")) {
                endTagCount++;
            } else if (!matchedText.startsWith("<!--")) {
                startTagCount++;
            }
        }
        LOG.trace("Tagcount: {}, textInTags percentage: {}", tagcount, (100L * textInTags) / textLength);
        boolean isHtml = tagcount > 10 && textInTags > textLength / 3 && startTagCount > 5 && endTagCount > 5;
        if (isHtml) {
            LOG.warn("Attribute {} contained a large HTML fragment and should probably be excluded from translation.", name);
        }
        return isHtml;
    }

    /**
     * This recognizes huge values that we shouldn't even attempt to translate since that would very likely
     * fail and block translation of the rest of the properties.
     * (Note: it is not impossible to translate such a thing but that would need a special implementation which
     * we will do if there is a real use case.)
     */
    public boolean isHuge(String name, Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.length() > HUGE_TRESHOLD) {
                int tokens = this.gptChatCompletionService.countTokens(stringValue);
                if (tokens > HUGE_TRESHOLD) {
                    LOG.warn("Property has huge content and therefore is not translated - perhaps exclude by configuration? {} with {} tokens", name, tokens);
                    return true;
                }
            }
        }
        return false;
    }

}
