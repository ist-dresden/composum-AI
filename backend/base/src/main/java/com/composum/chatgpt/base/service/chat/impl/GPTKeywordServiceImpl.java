package com.composum.chatgpt.base.service.chat.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTKeywordService;

/**
 * Building on {@link GPTChatCompletionService} this implements generating keywords.
 */
@Component(service = GPTKeywordService.class)
public class GPTKeywordServiceImpl implements GPTKeywordService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTKeywordServiceImpl.class);

    /**
     * Template for {@link GPTChatMessagesTemplate} to generate keywords from a text. Has placeholder
     * ${text} .
     */
    public static final String KEYWORD_TEMPLATE = "makekeywords";

    public static final String PLACEHOLDER_TEXT = "text";

    /**
     * To respect limits of ChatGPT we replace in texts longer than this many words we replace the middle with [...]
     */
    protected static final int MAXWORDS = 2000;

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    @Nonnull
    @Override
    public List<String> generateKeywords(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        GPTChatMessagesTemplate template = new GPTChatMessagesTemplate(null, KEYWORD_TEMPLATE);
        GPTChatRequest request = new GPTChatRequest();
        String shortenedText = chatCompletionService.shorten(text, MAXWORDS);
        List<GPTChatMessage> messages = template.getMessages(Map.of(PLACEHOLDER_TEXT, shortenedText));
        request.addMessages(messages);
        request.setMaxTokens(50); // pretty arbitrary limit for now, needs testing.
        String response = chatCompletionService.getSingleChatCompletion(request);
        return List.of(response.trim().split("\\s*\n\\s*"));
    }

}
