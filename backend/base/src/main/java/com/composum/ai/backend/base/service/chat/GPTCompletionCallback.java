package com.composum.ai.backend.base.service.chat;

/**
 * For a streaming mode this is given as parameter for the method call and receives the streamed data; the method returns only when the response is complete.
 */
public interface GPTCompletionCallback {

    public static enum GPTCompletionStatus {

    }

    /** This is called whenever a new data event comes in. */
    void receiveNextData(String data);

    /** This is called when the response is complete, or aborted because of an error. */
    void receiveFinish(GPTFinishReason finishReason);

}
