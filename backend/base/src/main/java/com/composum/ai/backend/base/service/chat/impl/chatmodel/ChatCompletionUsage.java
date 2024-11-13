package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the token usage details in a chat completion response, including
 * the number of tokens used for the prompt, the completion, and the total.
 */
public class ChatCompletionUsage {

    /**
     * The number of tokens used for the prompt (input) in this completion.
     */
    @SerializedName("prompt_tokens")
    private int promptTokens;

    /**
     * The number of tokens generated in the completion (output).
     */
    @SerializedName("completion_tokens")
    private int completionTokens;

    /**
     * The total number of tokens used (prompt + completion).
     */
    @SerializedName("total_tokens")
    private int totalTokens;

    // Getters and setters
    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
}
