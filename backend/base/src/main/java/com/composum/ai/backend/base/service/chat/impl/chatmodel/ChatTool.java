package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a tool in the OpenAI chat completion request, currently limited to functions.
 * Each tool contains details about the function including name, description, and parameters.
 */
public class ChatTool {

    /**
     * The type of the tool, currently fixed as "function".
     */
    @SerializedName("type")
    private String type = "function";  // currently Always "function"

    /**
     * The details of the function, such as its name, description, and parameters.
     */
    @SerializedName("function")
    private ChatFunctionDetails function;  // Function details object

    // Getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChatFunctionDetails getFunction() {
        return function;
    }

    public void setFunction(ChatFunctionDetails function) {
        this.function = function;
    }
}
