package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatCompletionMessage {

    @SerializedName("role")
    private ChatCompletionRequest.Role role;

    @SerializedName("content")
    private Object content; // To support both text and image_url types

    // Getters and setters
    public ChatCompletionRequest.Role getRole() {
        return role;
    }

    public void setRole(ChatCompletionRequest.Role role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
