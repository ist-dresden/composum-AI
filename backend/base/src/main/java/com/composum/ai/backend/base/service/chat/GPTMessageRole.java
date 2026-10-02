package com.composum.ai.backend.base.service.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Role of a {@link GPTChatMessage} in a dialog with ChatGPT.
 *
 * @see "https://platform.openai.com/docs/guides/chat"
 */
public enum GPTMessageRole {

    /**
     * The system message helps set the behavior of the assistant.
     */
    @JsonProperty("system")
    SYSTEM("system"),
    /**
     * The user messages help instruct the assistant.
     */
    @JsonProperty("user")
    USER("user"),
    /**
     * The assistant messages help store prior responses. It can also serve as an example of desired behavior.
     */
    @JsonProperty("assistant")
    ASSISTANT("assistant"),

    /**
     * A result of a tool call the assistant made.
     */
    @JsonProperty("tool")
    TOOL("tool");

    private final String externalRepresentation;

    GPTMessageRole(String externalRepresentation) {
        this.externalRepresentation = externalRepresentation;
    }

    @Override
    public String toString() {
        return externalRepresentation;
    }
}
