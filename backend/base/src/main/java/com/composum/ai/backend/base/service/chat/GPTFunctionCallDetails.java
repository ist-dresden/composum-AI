package com.composum.ai.backend.base.service.chat;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents the a call of a function used as a tool in the chat completion request.
 */
public class GPTFunctionCallDetails {

    private final String name;
    private final String arguments;

    /**
     * Creates the object
     *
     * @param name      The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     * @param arguments A JSON for the arguments the function is called with.
     */
    public GPTFunctionCallDetails(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    /**
     * The name of the function to be called. This must be unique and can only contain a-z, A-Z, 0-9, underscores, and dashes.
     */
    public String getName() {
        return name;
    }

    /**
     * A JSON for the arguments the function is called with.
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * String representation for debugging.
     */
    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTFunctionCallDetails)) return false;
        GPTFunctionCallDetails that = (GPTFunctionCallDetails) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getArguments(), that.getArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getArguments());
    }

    public GPTFunctionCallDetails mergeDelta(@Nullable GPTFunctionCallDetails function) {
        if (function == null) {
            return this;
        }
        String newName = name == null ? function.name : name;
        String newArguments = arguments;
        if (newArguments == null) {
            newArguments = function.arguments;
        } else if (function.arguments != null) {
            newArguments = newArguments + function.arguments;
        }
        return new GPTFunctionCallDetails(newName, newArguments);
    }

}
