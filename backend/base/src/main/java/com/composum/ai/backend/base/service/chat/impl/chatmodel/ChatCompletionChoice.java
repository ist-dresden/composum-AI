package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatCompletionChoice {

    @SerializedName("index")
    private int index;

    @SerializedName("message")
    private ChatCompletionChoiceMessage message;

    @SerializedName("delta")
    private ChatCompletionChoiceMessage delta;

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
