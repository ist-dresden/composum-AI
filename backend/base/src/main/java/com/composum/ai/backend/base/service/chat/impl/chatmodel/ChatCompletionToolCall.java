package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a tool call generated by the model in a chat completion response.
 * This can be a function call with specific arguments.
 */
public class ChatCompletionToolCall {

    /**
     * The ID of the tool call.
     */
    @SerializedName("id")
    private String id;

    /**
     * The type of the tool, currently only "function" is supported.
     */
    @SerializedName("type")
    private String type;

    /**
     * The function being called by the model, including its name and arguments.
     */
    @SerializedName("function")
    private ChatFunctionDetails function;

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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