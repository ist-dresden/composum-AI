package com.composum.ai.backend.base.service.chat;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A chat message in a dialog with ChatGPT. Currently limited to at most a text message and an image.
 *
 * @see "https://platform.openai.com/docs/guides/chat"
 */
public class GPTChatMessage {

    private final GPTMessageRole role;
    private final String content;
    private final String imageUrl;
    private final String tool_call_id;
    private final List<GPTToolCall> tool_calls;

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nonnull String content) {
        this(role, content, null);
    }

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nullable String content, @Nullable String imageUrl) {
        this(role, content, imageUrl, null);
    }

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nullable String content, @Nullable String imageUrl, String tool_call_id) {
        this(role, content, imageUrl, tool_call_id, null);
    }

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nullable String content, @Nullable String imageUrl, String tool_call_id, List<GPTToolCall> tool_calls) {
        this.role = role;
        this.content = content;
        this.imageUrl = imageUrl;
        this.tool_call_id = tool_call_id;
        this.tool_calls = tool_calls;
    }

    /**
     * Function of the message in the dialog.
     */
    public GPTMessageRole getRole() {
        return role;
    }

    /**
     * The content of the message, be that an answer of the assistant or a question of the user.
     */
    public String getContent() {
        return content;
    }

    /**
     * The URL with the content of the image to be analyzed.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * The ID of a prior tool call.
     */
    public String getToolCallId() {
        return tool_call_id;
    }

    /**
     * The tool calls that were made in the context of this message.
     */
    @Nullable
    public List<GPTToolCall> getToolCalls() {
        return tool_calls;
    }

    /**
     * String representation only for debugging.
     */
    @Override
    public String toString() {
        return "GPTChatMessage{" +
                "role=" + role +
                (content != null ? ", text='" + content + '\'' : "") +
                (imageUrl != null ? ", imageUrl='" + imageUrl + '\'' : "") +
                (tool_call_id != null ? ", tool_call_id='" + tool_call_id + '\'' : "") +
                (tool_calls != null ? ", tool_calls=" + tool_calls : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTChatMessage)) return false;
        GPTChatMessage that = (GPTChatMessage) o;
        return getRole() == that.getRole() && Objects.equals(getContent(), that.getContent()) &&
                Objects.equals(getImageUrl(), that.getImageUrl()) &&
                Objects.equals(getToolCallId(),  that.getToolCallId()) &&
                Objects.equals(getToolCalls(), that.getToolCalls());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole() != null ? getRole().toString() : "", getContent(), getImageUrl());
    }

}
