package com.composum.ai.backend.base.service.chat;

import com.google.gson.annotations.SerializedName;

/**
 * Role of a {@link GPTChatMessage} in a dialog with ChatGPT.
 *
 * @see "https://platform.openai.com/docs/guides/chat"
 */
public enum GPTMessageRole {

    /**
     * The system message helps set the behavior of the assistant.
     */
    @SerializedName("system")
    SYSTEM("system"),
    /**
     * The user messages help instruct the assistant.
     */
    @SerializedName("user")
    USER("user"),
    /**
     * The assistant messages help store prior responses. It can also serve as an example of desired behavior.
     */
    @SerializedName("assistant")
    ASSISTANT("assistant");

    private final String externalRepresentation;

    GPTMessageRole(String externalRepresentation) {
        this.externalRepresentation = externalRepresentation;
    }

    @Override
    public String toString() {
        return externalRepresentation;
    }
}
