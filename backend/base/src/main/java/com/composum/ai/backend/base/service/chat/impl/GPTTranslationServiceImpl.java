package com.composum.ai.backend.base.service.chat.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatMessagesTemplate;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.base.service.chat.GPTResponseCheck;
import com.composum.ai.backend.base.service.chat.GPTTranslationService;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
@Designate(ocd = GPTTranslationServiceImpl.Config.class)
@Component(service = GPTTranslationService.class)
public class GPTTranslationServiceImpl implements GPTTranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTTranslationServiceImpl.class);

    /**
     * Template for {@link GPTChatMessagesTemplate} to translate a single word or phrase. Has placeholders
     * ${sourcelanguage} ${sourcephrase} and ${targetlanguage}.
     */
    public static final String TEMPLATE_SINGLETRANSLATION = "singletranslation";

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    protected Config config;
    protected Double temperature;
    protected Integer seed;

    protected Path cacheDir;

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    @Nullable
    @Override
    public String singleTranslation(@Nullable String rawText, @Nullable String sourceLanguage, @Nullable String targetLanguage, @Nullable GPTConfiguration configuration) {
        ensureEnabled();
        if (rawText == null) {
            return null;
        }
        Matcher m = PATTERN_SEPARATE_WHITESPACE.matcher(rawText);
        if (m.matches()) {
            String before = m.group(1);
            String text = m.group(2);
            String after = m.group(3);
            String response;

            if (text == null || text.trim().isEmpty() || targetLanguage == null || targetLanguage.trim().isEmpty()) {
                return "";
            }

            GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);
            String cacheKey = cacheKey(request);
            String cachedResponse = getCachedResponse(cacheKey);
            if (cachedResponse != null) {
                LOG.debug("Returning cached result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, cachedResponse);
                return cachedResponse;
            }

            if (config.fakeTranslation()) {
                LOG.debug("Faking response to request {}", request);
                response = fakeTranslation(text);
            } else {
                response = chatCompletionService.getSingleChatCompletion(request);
            }

            response = response != null ? response.trim() : "";
            LOG.trace("Returning result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, response);
            cacheResponse(cacheKey, request, response);

            return before + response + after;
        } else {
            throw new IllegalStateException("Bug - that shouldn't happen. Text: '" + rawText + "'");
        }
    }

    @Override
    public void streamingSingleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration, @Nonnull GPTCompletionCallback callback) throws GPTException {
        ensureEnabled();

        if (text == null || text.trim().isEmpty() ||
                targetLanguage == null || targetLanguage.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty text or languages");
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);

        if (config.fakeTranslation()) {
            LOG.debug("Faking response to request {}", request);
            callback.onNext(fakeTranslation(text));
            callback.onFinish(GPTFinishReason.STOP);
            return;
        }

        // we don't do caching here since that'd be complicated and this is only for interactive use, not bulk use, anyway.
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    /**
     * Start of separator like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%` .
     */
    public static final String MULTITRANSLATION_SEPARATOR_START = "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ";

    /**
     * End of separator like `573472 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%` .
     */
    public static final String MULTITRANSLATION_SEPARATOR_END = " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";

    /**
     * Regexp matching separator like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%` (group "id" matches the number).
     * The \n cannot be directly matched since at the start it's sometimes ```%%%%...
     * We give the pattern a bit of leeway since some models get the number of % wrong.
     */
    protected static final Pattern MULTITRANSLATION_SEPARATOR_PATTERN = Pattern.compile("(?<!%)%{20,40} (?<id>\\d{6}) %{20,40}(?!%)");

    public static final String LASTID = "424242";

    protected static final Pattern PATTERN_HAS_LETTER = Pattern.compile("\\p{L}");

    /**
     * Separate whitespace at the beginning and end from the non-whitespace text.
     */
    protected static final Pattern PATTERN_SEPARATE_WHITESPACE = Pattern.compile("\\A(\\s*+)(.*?)(\\s*+)\\Z", Pattern.DOTALL);

    /**
     * We join all text fragments we have to translate into one big texts separated with separators like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%` and
     * then translate that. Then we split the result at the separators and return the fragments. Safety check is that the is from the
     * fragments have to match the ids in the result.
     */
    @Nonnull
    @Override
    public List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration,
                                              @Nullable List<GPTResponseCheck> translationChecks) throws GPTException {
        ensureEnabled();
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> realTexts = texts.stream()
                .filter(s -> s != null && PATTERN_HAS_LETTER.matcher(s).find())
                .map(s -> PATTERN_SEPARATE_WHITESPACE.matcher(s).replaceAll("$2"))
                .distinct()
                .collect(Collectors.toList());

        List<String> translatedRealTexts = fragmentedTranslationDivideAndConquer(realTexts, targetLanguage, configuration, new AtomicInteger(10), translationChecks);

        Map<String, String> translatedRealTextsMap = new LinkedHashMap<>();
        for (int i = 0; i < realTexts.size(); i++) {
            translatedRealTextsMap.put(realTexts.get(i), translatedRealTexts.get(i));
        }

        List<String> result = new ArrayList<>();
        for (String text : texts) {
            if (translatedRealTextsMap.containsKey(text)) {
                result.add(translatedRealTextsMap.get(text));
            } else if (text == null || !PATTERN_HAS_LETTER.matcher(text).find()) {
                result.add(text);
            } else {
                Matcher m = PATTERN_SEPARATE_WHITESPACE.matcher(text);
                if (m.matches()) {
                    String before = m.group(1);
                    String after = m.group(3);
                    String realText = m.group(2);
                    result.add(before + translatedRealTextsMap.get(realText) + after);
                } else {
                    throw new IllegalStateException("Bug - that shouldn't happen. Text: '" + text + "'");
                }
            }
        }
        return result;
    }

    /**
     * We try to translate the whole lot of texts. If that leads to an exception because we are out of tokens or the response was garbled, we split it into two and translate these individually. If even one text is too long, we are lost and give up.
     */
    protected List<String> fragmentedTranslationDivideAndConquer(@Nonnull List<String> texts, @Nonnull String targetLanguage,
                                                                 @Nullable GPTConfiguration configuration, @Nonnull AtomicInteger permittedRetries, List<GPTResponseCheck> translationChecks) throws GPTException {
        if (texts.isEmpty()) {
            return Collections.emptyList();
        } else if (texts.size() == 1 && (translationChecks == null || translationChecks.isEmpty())) {
            return Collections.singletonList(singleTranslation(texts.get(0), null, targetLanguage, configuration));
        }

        if (permittedRetries.get() <= 0) {
            LOG.error("Too many retries for fragmented translation to {} of {}", targetLanguage, texts);
            throw new GPTException("Too many retries for fragmented translation");
        }

        int textlength = texts.stream().mapToInt(String::length).sum();
        try {
            return fragmentedTranslation(texts, targetLanguage, configuration, permittedRetries, translationChecks);
        } catch (GPTException.GPTRetryableResponseErrorException e) {
            // is hopefully rare - otherwise we likely have to rethink this.
            LOG.info("Splitting translation because of retryable error at {} texts with length {}: {}", texts.size(), textlength, e.toString());
            // that did cost something, so retry permits are decremented. We split anyway, since that might make things easier for the GPT service.
            permittedRetries.decrementAndGet();
        } catch (GPTException.GPTContextLengthExceededException e) {
            // everything is fine - that doesn't cost anything. Just split
            LOG.info("Splitting translation because of context length exceeded {} at {} texts length {}",
                    e.getMessage(), texts.size(), textlength);
        }

        // The loss of context is a problem, but we go for graceful degradation here.
        GPTConfiguration cleanedConfiguration = configuration != null ? configuration.replaceContexts(null) : configuration;

        int half = texts.size() / 2;
        List<String> firstHalf = texts.subList(0, half);
        List<String> secondHalf = texts.subList(half, texts.size());
        List<String> result = new ArrayList<>();
        result.addAll(fragmentedTranslationDivideAndConquer(firstHalf, targetLanguage, cleanedConfiguration, permittedRetries, translationChecks));
        result.addAll(fragmentedTranslationDivideAndConquer(secondHalf, targetLanguage, cleanedConfiguration, permittedRetries, translationChecks));
        return result;
    }

    protected List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage,
                                                 @Nullable GPTConfiguration configuration, @Nonnull AtomicInteger permittedRetries,
                                                 @Nullable List<GPTResponseCheck> translationChecks) throws GPTException {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ids = new ArrayList<>();
        String joinedtexts = joinTexts(texts, ids);

        String response = singleTranslation(joinedtexts, null, targetLanguage, configuration);
        String responseProblems;
        while ((responseProblems = GPTResponseCheck.collectResponseProblems(translationChecks, joinedtexts, response)) != null) {
            if (permittedRetries.decrementAndGet() <= 0) {
                LOG.error("Too many retries with response problems for fragmented translation, to {} found problems {} text {}", targetLanguage, responseProblems, texts);
                throw new GPTException("Too many retries for fragmented translation, response problems: " + responseProblems);
            }
            GPTConfiguration fixupInstructions = GPTConfiguration.ofAdditionalInstructions(responseProblems);
            GPTConfiguration retryConfig = configuration != null ? configuration.merge(fixupInstructions) : fixupInstructions;
            response = singleTranslation(joinedtexts, null, targetLanguage, retryConfig);
        }

        return separateResultTexts(response, texts, ids, joinedtexts);
    }

    protected static String joinTexts(List<String> texts, List<String> ids) {
        StringBuilder joinedtexts = new StringBuilder();
        int rndid = 382938675;
        for (String text : texts) {
            rndid = rndid * 92821 + Objects.hashCode(text); // deterministic pseudo random number for cachability
            String id = ("" + (1000000 + Math.abs(rndid) % 1000000)).substring(1);
            joinedtexts.append(MULTITRANSLATION_SEPARATOR_START).append(id).append(MULTITRANSLATION_SEPARATOR_END);
            joinedtexts.append(text);
            ids.add(id);
        }
        joinedtexts.append(MULTITRANSLATION_SEPARATOR_START).append(LASTID).append(MULTITRANSLATION_SEPARATOR_END);
        return joinedtexts.toString();
    }

    protected static List<String> separateResultTexts(String response, List<String> texts, List<String> ids, String joinedtexts) {
        List<String> result = new ArrayList<>();
        Matcher m = MULTITRANSLATION_SEPARATOR_PATTERN.matcher("\n" + response + "\n");
        int start = 0;
        int idnum = 0;
        while (m.find()) {
            String id = m.group("id");
            int end = m.start();
            if (start > 0) { // remove text before first separator
                String part = response.substring(start - 1, end - 1).trim();
                result.add(part);
            }
            start = m.end();

            if (id.equals(LASTID)) {
                break;
            }
            if (idnum >= ids.size() || !ids.get(idnum).equals(id)) {
                LOG.debug("Original text:\n{}", joinedtexts);
                LOG.debug("Mismatch in response:\n{}", response);
                throw new GPTException.GPTRetryableResponseErrorException("Mismatch in translation fragments: " + id + " vs. " + ids.get(idnum));
            }
            idnum++;
        }
        if (result.size() != texts.size()) {
            LOG.debug("Original text:\n{}", joinedtexts);
            LOG.debug("Mismatch in response:\n{}", response);
            throw new GPTException.GPTRetryableResponseErrorException("Mismatch in number of translation fragments: " + result.size() + " vs. " + texts.size());
        }
        return result;
    }

    public static final Pattern HTML_TAG_AT_START = Pattern.compile("\\A\\s*(<[^>]*>)");

    private GPTChatRequest makeRequest(String text, String sourceLanguage, String targetLanguage, @Nullable GPTConfiguration outerConfiguration) {
        // fetch the GPTChatMessagesTemplate, replace the placeholders and call the chatCompletionService
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_SINGLETRANSLATION);
        GPTChatRequest request = new GPTChatRequest();
        GPTConfiguration configuration = getServiceConfiguration().merge(outerConfiguration);
        request.setConfiguration(configuration);
        String addition = configuration != null && configuration.getAdditionalInstructions() != null ? "\n\n" + configuration.getAdditionalInstructions() : "";
        if (configuration != null && configuration.isHtml()) {
            Matcher m = HTML_TAG_AT_START.matcher(text);
            String firstTag = "<p>";
            if (m.find()) {
                firstTag = m.group(1);
            }
            addition += (addition.isEmpty() ? "" : "\n\n") + "Output HTML; start the translation with " + firstTag;
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sourcelanguage", sourceLanguage != null ? sourceLanguage : "guess it from the text");
        parameters.put("sourcephrase", text);
        parameters.put("targetlanguage", targetLanguage);
        parameters.put("addition", addition);
        List<GPTChatMessage> messages = template.getMessages(parameters);
        if (configuration.getContexts() != null && !configuration.getContexts().isEmpty()) {
            int start = messages.get(0).getRole() == GPTMessageRole.SYSTEM ? 1 : 0;
            for (int i = configuration.getContexts().size() - 1; i >= 0; i--) {
                GPTConfiguration.GPTContextInfo context = configuration.getContexts().get(i);
                GPTChatMessage contextMessage = new GPTChatMessage(GPTMessageRole.USER, context.getTitle());
                messages.add(start, contextMessage);
                contextMessage = new GPTChatMessage(GPTMessageRole.ASSISTANT, context.getText());
                messages.add(start + 1, contextMessage);
            }
        }
        request.addMessages(messages);

        // set request.setMaxTokens to about 3 times the number of tokens in the text to translate
        // since that seems a generous limit for the translation, but gives a leeway for error messages.
        // this usually quite an overestimation, but that's better than underestimating in this context.
        // 3 times since e.g. korean seems to take that many times tokens as english.

        int maxTokens = 3 * chatCompletionService.countTokens(text) + 100;
        if (maxTokens < 4096) { // setting more than that could lead to trouble with weaker models - length restriction will hit anyway.
            request.setMaxTokens(maxTokens);
        }
        return request;
    }

    protected GPTConfiguration getServiceConfiguration() {
        return new GPTConfiguration(null, null, null, null, null, temperature, seed);
    }

    /**
     * This turns the capitalization of every odd letter in each word on it's head. If we are in a HTML tag (that is,
     * between a &lt; and a &gt; ) then nothing is changed to avoid destroying richtext.
     * For quick and inexpensive testing e.g. of bulk translation mechanics.
     * <p>Example: "This is a test <code>and some Code</code>" -> "THiS iS a tEsT <code>aNd sOmE COdE</code>"</p>
     */
    protected static String fakeTranslation(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        boolean inTag = false;
        for (char c : text.toCharArray()) {
            if (c == '<') {
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            }
            if (inTag) {
                result.append(c);
            } else {
                if (Character.isLetter(c)) {
                    if (Math.random() < 0.8) {
                        result.append(c);
                    } else {
                        result.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
                    }
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString().trim(); // trim to make it behave like the original.
    }


    private String cacheKey(GPTChatRequest request) {
        if (cacheDir == null) {
            return null;
        }
        int hash = 17;
        for (char c : request.toString().toCharArray()) {
            hash = 92821 * hash + c;
        }
        boolean hi = request.getConfiguration() != null ? request.getConfiguration().highIntelligenceNeededIsSet() : false;
        return (hi ? "hi-" : "") + Integer.toHexString(Math.abs(hash));
    }

    protected void cacheResponse(String cacheKey, GPTChatRequest request, String response) {
        if (cacheDir != null) {
            Path cacheRequest = cacheDir.resolve(cacheKey + ".request");
            Path cacheResponse = cacheDir.resolve(cacheKey + ".response");
            try {
                Files.write(cacheRequest, request.toString().getBytes(StandardCharsets.UTF_8));
                Files.write(cacheResponse, response.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOG.error("Writing to this or response file " + cacheResponse, e);
            }
        }
    }

    protected String getCachedResponse(String cacheKey) {
        if (cacheDir != null) {
            Path cacheResponse = cacheDir.resolve(cacheKey + ".response");
            if (Files.exists(cacheResponse)) {
                try {
                    return new String(Files.readAllBytes(cacheResponse), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOG.warn("Reading from " + cacheResponse, e);
                }
            }
        } else {
            LOG.debug("No cached response for {}", cacheKey);
        }
        return null;
    }


    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        if (config != null) {
            File cacheDir = config.diskCache() != null && !config.diskCache().trim().isEmpty() ? new File(config.diskCache().trim()) : null;
            this.cacheDir = null;
            if (cacheDir != null) {
                if (cacheDir.exists()) {
                    LOG.info("Using disk cache for translations at {}", cacheDir);
                    this.cacheDir = cacheDir.toPath();
                } else {
                    LOG.error("Disk cache for translations does not exist: {}", cacheDir);
                }
            }
            if (config.temperature() != null && !config.temperature().trim().isEmpty()) {
                try {
                    this.temperature = Double.parseDouble(config.temperature().trim());
                } catch (NumberFormatException e) {
                    LOG.error("Invalid temperature value: {}", config.temperature());
                }
            }
            if (config.seed() != null && !config.seed().trim().isEmpty()) {
                try {
                    this.seed = Integer.parseInt(config.seed().trim());
                } catch (NumberFormatException e) {
                    LOG.error("Invalid seed value: {}", config.seed());
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
        this.seed = null;
        this.temperature = null;
    }

    protected void ensureEnabled() {
        if (config == null || config.disabled()) {
            throw new IllegalStateException("Translation service is currently disabled" +
                    (config == null ? "" : " by configuration"));
        }
    }

    @ObjectClassDefinition(name = "Composum AI Translation Service Configuration",
            description = "Configuration for the basic Composum AI Translation Service")
    public @interface Config {

        @AttributeDefinition(name = "Disable the translation service", defaultValue = "false")
        boolean disabled() default false;

        @AttributeDefinition(name = "Fake translation", description = "For quick and inexpensive testing, " +
                "when you just want to check that the translation does something for e.g. a bulk of texts, " +
                "you can enable this. The \"translation\" then just turns the text iNtO tHiS cApItAlIsAtIoN. " +
                "Easy to spot, but probably doesn't destroy the content completely.", defaultValue = "false")
        boolean fakeTranslation() default false;

        @AttributeDefinition(name = "Disk cache", description = "Path to a directory where to cache the translations. " +
                "If empty, no caching is done. If the path is relative, it is relative to the current working directory. " +
                "If the path is absolute, it is used as is.", defaultValue = "")
        String diskCache();

        @AttributeDefinition(name = "temperature", description = "The sampling temperature, between 0 and 1. " +
                "Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.")
        String temperature() default "";

        @AttributeDefinition(name = "seed", description = "If specified, OpenAI will make a best effort to sample deterministically, " +
                "such that repeated requests with the same seed and parameters should return the same result.")
        String seed() default "";
    }
}
