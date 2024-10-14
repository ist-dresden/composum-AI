package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the message content in a chat completion choice, including the role (e.g., user, assistant)
 * and the actual text content of the message.
 */
public class ChatCompletionChoiceMessage {

    /**
     * The role of the message (e.g., user, assistant, or system).
     */
    @SerializedName("role")
    private ChatCompletionRequest.Role role;

    /**
     * The text content of the message.
     */
    @SerializedName("content")
    private String content;

    // Getters and setters
    public ChatCompletionRequest.Role getRole() {
        return role;
    }

    public void setRole(ChatCompletionRequest.Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
