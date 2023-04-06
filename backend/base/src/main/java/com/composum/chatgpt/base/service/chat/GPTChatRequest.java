package com.composum.chatgpt.base.service.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * A request to ChatGPT.
 */
public class GPTChatRequest {

    private final ArrayList<GPTChatMessage> messages = new ArrayList<>();

    /**
     * Builder style adding of messages.
     *
     * @return this
     */
    public GPTChatRequest addMessage(GPTMessageRole role, String content) {
        messages.add(new GPTChatMessage(role, content));
        return this;
    }

    /**
     * Builder style adding of messages.
     *
     * @return this
     */
    public GPTChatRequest addMessages(List<GPTChatMessage> messages) {
        this.messages.addAll(messages);
        return this;
    }

    /**
     * Returns the chat messages set with {@link #addMessage(GPTMessageRole, String)}.
     */
    public List<GPTChatMessage> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "GPTChatRequest{" +
                "messages=" + messages +
                '}';
    }

}
