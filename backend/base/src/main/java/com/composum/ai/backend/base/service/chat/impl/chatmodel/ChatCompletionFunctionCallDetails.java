package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the a call of a function used as a tool in the chat completion request.
 */
public class ChatCompletionFunctionCallDetails {

    /**
     * The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     */
    @SerializedName("name")
    private String name;

    /**
     * A JSON for the arguments the function is called with.
     */
    @SerializedName("arguments")
    private String arguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

}
