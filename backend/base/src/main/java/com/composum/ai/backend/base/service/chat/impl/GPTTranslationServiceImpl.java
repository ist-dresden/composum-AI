package com.composum.ai.backend.base.service.chat.impl;

import java.util.List;
import java.util.Map;
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
import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.google.common.base.Strings;

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
    public String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage, boolean richtext) {
        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(sourceLanguage) || Strings.isNullOrEmpty(targetLanguage)) {
            return "";
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, richtext);
        String response = chatCompletionService.getSingleChatCompletion(request);
        response = response.trim();
        LOG.debug("Returning result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, response);
        return response;
    }

    @Override
    public void streamingSingleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage, boolean richtext, @Nonnull GPTCompletionCallback callback) throws GPTException {
        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(sourceLanguage) || Strings.isNullOrEmpty(targetLanguage)) {
            throw new IllegalArgumentException("Empty text or languages");
        }

        GPTChatRequest request = makeRequest(text, sourceLanguage, targetLanguage, richtext);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    public static final Pattern HTML_TAG_AT_START = Pattern.compile("\\A\\s*(<[^>]*>)");

    private GPTChatRequest makeRequest(String text, String sourceLanguage, String targetLanguage, boolean richtext) {
        // fetch the GPTChatMessagesTemplate, replace the placeholders and call the chatCompletionService
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_SINGLETRANSLATION);
        GPTChatRequest request = new GPTChatRequest();
        String addition = "";
        if (richtext) {
            Matcher m = HTML_TAG_AT_START.matcher(text);
            String firstTag = "<p>";
            if (m.find()) {
                firstTag = m.group(1);
            }
            addition = "Output HTML; start the translation with " + firstTag;
        }
        List<GPTChatMessage> messages = template.getMessages(Map.of("sourcelanguage", sourceLanguage, "sourcephrase", text, "targetlanguage", targetLanguage, "addition", addition));
        request.addMessages(messages);
        // set request.setMaxTokens to about 2 times the number of words in the text to translate
        // since that seems a generous limit for the translation, but give a leeway for error messages.
        // this splitting is quite an overestimation, but that's better than underestimating in this context.
        int maxTokens = 2 * text.split(" |[^a-z]").length + 50;
        request.setMaxTokens(maxTokens);
        return request;
    }

}
