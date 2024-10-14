package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a choice in the chat completion response. Each choice may include a message,
 * a delta (for streaming responses), and a finish reason indicating why the completion stopped.
 */
public class ChatCompletionChoice {

    /**
     * The position of this choice in the list of choices returned by the API.
     */
    @SerializedName("index")
    private int index;

    /**
     * The message content associated with this choice.
     */
    @SerializedName("message")
    private ChatCompletionChoiceMessage message;

    /**
     * Used for incremental updates (streaming responses), represents partial message content.
     */
    @SerializedName("delta")
    private ChatCompletionChoiceMessage delta;

    /**
     * The reason why the completion stopped (e.g., length, stop signal).
     */
    @SerializedName("finish_reason")
    private ChatCompletionResponse.FinishReason finishReason;

    // Getters and setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ChatCompletionChoiceMessage getMessage() {
        return message;
    }

    public void setMessage(ChatCompletionChoiceMessage message) {
        this.message = message;
    }

    /**
     * Alternative to {@link #getMessage()} if it's a response chunk.
     */
    public ChatCompletionChoiceMessage getDelta() {
        return delta;
    }

    public void setDelta(ChatCompletionChoiceMessage delta) {
        this.delta = delta;
    }

    public ChatCompletionResponse.FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(ChatCompletionResponse.FinishReason finishReason) {
        this.finishReason = finishReason;
    }
}
