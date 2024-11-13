package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.Collections;
import java.util.List;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a message in a chat completion request, containing the role of the speaker
 * (user, assistant, or system) and the message content, which may include text or other parts.
 */
public class ChatCompletionMessage {

    /**
     * The role of the speaker for this message, such as 'user', 'assistant', 'system', or 'tool'.
     */
    @SerializedName("role")
    private ChatCompletionRequest.Role role;

    /**
     * The content of the message, which may include text or other parts (like images).
     */
    @SerializedName("content")
    @JsonAdapter(ChatCompletionMessagePart.ChatCompletionMessagePartListDeSerializer.class)
    private List<ChatCompletionMessagePart> content;

    /**
     * The ID of the tool call that this message is responding to.
     * Only applicable when the role is 'tool'.
     */
    @SerializedName("tool_call_id")
    private String toolCallId;

    /** List of tool calls that should be executed. */
    @SerializedName("tool_calls")
    private List<ChatCompletionToolCall> toolCalls;

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
        result.setToolCallId(message.getToolCallId());
        result.setToolCalls(ChatCompletionToolCall.make(message.getToolCalls()));
        return result;
    }

    // Getters and setters
    public ChatCompletionRequest.Role getRole() {
        return role;
    }

    public void setRole(ChatCompletionRequest.Role role) {
        this.role = role;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public List<ChatCompletionMessagePart> getContent() {
        return content;
    }

    public void setContent(List<ChatCompletionMessagePart> content) {
        this.content = content;
    }

    public List<ChatCompletionToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ChatCompletionToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public boolean isEmpty(Void ignoreJustPreventSerialization) {
        boolean contentIsEmpty = content == null || content.isEmpty() ||
                !content.stream().anyMatch(m -> !m.isEmpty(null));
        return contentIsEmpty && (toolCallId == null || toolCallId.isEmpty()) &&
                (toolCalls == null || toolCalls.isEmpty());
    }
}
