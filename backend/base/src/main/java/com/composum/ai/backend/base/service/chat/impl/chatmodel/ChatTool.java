package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a tool in the OpenAI chat completion request, currently limited to functions.
 * Each tool contains details about the function including name, description, and parameters.
 */
public class ChatTool {

    /**
     * The type of the tool, currently fixed as "function".
     */
    @JsonProperty("type")
    private String type = "function";  // currently Always "function"

    /**
     * The details of the function, such as its name, description, and parameters.
     */
    @JsonProperty("function")
    private ChatCompletionFunctionDetails function;  // Function details object

    // Getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChatCompletionFunctionDetails getFunction() {
        return function;
    }

    public void setFunction(ChatCompletionFunctionDetails function) {
        this.function = function;
    }
}
