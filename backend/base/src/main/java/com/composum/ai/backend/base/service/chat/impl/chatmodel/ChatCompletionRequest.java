package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a request to the OpenAI chat completion API, including model, messages,
 * and optional parameters like max tokens, temperature, and response format.
 */
public class ChatCompletionRequest {

    public static final ResponseFormat JSON = new ResponseFormat();
    /**
     * The AI model to use for the chat completion request, e.g., "gpt-4".
     */
    @SerializedName("model")
    private String model;
    /**
     * The list of messages in the conversation, each with a role (user, assistant, system) and content.
     */
    @SerializedName("messages")
    private List<ChatCompletionMessage> messages;
    /**
     * The maximum number of tokens to generate in the completion.
     */
    @SerializedName("max_tokens")
    private Integer maxTokens;
    /**
     * Whether to stream the response incrementally.
     */
    @SerializedName("stream")
    private Boolean stream;
    /**
     * The sampling temperature, used to control randomness. Values closer to 0 make the output more deterministic.
     */
    @SerializedName("temperature")
    private Double temperature;
    /**
     * The format of the response. Possible values are "text" or "json_object".
     */
    @SerializedName("response_format")
    private ResponseFormat responseFormat;
    /**
     * A seed for deterministic generation, useful for testing or reproducible results.
     */
    @SerializedName("seed")
    private Integer seed;
    /**
     * A list of tools (functions) the model can call during the chat. Each tool contains a type and function details.
     */
    @SerializedName("tools")
    private List<ChatTool> tools;

    {
        {
            JSON.setType(ResponseFormatType.JSON_OBJECT);
        }
    }

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

    public List<ChatTool> getTools() {
        return tools;
    }

    public void setTools(List<ChatTool> tools) {
        this.tools = tools;
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

}
