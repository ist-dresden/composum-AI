package com.composum.ai.backend.base.service.chat;

/**
 * Represents the details of a function used as a tool in the chat completion request.
 * Includes the function's name, description, parameters, and an optional strict flag.
 */
public class GPTFunctionDetails {

    private final String name;
    private final String description;
    private final Object parameters;  // Arbitrary JSON schema
    private final Boolean strict;

    /**
     * Creates the object
     *
     * @param name        The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     * @param description A brief description of what the function does. Helps the model choose when to call it.
     *                    The parameters accepted by the function, defined as an arbitrary JSON schema object.
     * @param parameters  The parameters accepted by the function, defined as an arbitrary JSON schema object.
     * @param strict      Whether to enforce strict schema adherence for the parameters
     */
    public GPTFunctionDetails(String name, String description, Object parameters, Boolean strict) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.strict = strict;
    }

    /**
     * The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     */
    public String getName() {
        return name;
    }

    /**
     * A brief description of what the function does. Helps the model choose when to call it.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The parameters accepted by the function, defined as an arbitrary JSON schema object.
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * Whether to enforce strict schema adherence for the parameters.
     */
    public Boolean getStrict() {
        return strict;
    }

}
