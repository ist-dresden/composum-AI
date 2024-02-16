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

    /**
     * <code><pre>
     * {
     *   "error": {
     *     "message": "This model's maximum context length is 4097 tokens. However, you requested 4671 tokens (1203 in the messages, 3468 in the completion). Please reduce the length of the messages or completion.",
     *     "type": "invalid_request_error",
     *     "param": "messages",
     *     "code": "context_length_exceeded"
     *   }
     * }
     * </pre></code>
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

}
