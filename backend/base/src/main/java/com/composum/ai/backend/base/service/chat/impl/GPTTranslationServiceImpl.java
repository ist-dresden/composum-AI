package com.composum.ai.backend.base.service.chat.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

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

    protected Path cacheDir;

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    @Nonnull
    @Override
    public String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage, @Nullable GPTConfiguration configuration) {
        ensureEnabled();
        if (config.fakeTranslation()) {
            return fakeTranslation(text);
        }

        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(targetLanguage)) {
            return "";
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);
        String cacheKey = cacheKey(request);
        String cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            LOG.debug("Returning cached result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, cachedResponse);
            return cachedResponse;
        }
        String response = chatCompletionService.getSingleChatCompletion(request);
        response = response.trim();
        LOG.debug("Returning result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, response);
        cacheResponse(cacheKey, request, response);
        return response;
    }

    @Override
    public void streamingSingleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration, @Nonnull GPTCompletionCallback callback) throws GPTException {
        ensureEnabled();

        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(sourceLanguage) || Strings.isNullOrEmpty(targetLanguage)) {
            throw new IllegalArgumentException("Empty text or languages");
        }
        if (config.fakeTranslation()) {
            callback.onNext(fakeTranslation(text));
            callback.onFinish(GPTFinishReason.STOP);
            return;
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);
        // we don't do caching here since that'd be complicated and this is only for interactive use, not bulk use, anyway.
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    /**
     * Start of separator like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%` .
     */
    protected static final String MULTITRANSLATION_SEPARATOR_START = "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ";

    /**
     * End of separator like `573472 %%%%%%%%%%%%%%%%` .
     */
    protected static final String MULTITRANSLATION_SEPARATOR_END = " %%%%%%%%%%%%%%%%\n";

    /**
     * Regexp matching separator like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%` (group "id" matches the number)
     */
    protected static final Pattern MULTITRANSLATION_SEPARATOR_PATTERN = Pattern.compile("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% (?<id>\\d{6}) %%%%%%%%%%%%%%%%\n");

    protected static final String LASTID = "424242";

    /**
     * We join all text fragments we have to translate into one big texts separated with separators like `%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%` and
     * then translate that. Then we split the result at the separators and return the fragments. Safety check is that the is from the
     * fragments have to match the ids in the result.
     */
    @Nonnull
    @Override
    public List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration) throws GPTException {
        ensureEnabled();
        if (config.fakeTranslation()) {
            return texts.stream().map(GPTTranslationServiceImpl::fakeTranslation).collect(Collectors.toList());
        }

        return fragmentedTranslationDivideAndConquer(texts, targetLanguage, configuration, new AtomicInteger(5));
    }

    /**
     * We try to translate the whole lot of texts. If that leads to an exception because we are out of tokens or the response was garbled, we split it into two and translate these individually. If even one text is too long, we are lost and give up.
     */
    protected List<String> fragmentedTranslationDivideAndConquer(@Nonnull List<String> texts, @Nonnull String targetLanguage,
                                                                 @Nullable GPTConfiguration configuration, @Nonnull AtomicInteger permittedRetries) throws GPTException {
        if (texts.isEmpty()) {
            return Collections.emptyList();
        } else if (texts.size() == 1) {
            return Collections.singletonList(singleTranslation(texts.get(0), null, targetLanguage, configuration));
        }

        if (permittedRetries.get() <= 0) {
            LOG.error("Too many retries for fragmented translation to {} of {}", targetLanguage, texts);
            throw new GPTException("Too many retries for fragmented translation");
        }

        try {
            return fragmentedTranslation(texts, targetLanguage, configuration, permittedRetries);
        } catch (GPTException.GPTRetryableResponseErrorException e) {
            // is hopefully rare - otherwise we likely have to rethink this.
            LOG.info("Splitting translation because of retryable error: {}", e.toString());
            LOG.info("GPTRetryableResponseErrorException occurred for {} texts with length {}", texts.size(),
                    texts.stream().collect(Collectors.summarizingInt(String::length)));
            // that did cost something, so retry permits are decremented. We split anyway, since that might make things easier for the GPT service.
            permittedRetries.decrementAndGet();
        } catch (GPTException.GPTContextLengthExceededException e) {
            // everything is fine - that doesn't cost anything. Just split
        }

        int half = texts.size() / 2;
        List<String> firstHalf = texts.subList(0, half);
        List<String> secondHalf = texts.subList(half, texts.size());
        List<String> result = new ArrayList<>();
        result.addAll(fragmentedTranslationDivideAndConquer(firstHalf, targetLanguage, configuration, permittedRetries));
        result.addAll(fragmentedTranslationDivideAndConquer(secondHalf, targetLanguage, configuration, permittedRetries));
        return result;
    }

    protected List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage,
                                                 @Nullable GPTConfiguration configuration, @Nonnull AtomicInteger permittedRetries) throws GPTException {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ids = new ArrayList<>();
        String joinedtexts = joinTexts(texts, ids);

        String response = singleTranslation(joinedtexts, null, targetLanguage, configuration);

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

    private GPTChatRequest makeRequest(String text, String sourceLanguage, String targetLanguage, @Nullable GPTConfiguration configuration) {
        // fetch the GPTChatMessagesTemplate, replace the placeholders and call the chatCompletionService
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_SINGLETRANSLATION);
        GPTChatRequest request = new GPTChatRequest();
        String addition = "";
        if (configuration != null && configuration.isHtml()) {
            Matcher m = HTML_TAG_AT_START.matcher(text);
            String firstTag = "<p>";
            if (m.find()) {
                firstTag = m.group(1);
            }
            addition = "Output HTML; start the translation with " + firstTag;
        }
        List<GPTChatMessage> messages = template.getMessages(
                ImmutableMap.of("sourcelanguage", sourceLanguage != null ? sourceLanguage : "guess it from the text",
                        "sourcephrase", text,
                        "targetlanguage", targetLanguage,
                        "addition", addition));
        request.addMessages(messages);
        // set request.setMaxTokens to about 2 times the number of words in the text to translate
        // since that seems a generous limit for the translation, but give a leeway for error messages.
        // this splitting is quite an overestimation, but that's better than underestimating in this context.
        int maxTokens = 2 * text.split(" |[^a-z]").length + 50;
        request.setMaxTokens(maxTokens);
        return request;
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
        boolean oddChar = true;
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
                    if (oddChar) {
                        result.append(c);
                    } else {
                        result.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
                    }
                    oddChar = !oddChar;
                } else {
                    result.append(c);
                    oddChar = true;
                }
            }
        }
        return result.toString();
    }


    private String cacheKey(GPTChatRequest request) {
        if (cacheDir == null) {
            return null;
        }
        int hash = 17;
        for (char c : request.toString().toCharArray()) {
            hash = 92821 * hash + c;
        }
        return Integer.toHexString(Math.abs(hash));
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
        }
        return null;
    }


    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
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
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    protected void ensureEnabled() {
        if (config == null || config.disabled()) {
            throw new IllegalStateException("Translation service is currently disabled" +
                    (config == null ? "" : " by configuration"));
        }
    }

    @ObjectClassDefinition(name = "Composum AI Translation Service Configuration",
            description = "Configuration for the Composum AI Translation Service")
    public @interface Config {

        @AttributeDefinition(name = "Disable the Autotranslate service", defaultValue = "true")
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
    }
}
