package com.composum.ai.backend.base.service.chat;

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

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nonnull String content) {
        this.role = role;
        this.content = content;
        this.imageUrl = null;
    }

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nullable String content, @Nullable String imageUrl) {
        this.role = role;
        this.content = content;
        this.imageUrl = imageUrl;
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
     * String representation only for debugging.
     */
    @Override
    public String toString() {
        return "GPTChatMessage{" +
                "role=" + role +
                (content != null ? ", text='" + content + '\'' : "") +
                (imageUrl != null ? ", imageUrl='" + imageUrl + '\'' : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTChatMessage)) return false;
        GPTChatMessage that = (GPTChatMessage) o;
        return getRole() == that.getRole() && Objects.equals(getContent(), that.getContent()) &&
                Objects.equals(getImageUrl(), that.getImageUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole() != null ? getRole().toString() : "", getContent(), getImageUrl());
    }

}
