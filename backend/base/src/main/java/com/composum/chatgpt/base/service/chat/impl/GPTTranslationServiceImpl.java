package com.composum.chatgpt.base.service.chat.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTTranslationService;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
@Component(service = GPTTranslationService.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GPTTranslationServiceImpl implements GPTTranslationService {

    /**
     * Template for {@link GPTChatMessagesTemplate} to translate a single word or phrase. Has placeholders
     * ${sourcelanguage} ${sourcephrase} and ${targetlanguage}.
     */
    public static final String SINGLETRANSLATION_TEMPLATE = "singleTranslation";

    // annotation to inject that OSGI service
    @Reference
    protected GPTChatCompletionService chatCompletionService;

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    public String singleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage) {
        // fetch the GPTChatMessagesTemplate, replace the placeholders and call the chatCompletionService
        GPTChatMessagesTemplate template = new GPTChatMessagesTemplate(null, SINGLETRANSLATION_TEMPLATE);
        GPTChatRequest request = new GPTChatRequest();
        List<GPTChatMessage> messages = template.getMessages(Map.of("sourcelanguage", sourceLanguage, "sourcephrase", text, "targetlanguage", targetLanguage));
        request.addMessages(messages);
        // set request.setMaxTokens to about 2 times the number of words in the text to translate
        // since that seems a likely limit for the translation
        request.setMaxTokens(2 * text.split("\\s+").length);
        String response = chatCompletionService.getSingleChatCompletion(request);
        return response;
    }

}
