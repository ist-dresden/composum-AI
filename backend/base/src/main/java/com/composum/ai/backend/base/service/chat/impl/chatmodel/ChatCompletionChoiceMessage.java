package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatCompletionChoiceMessage {

    @SerializedName("role")
    private ChatCompletionRequest.Role role;

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
