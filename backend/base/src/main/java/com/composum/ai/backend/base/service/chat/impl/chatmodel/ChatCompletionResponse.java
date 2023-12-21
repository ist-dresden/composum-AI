package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.google.gson.annotations.SerializedName;

public class ChatCompletionResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("created")
    private long created;

    @SerializedName("model")
    private String model;

    @SerializedName("system_fingerprint")
    private String systemFingerprint;

    @SerializedName("choices")
    private List<ChatCompletionChoice> choices;

    @SerializedName("usage")
    private ChatCompletionUsage usage;

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

    public enum FinishReason {
        @SerializedName("stop")
        STOP,
        @SerializedName("length")
        LENGTH,
        @SerializedName("content_filter")
        CONTENT_FILTER;

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
                default:
                    throw new IllegalArgumentException("Unknown finish reason: " + finishReason);
            }
        }
    }
}
