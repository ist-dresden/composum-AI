package com.composum.chatgpt.base.service.chat.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTTranslationService;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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

    protected Cache<List<String>, String> cache;

    @Activate
    public void activate(GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig config) {
        // FIXME(hps,19.04.23) use more decent implementation or at least make it configurable.
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .expireAfterWrite(120, TimeUnit.MINUTES)
                .maximumSize(128)  // each entry can be at most a few kilobytes, so that'd be less than one megabyte
                .removalListener(notification -> {
                    LOG.debug("Removing translation from cache: {}", notification.getKey());
                })
                .build();
    }

    @Deactivate
    public void deactivate() {
        cache = null;
    }

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    @Nonnull
    public String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage) {
        if (Strings.isNullOrEmpty(text) || Strings.isNullOrEmpty(sourceLanguage) || Strings.isNullOrEmpty(targetLanguage)) {
            return "";
        }
        List<String> cachekey = List.of(sourceLanguage, targetLanguage, text);
        String cached = cache.getIfPresent(cachekey);
        if (cached != null) {
            // log parameters and result
            LOG.debug("Returning cached result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, cached);
            return cached;
        }

        // fetch the GPTChatMessagesTemplate, replace the placeholders and call the chatCompletionService
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_SINGLETRANSLATION);
        GPTChatRequest request = new GPTChatRequest();
        List<GPTChatMessage> messages = template.getMessages(Map.of("sourcelanguage", sourceLanguage, "sourcephrase", text, "targetlanguage", targetLanguage));
        request.addMessages(messages);
        // set request.setMaxTokens to about 2 times the number of words in the text to translate
        // since that seems a likely limit for the translation, but give a leeway for error messages.
        request.setMaxTokens(2 * text.split("\\s+").length + 50);
        String response = chatCompletionService.getSingleChatCompletion(request);
        cache.put(cachekey, response);
        LOG.debug("Returning result: {} -> {} - {} -> {}", sourceLanguage, targetLanguage, text, cached);
        return response;
    }

}
