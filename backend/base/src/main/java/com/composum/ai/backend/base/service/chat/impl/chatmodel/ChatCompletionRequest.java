package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ChatCompletionRequest {

    @SerializedName("model")
    private String model;

    @SerializedName("messages")
    private List<ChatCompletionMessage> messages;

    @SerializedName("max_tokens")
    private int maxTokens;

    @SerializedName("stream")
    private boolean stream;

    // Getters and setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatCompletionMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatCompletionMessage> messages) {
        this.messages = messages;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public enum Role {
        @SerializedName("user")
        USER,
        @SerializedName("assistant")
        ASSISTANT,
        @SerializedName("system")
        SYSTEM
    }

    public enum Type {
        @SerializedName("text")
        TEXT,
        @SerializedName("image_url")
        IMAGE_URL
    }
}
