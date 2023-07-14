package com.composum.ai.backend.base.service.chat.impl;

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
     * TODO: use an intelligent algorithm to determine this limit, but that's pretty hard for executePromptOnText.
     * 3000 would collide with the 1000 token default for maxtokens, so we use 2800.
     */
    protected static final int MAXTOKENS = 2800;

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
        String shortenedText = chatCompletionService.shorten(text, MAXTOKENS);
        List<GPTChatMessage> messages = template.getMessages(Map.of(PLACEHOLDER_TEXT, shortenedText));
        request.addMessages(messages);
        request.setMaxTokens(50); // pretty arbitrary limit for now, needs testing.
        String response = chatCompletionService.getSingleChatCompletion(request);
        List<String> lines = List.of(response.trim().split("\\s*\n\\s*"));
        lines = lines.stream()
                .filter(l -> !l.isBlank())
                .filter(l -> l.length() < 40) // that should drop weird things and comments
                .map(String::trim)
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
        String shortenedText = chatCompletionService.shorten(text, MAXTOKENS);
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
        return chatCompletionService.getSingleChatCompletion(request);
    }

    @Nonnull
    @Override
    public String executePrompt(@Nullable String prompt, @Nullable GPTChatRequest additionalParameters) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        GPTChatRequest request = makeExecutePromptRequest(prompt, additionalParameters);
        return chatCompletionService.getSingleChatCompletion(request);
    }

    protected GPTChatRequest makeExecutePromptRequest(String prompt, @Nullable GPTChatRequest additionalParameters) {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, prompt);
        request.mergeIn(additionalParameters);
        return request;
    }

    @Override
    public void executePromptStreaming(@Nonnull String prompt, @Nullable GPTChatRequest additionalParameters, @Nonnull GPTCompletionCallback callback) throws GPTException {
        GPTChatRequest request = makeExecutePromptRequest(prompt, additionalParameters);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

    @Nonnull
    @Override
    public String executePromptOnText(@Nullable String prompt, @Nullable String text, @Nullable GPTChatRequest additionalParameters) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        GPTChatRequest request = makeExecuteOnTextRequest(prompt, text, additionalParameters);
        return chatCompletionService.getSingleChatCompletion(request);
    }

    protected GPTChatRequest makeExecuteOnTextRequest(String prompt, String text, @Nullable GPTChatRequest additionalParameters) {
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_PROMPTONTEXT);
        String shortenedText = chatCompletionService.shorten(text, MAXTOKENS);
        // TODO use intelligent algorithm to determine this limit, but that's pretty hard here.
        // also, the user should be alerted about that.
        List<GPTChatMessage> messages = template.getMessages(Map.of(PLACEHOLDER_TEXT, shortenedText, "prompt", prompt));
        GPTChatRequest request = new GPTChatRequest();
        request.addMessages(messages);
        request.mergeIn(additionalParameters);
        return request;
    }

    @Override
    public void executePromptOnTextStreaming(@Nonnull String prompt, @Nonnull String text, @Nullable GPTChatRequest additionalParameters, @Nonnull GPTCompletionCallback callback) throws GPTException {
        GPTChatRequest request = makeExecuteOnTextRequest(prompt, text, additionalParameters);
        chatCompletionService.streamingChatCompletion(request, callback);
    }

}
