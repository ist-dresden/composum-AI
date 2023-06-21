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
}
