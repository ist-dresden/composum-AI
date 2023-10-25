package com.composum.ai.backend.base.service.chat;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A chat message in a dialog with ChatGPT.
 *
 * @see "https://platform.openai.com/docs/guides/chat"
 */
public class GPTChatMessage {

    private final GPTMessageRole role;
    private final String content;

    public GPTChatMessage(@Nonnull GPTMessageRole role, @Nonnull String content) {
        this.role = role;
        this.content = content;
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
     * String representation only for debugging.
     */
    @Override
    public String toString() {
        return "GPTChatMessage{" +
                "role=" + role +
                ", text='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTChatMessage)) return false;
        GPTChatMessage that = (GPTChatMessage) o;
        return getRole() == that.getRole() && Objects.equals(getContent(), that.getContent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole(), getContent());
    }

}
