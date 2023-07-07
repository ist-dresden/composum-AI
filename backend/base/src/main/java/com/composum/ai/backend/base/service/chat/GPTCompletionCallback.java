package com.composum.ai.backend.base.service.chat;

import java.util.concurrent.Flow;

/**
 * For a streaming mode this is given as parameter for the method call and receives the streamed data; the method returns only when the response is complete.
 */
public interface GPTCompletionCallback extends Flow.Subscriber<String> {

    /**
     * This is called when the response is complete to specify the reason (e.g. {@link GPTFinishReason#STOP}
     * when done or {@link GPTFinishReason#LENGTH} if the maximum length has been reached).
     */
    void onFinish(GPTFinishReason finishReason);

    @Override
    default void onComplete() {
        // empty because we have onFinish.
    }

    /**
     * For debugging: this sets the internal ID that is used for logging purposes. Not a good ID give to the world, though.
     */
    void setLoggingId(String loggingId);

    /**
     * For debugging - the request that was sent to ChatGPT as JSON.
     */
    default void setRequest(String json) {
    }
}
