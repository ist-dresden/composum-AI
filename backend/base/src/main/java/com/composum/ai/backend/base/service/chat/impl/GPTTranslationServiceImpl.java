package com.composum.ai.backend.base.service.chat.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
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

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    @Nonnull
    @Override
    public String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage, @Nullable GPTConfiguration configuration) {
        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(targetLanguage)) {
            return "";
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);
        String response = chatCompletionService.getSingleChatCompletion(request);
        response = response.trim();
        LOG.debug("Returning result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, response);
        return response;
    }

    @Override
    public void streamingSingleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration, @Nonnull GPTCompletionCallback callback) throws GPTException {
        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(sourceLanguage) || Strings.isNullOrEmpty(targetLanguage)) {
            throw new IllegalArgumentException("Empty text or languages");
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, configuration);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    /**
     * Start of separator like `===<<<### 573472 ###>>>===` .
     */
    protected static final String MULTITRANSLATION_SEPARATOR_START = "\n===<<<### ";

    /**
     * End of separator like `573472 ###>>>===` .
     */
    protected static final String MULTITRANSLATION_SEPARATOR_END = " ###>>>===\n";

    /**
     * Regexp matching separator like `===<<<### 573472 ###>>>===` (group "id" matches the number)
     */
    protected static final Pattern MULTITRANSLATION_SEPARATOR_PATTERN = Pattern.compile("\\s*\n===<<<### (?<id>\\d+) ###>>>===\n\\s*");

    protected static final String LASTID = "424242";

    /**
     * We join all text fragments we have to translate into one big texts separated with separators like `===<<<### 573472 ###>>>===` and
     * then translate that. Then we split the result at the separators and return the fragments. Safety check is that the is from the
     * fragments have to match the ids in the result.
     */
    @Nonnull
    @Override
    public List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration) throws GPTException {
        return fragmentedTranslationDivideAndConquer(texts, targetLanguage, configuration, new AtomicInteger(5));
    }

    /**
     * We try to translate the whole lot of texts. If that leads to an exception because we are out of tokens or the response was garbled, we split it into two and translate these individually. If even one text is too long, we are lost and give up.
     */
    protected List<String> fragmentedTranslationDivideAndConquer(@Nonnull List<String> texts, @Nonnull String targetLanguage,
                                                                 @Nullable GPTConfiguration configuration, @Nonnull AtomicInteger permittedRetries) throws GPTException {
        if (permittedRetries.get() <= 0) {
            LOG.error("Too many retries for fragmented translation to {} of {}", targetLanguage, texts);
            throw new GPTException("Too many retries for fragmented translation");
        }
        try {
            return fragmentedTranslation(texts, targetLanguage, configuration, permittedRetries);
        } catch (GPTException.GPTRetryableResponseErrorException e) {
            // is hopefully rare - otherwise we likely have to rethink this.
            LOG.info("Splitting translation because of retryable error: {}", e.toString());
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
        for (String text : texts) {
            String id = "" + Math.round(Math.abs(Math.random() * 1000000));
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
                String part = response.substring(start - 1, end - 1);
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

}
