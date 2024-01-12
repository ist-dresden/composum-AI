package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.Collections;
import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

public class ChatCompletionMessage {

    @SerializedName("role")
    private ChatCompletionRequest.Role role;

    @SerializedName("content")
    @JsonAdapter(ChatCompletionMessagePart.ChatCompletionMessagePartListDeSerializer.class)
    private List<ChatCompletionMessagePart> content;

    // Getters and setters
    public ChatCompletionRequest.Role getRole() {
        return role;
    }

    public void setRole(ChatCompletionRequest.Role role) {
        this.role = role;
    }

    public List<ChatCompletionMessagePart> getContent() {
        return content;
    }

    public void setContent(List<ChatCompletionMessagePart> content) {
        this.content = content;
    }

    public boolean isEmpty(Void ignoreJustPreventSerialization) {
        return content == null || content.isEmpty() ||
                !content.stream().anyMatch(m -> !m.isEmpty(null));
    }

    public static ChatCompletionMessage make(GPTChatMessage message) {
        ChatCompletionMessagePart part;
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            part = ChatCompletionMessagePart.imageUrl(message.getImageUrl());
        } else {
            part = ChatCompletionMessagePart.text(message.getContent());
        }
        ChatCompletionRequest.Role role = ChatCompletionRequest.Role.make(message.getRole());
        ChatCompletionMessage result = new ChatCompletionMessage();
        result.setRole(role);
        result.setContent(Collections.singletonList(part));
        return result;
    }
}
