package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatCompletionChoice {

    @SerializedName("index")
    private int index;

    @SerializedName("message")
    private ChatCompletionMessage message;

    @SerializedName("finish_reason")
    private ChatCompletionResponse.FinishReason finishReason;

    // Getters and setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ChatCompletionMessage getMessage() {
        return message;
    }

    public void setMessage(ChatCompletionMessage message) {
        this.message = message;
    }

    public ChatCompletionResponse.FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(ChatCompletionResponse.FinishReason finishReason) {
        this.finishReason = finishReason;
    }
}
