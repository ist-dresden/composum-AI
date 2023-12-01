package com.composum.ai.backend.base.service.chat;

/**
 * For a streaming mode this is given as parameter for the method call and receives the streamed data; the method returns only when the response is complete.
 */
public interface GPTCompletionCallback {

    /**
     * This is called when the response is complete to specify the reason (e.g. {@link GPTFinishReason#STOP}
     * when done or {@link GPTFinishReason#LENGTH} if the maximum length has been reached).
     */
    void onFinish(GPTFinishReason finishReason);

    /**
     * Called when a couple of characters come in.
     */
    void onNext(String chars);

    /**
     * Called when an error occurs.
     */
    void onError(Throwable throwable);

    /**
     * For debugging: this sets the internal ID that is used for logging purposes. Not a good ID give to the world, though.
     */
    void setLoggingId(String loggingId);

    /**
     * For debugging - the request that was sent to ChatGPT as JSON.
     */
    default void setRequest(String json) {
    }

    /**
     * A simple collector that just takes note of things.
     */
    public static class GPTCompletionCollector implements GPTCompletionCallback {

        private StringBuilder buffer = new StringBuilder();
        private Throwable throwable;
        private GPTFinishReason finishReason;

        @Override
        public void onFinish(GPTFinishReason finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public void onNext(String chars) {
            buffer.append(chars);
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void setLoggingId(String loggingId) {
        }

        public GPTFinishReason getFinishReason() {
            return finishReason;
        }

        public String getResult() {
            return buffer.toString();
        }

        public Throwable getError() {
            return throwable;
        }
    }

}
