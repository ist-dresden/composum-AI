package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.composum.ai.backend.base.service.chat.GPTContentCreationService;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;

/**
 * Building on {@link GPTChatCompletionService} this implements generating keywords.
 */
@Component(service = GPTContentCreationService.class)
public class GPTContentCreationServiceImpl implements GPTContentCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTContentCreationServiceImpl.class);

    /**
     * Template for {@link GPTChatMessagesTemplate} to generate keywords from a text. Has placeholder
     * ${text} .
     */
    public static final String TEMPLATE_MAKEKEYWORDS = "makekeywords";

    public static final String TEMPLATE_MAKEDESCRIPTION = "makedescription";

    public static final String TEMPLATE_PROMPTONTEXT = "promptontext";

    public static final String PLACEHOLDER_TEXT = "text";

    public static final String PLACEHOLDER_WORDCOUNTLIMIT = "wordcountlimit";

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
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_MAKEKEYWORDS);
        GPTChatRequest request = new GPTChatRequest();
        String shortenedText = chatCompletionService.shorten(text, MAXWORDS);
        List<GPTChatMessage> messages = template.getMessages(Map.of(PLACEHOLDER_TEXT, shortenedText));
        request.addMessages(messages);
        request.setMaxTokens(50); // pretty arbitrary limit for now, needs testing.
        String response = chatCompletionService.getSingleChatCompletion(request);
        List<String> lines = List.of(response.trim().split("\\s*\n\\s*"));
        lines = lines.stream()
                .filter(l -> !l.isBlank())
                .filter(l -> l.length() < 40) // that should drop weird things and comments
                .map(l -> l.trim())
                .map(l -> l.startsWith("- ") ? l.substring(2) : l)
                .collect(Collectors.toList());
        return lines;
    }

    @Nonnull
    @Override
    public String generateDescription(@Nullable String text, int maxwords) {
        if (text == null || text.isBlank()) {
            return "";
        }
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_MAKEDESCRIPTION);
        GPTChatRequest request = new GPTChatRequest();
        String shortenedText = chatCompletionService.shorten(text, MAXWORDS);
        int maxtokens = 150;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(PLACEHOLDER_TEXT, shortenedText);
        placeholders.put(PLACEHOLDER_WORDCOUNTLIMIT, "");
        if (maxwords > 0) {
            maxtokens = maxwords * 2 + 30; // give a limit to prevent accidents, but give some leeway
            placeholders.put(PLACEHOLDER_WORDCOUNTLIMIT, " Use at most " + maxwords + " words. ");
        }
        request.setMaxTokens(maxtokens);
        List<GPTChatMessage> messages = template.getMessages(placeholders);
        request.addMessages(messages);
        String response = chatCompletionService.getSingleChatCompletion(request);
        return response;
    }

    @Nonnull
    @Override
    public String executePrompt(@Nullable String prompt, int maxwords) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        GPTChatRequest request = makeExecutePromptRequest(prompt, maxwords);
        return chatCompletionService.getSingleChatCompletion(request);
    }

    protected GPTChatRequest makeExecutePromptRequest(String prompt, int maxwords) {
        GPTChatRequest request = new GPTChatRequest();
        int maxtokens = 200;
        if (maxwords > 0) {
            maxtokens = maxwords * 4 / 3;
        }
        request.setMaxTokens(maxtokens);
        request.addMessage(GPTMessageRole.USER, prompt);
        return request;
    }

    @Override
    public void executePromptStreaming(@Nonnull String prompt, int maxwords, @Nonnull GPTCompletionCallback callback) throws GPTException {
        GPTChatRequest request = makeExecutePromptRequest(prompt, maxwords);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    @Nonnull
    @Override
    public String executePromptOnText(@Nullable String prompt, @Nullable String text, int maxwords) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        GPTChatRequest request = makeExecuteOnTextRequest(prompt, text, maxwords);
        return chatCompletionService.getSingleChatCompletion(request);
    }

    protected GPTChatRequest makeExecuteOnTextRequest(String prompt, String text, int maxwords) {
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_PROMPTONTEXT);
        String shortenedText = chatCompletionService.shorten(text, MAXWORDS);
        List<GPTChatMessage> messages = template.getMessages(Map.of(PLACEHOLDER_TEXT, shortenedText, "prompt", prompt));
        GPTChatRequest request = new GPTChatRequest();
        if (maxwords > 0) {
            int maxtokens = maxwords > 0 ? maxwords * 4 / 3 : 200;
            request.setMaxTokens(maxtokens);
        }
        request.addMessages(messages);
        return request;
    }

    @Override
    public void executePromptOnTextStreaming(@Nonnull String prompt, @Nonnull String text, int maxwords, @Nonnull GPTCompletionCallback callback) throws GPTException {
        GPTChatRequest request = makeExecuteOnTextRequest(prompt, text, maxwords);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

}
