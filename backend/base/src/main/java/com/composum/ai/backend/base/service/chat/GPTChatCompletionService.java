package com.composum.ai.backend.base.service.chat;

import java.util.concurrent.Flow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.impl.GPTChatMessagesTemplate;

/**
 * Raw abstraction of the ChatGPT chat interface, with only the details that are needed.
 * <p>
 * This does deliberately not use the OpenAI API classes because we want to be able to switch to a different API implementation, and hide their complexity from the rest of the code. If we need special parameters,
 * we will add new methods with more specific function.
 */
public interface GPTChatCompletionService {

    /**
     * The simplest case: give some messages and get a single response.
     * If the response can be more than a few words, do consider using {@link #streamingChatCompletion(GPTChatRequest, GPTCompletionCallback)} instead,
     * to give the user some feedback while waiting.
     */
    @Nullable
    String getSingleChatCompletion(@Nonnull GPTChatRequest request) throws GPTException;

    /**
     * Give some messages and receive the streaming response via callback, to reduce waiting time.
     * It possibly waits if a rate limit is reached, but otherwise returns immediately after scheduling an asynchronous call.
     * The asynchronous call can be aborted by calling {@link Flow.Subscription#cancel()} on the subscription that is
     * presented to {@link GPTCompletionCallback#onSubscribe(Flow.Subscription)} once the call is established.
     */
    @Nonnull
    void streamingChatCompletion(@Nonnull GPTChatRequest request, @Nonnull GPTCompletionCallback callback) throws GPTException;

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

    /**
     * Helper for preprocessing HTML so that it can easily read by ChatGPT.
     */
    @Nonnull
    String htmlToMarkdown(@Nullable String html);

    /**
     * Opposite of {@link #markdownToHtml(String)}.
     */
    String markdownToHtml(String markdown);

    /**
     * Counts the number of tokens for the text for the normally used model. Caution: message boundaries need some tokens
     * and slicing text might create a token or two, too, so do not exactly rely on that.
     */
    int countTokens(@Nullable String text);

    /**
     * Whether ChatGPT completion is enabled. If not, calling the methods that access ChatGPT throws an IllegalStateException.
     */
    boolean isEnabled();

}
