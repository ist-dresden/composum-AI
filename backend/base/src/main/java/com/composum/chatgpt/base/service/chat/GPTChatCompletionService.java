package com.composum.chatgpt.base.service.chat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.composum.chatgpt.base.service.GPTException;
import com.composum.chatgpt.base.service.chat.impl.GPTChatMessagesTemplate;

/**
 * Raw abstraction of the ChatGPT chat interface, with only the details that are needed.
 * <p>
 * This does deliberately not use the OpenAI API classes because we want to be able to switch to a different API implementation, and hide their complexity from the rest of the code. If we need special parameters,
 * we will add new methods with more specific function.
 */
public interface GPTChatCompletionService {

    /**
     * The simplest case: give some messages and get a single response.
     */
    String getSingleChatCompletion(GPTChatRequest request) throws GPTException;

    /**
     * Retrieves a (usually cached) chat template with that name. Mostly for backend internal use.
     * The templates are retrieved from the bundle resources at "chattemplates/", and are cached.
     *
     * @param templateName the name of the template to retrieve, e.g. "singleTranslation" .
     */
    @Nonnull
    GPTChatMessagesTemplate getTemplate(@Nonnull String templateName) throws GPTException;

    /**
     * Helper method to shorten texts by taking out the middle if too long.
     * In texts longer than this many words we replace the middle with ... since ChatGPT can only process a limited
     * number of words / tokens and in the introduction or summary there is probably the most information about the text.
     * The output has then maxwords words, including the ... marker.
     *
     * @param text     the text to shorten
     * @param maxwords the maximum number of words in the output
     */
    @Nonnull
    String shorten(@Nullable String text, int maxwords) throws GPTException;
}
