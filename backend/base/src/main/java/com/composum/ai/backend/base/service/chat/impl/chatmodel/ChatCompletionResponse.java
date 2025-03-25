package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.google.gson.annotations.SerializedName;

/**
 * Represents the response from the OpenAI chat completion API, containing details
 * about the generated choices, token usage, and metadata like the model used and creation time.
 */
public class ChatCompletionResponse {

    /**
     * The unique identifier for this chat completion response.
     */
    @SerializedName("id")
    private String id;

    /**
     * The type of object returned, typically 'chat.completion'.
     */
    @SerializedName("object")
    private String object;

    /**
     * The timestamp (in epoch seconds) when this response was created.
     */
    @SerializedName("created")
    private long created;

    /**
     * The model used for this chat completion, e.g., 'gpt-4'.
     */
    @SerializedName("model")
    private String model;

    /**
     * An optional fingerprint of the system that generated this response.
     */
    @SerializedName("system_fingerprint")
    private String systemFingerprint;

    /**
     * The list of choices the model generated, each with a message and finish reason.
     */
    @SerializedName("choices")
    private List<ChatCompletionChoice> choices;

    /**
     * Token usage information for this completion, including total, prompt, and completion tokens.
     */
    @SerializedName("usage")
    private ChatCompletionUsage usage;

    /** Special response from claude that has no actual content. */
    @SerializedName("type")
    private String type;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemFingerprint() {
        return systemFingerprint;
    }

    public void setSystemFingerprint(String systemFingerprint) {
        this.systemFingerprint = systemFingerprint;
    }

    public List<ChatCompletionChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ChatCompletionChoice> choices) {
        this.choices = choices;
    }

    public ChatCompletionUsage getUsage() {
        return usage;
    }

    public void setUsage(ChatCompletionUsage usage) {
        this.usage = usage;
    }

    /** Special response "ping" from Anthropic Claude that has no actual content. */
    public String getType() {
        return type;
    }

    public enum FinishReason {
        @SerializedName("stop")
        STOP,
        @SerializedName("length")
        LENGTH,
        @SerializedName("content_filter")
        CONTENT_FILTER,
        @SerializedName("tool_calls")
        TOOL_CALLS,
        @Deprecated
        @SerializedName("function_call")
        FUNCTION_CALL,
        ;

        public static GPTFinishReason toGPTFinishReason(FinishReason finishReason) {
            if (finishReason == null) {
                return null;
            }
            switch (finishReason) {
                case STOP:
                    return GPTFinishReason.STOP;
                case LENGTH:
                    return GPTFinishReason.LENGTH;
                case CONTENT_FILTER:
                    return GPTFinishReason.CONTENT_FILTER;
                case TOOL_CALLS:
                    return GPTFinishReason.TOOL_CALLS;
                case FUNCTION_CALL:
                    return GPTFinishReason.FUNCTION_CALL;
                default:
                    throw new IllegalArgumentException("Unknown finish reason: " + finishReason);
            }
        }
    }
}
