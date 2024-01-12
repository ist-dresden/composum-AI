package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.gson.annotations.SerializedName;

public class ChatCompletionRequest {

    @SerializedName("model")
    private String model;

    @SerializedName("messages")
    private List<ChatCompletionMessage> messages;

    @SerializedName("max_tokens")
    private Integer maxTokens;

    @SerializedName("stream")
    private Boolean stream;

    @SerializedName("temperature")
    private Double temperature;

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

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean isStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public enum Role {
        @SerializedName("user")
        USER,
        @SerializedName("assistant")
        ASSISTANT,
        @SerializedName("system")
        SYSTEM;

        public static Role make(GPTMessageRole role) {
            switch (role) {
                case USER:
                    return USER;
                case SYSTEM:
                    return SYSTEM;
                case ASSISTANT:
                    return ASSISTANT;
                default:
                    throw new IllegalArgumentException("Unknown role " + role);
            }
        }
    }
}
