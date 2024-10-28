package com.composum.ai.backend.base.service.chat;

import javax.annotation.Nullable;

public enum GPTFinishReason {

    /**
     * The response is complete.
     */
    STOP,

    /**
     * The response is not complete, but the maximum length has been reached.
     */
    LENGTH,

    /**
     * Model decided to call a function.
     */
    @Deprecated
    FUNCTION_CALL,

    /**
     * Model decided to call one or more tools.
     */
    TOOL_CALLS,

    /**
     * Omitted content due to a flag from our content filters.
     */
    CONTENT_FILTER;

    @Nullable
    public static GPTFinishReason fromChatGPT(@Nullable String chatGptRepresentation) {
        if (chatGptRepresentation == null || chatGptRepresentation.trim().isEmpty()) {
            return null;
        }
        switch (chatGptRepresentation) {
            case "stop":
                return STOP;
            case "length":
                return LENGTH;
            case "function_call":
                return FUNCTION_CALL;
            case "content_filter":
                return CONTENT_FILTER;
            default:
                throw new IllegalArgumentException("Unknown finish reason: " + chatGptRepresentation);
        }
    }

}
