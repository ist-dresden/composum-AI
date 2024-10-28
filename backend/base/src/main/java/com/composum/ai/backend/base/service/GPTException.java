package com.composum.ai.backend.base.service;

/**
 * Any kind of error when accessing GPT.
 */
public class GPTException extends RuntimeException {

    public GPTException(String message) {
        super(message);
    }

    public GPTException(String message, Throwable cause) {
        super(message, cause);
    }

    public GPTException(Throwable cause) {
        super(cause);
    }

    public static GPTException buildException(Integer errorStatusCode, String result) {
        // this is annoyingly heuristic and seems to break once in a while.
        if (Integer.valueOf(400).equals(errorStatusCode) && result != null
                && result.contains("invalid_request_error") &&
                (result.contains("context_length_exceeded") || result.contains("max_tokens") ||
                        result.contains("model supports at most") ||
                        result.contains("maximum context length"))) {
            return new GPTContextLengthExceededException(result);
        }
        return new GPTException("Error response from GPT (status " + errorStatusCode
                + ") : " + result);
    }

    /**
     * <pre><code>
     * {
     *   "error": {
     *     "message": "This model's maximum context length is 4097 tokens. However, you requested 4671 tokens (1203 in the messages, 3468 in the completion). Please reduce the length of the messages or completion.",
     *     "type": "invalid_request_error",
     *     "param": "messages",
     *     "code": "context_length_exceeded"
     *   }
     * }
     * </code></pre>
     */
    public static class GPTContextLengthExceededException extends GPTException {
        public GPTContextLengthExceededException(String message) {
            super(message);
        }
    }

    /**
     * An exception that is thrown when the response from the GPT service is not as expected.
     * That might or might not change with an retry - a limited number of retries might make sense.
     */
    public static class GPTRetryableResponseErrorException extends GPTException {
        public GPTRetryableResponseErrorException(String message) {
            super(message);
        }
    }

    /**
     * A special exception if the user should be notified about something instead of completing the task.
     * This is used to transport information in cases where a special return value is not feasible.
     */
    public static class GPTUserNotificationException extends GPTException {
        public GPTUserNotificationException(String message) {
            super(message);
        }
    }

}
