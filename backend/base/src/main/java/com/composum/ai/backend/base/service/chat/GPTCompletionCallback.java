package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * For tool calls: set the context to execute actions in.
     */
    default GPTToolExecutionContext getToolExecutionContext() {
        return null;
    }

    /**
     * For debugging - the request that was sent to ChatGPT as JSON.
     */
    default void setRequest(String json) {
    }

    /**
     * Notifies that the request is completely finished / closed, nothing more will arrive.
     */
    default void close() {
        // empty
    }

    /**
     * Called when a tool call is made.
     */
    default void toolDelta(List<GPTToolCall> toolCalls) {
        // empty
    }

    /**
     * For tool calls: context to execute actions in.
     */
    public interface GPTToolExecutionContext {
        // empty here - has to be specifiec in other layers
    }

    /**
     * Forwards all methods to a delegate.
     */
    public static class GPTCompletionCallbackWrapper implements GPTCompletionCallback {

        @Nonnull
        protected GPTCompletionCallback delegate;

        public GPTCompletionCallbackWrapper(@Nonnull GPTCompletionCallback delegate) {
            this.delegate = delegate;
        }

        public void onFinish(GPTFinishReason finishReason) {
            delegate.onFinish(finishReason);
        }

        public void onNext(String chars) {
            delegate.onNext(chars);
        }

        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        public void setLoggingId(String loggingId) {
            delegate.setLoggingId(loggingId);
        }

        public void setRequest(String json) {
            delegate.setRequest(json);
        }

        public void close() {
            delegate.close();
        }

        public void toolDelta(List<GPTToolCall> toolCalls) {
            delegate.toolDelta(toolCalls);
        }

        public GPTToolExecutionContext getToolExecutionContext() {
            return delegate.getToolExecutionContext();
        }

    }

    /**
     * A simple collector that just takes note of things.
     */
    public static class GPTCompletionCollector implements GPTCompletionCallback {

        private static final Logger LOG = LoggerFactory.getLogger(GPTCompletionCollector.class);

        private StringBuilder buffer = new StringBuilder();
        private Throwable throwable;
        private GPTFinishReason finishReason;
        private List<GPTToolCall> toolCalls;

        @Override
        public void onFinish(GPTFinishReason finishReason) {
            LOG.debug("Finished with reason {} :\n{}", finishReason, buffer);
            this.finishReason = finishReason;
        }

        @Override
        public void onNext(String chars) {
            buffer.append(chars);
        }

        @Override
        public void onError(Throwable throwable) {
            LOG.debug("Error: ", throwable);
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

        @Override
        public void toolDelta(List<GPTToolCall> toolCalls) {
            this.toolCalls = GPTToolCall.mergeDelta(this.toolCalls, toolCalls);
        }

        public List<GPTToolCall> getToolCalls() {
            return toolCalls;
        }

        @Override
        public GPTToolExecutionContext getToolExecutionContext() {
            return null;
        }

    }

}
