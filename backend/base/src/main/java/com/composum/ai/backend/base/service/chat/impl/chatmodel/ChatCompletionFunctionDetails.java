package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the details of a function used as a tool in the chat completion request.
 * Includes the function's name, description, parameters, and an optional strict flag.
 */
public class ChatCompletionFunctionDetails {

    /**
     * The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     */
    @SerializedName("name")
    private String name;

    /**
     * A brief description of what the function does. Helps the model choose when to call it.
     */
    @SerializedName("description")
    private String description;

    /**
     * The parameters accepted by the function, defined as an arbitrary JSON schema object.
     */
    @SerializedName("parameters")
    private Object parameters;  // Arbitrary JSON schema

    /**
     * Whether to enforce strict schema adherence for the parameters.
     */
    @SerializedName("strict")
    private Boolean strict;

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }
}
