package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import com.google.gson.annotations.SerializedName;

public class ChatFunctionDetails {

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("parameters")
    private Object parameters;  // Arbitrary JSON schema

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
