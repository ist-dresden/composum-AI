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

    @SerializedName("response_format")
    private ResponseFormat responseFormat;

    @SerializedName("seed")
    private Integer seed;

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

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
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

    public enum ResponseFormatType {
        @SerializedName("text")
        TEXT,
        @SerializedName("json_object")
        JSON_OBJECT;

        public static ResponseFormatType make(String type) {
            switch (type) {
                case "text":
                    return TEXT;
                case "json_object":
                    return JSON_OBJECT;
                default:
                    throw new IllegalArgumentException("Unknown response format type " + type);
            }
        }
    }

    public static class ResponseFormat {
        @SerializedName("type")
        private ResponseFormatType type;

        public ResponseFormatType getType() {
            return type;
        }

        public void setType(ResponseFormatType type) {
            this.type = type;
        }
    }

    public static final ResponseFormat JSON = new ResponseFormat();
    {{
        JSON.setType(ResponseFormatType.JSON_OBJECT);
    }};

}
