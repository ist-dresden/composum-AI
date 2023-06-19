package com.composum.ai.backend.base.service.chat;

/**
 * Role of a {@link GPTChatMessage} in a dialog with ChatGPT.
 *
 * @see "https://platform.openai.com/docs/guides/chat"
 */
public enum GPTMessageRole {

    /**
     * The system message helps set the behavior of the assistant.
     */
    SYSTEM("system"),
    /**
     * The user messages help instruct the assistant.
     */
    USER("user"),
    /**
     * The assistant messages help store prior responses. It can also serve as an example of desired behavior.
     */
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
