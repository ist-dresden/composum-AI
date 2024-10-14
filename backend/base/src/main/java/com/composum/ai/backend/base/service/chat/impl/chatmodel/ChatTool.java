package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatTool {

    @SerializedName("type")
    private String type;  // Always "function"

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