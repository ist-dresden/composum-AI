package com.composum.ai.backend.base.service.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A request to ChatGPT.
 */
public class GPTChatRequest {

    private static final Logger LOG = LoggerFactory.getLogger(GPTChatRequest.class);

    @Nonnull
    private final List<GPTChatMessage> messages = new ArrayList<>();
    private Integer maxTokens;
    private GPTConfiguration configuration;

    public GPTChatRequest() {
    }

    public GPTChatRequest(List<GPTChatMessage> messages) {
        if (messages != null) {
            this.messages.addAll(messages);
        }
    }

    public GPTChatRequest(GPTConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Builder style adding of messages.
     *
     * @return this
     */
    public GPTChatRequest addMessage(GPTMessageRole role, String content) {
        messages.add(new GPTChatMessage(role, content));
        return this;
    }

    /**
     * Builder style adding of messages.
     *
     * @return this
     */
    public GPTChatRequest addMessages(List<GPTChatMessage> messages) {
        this.messages.addAll(messages);
        return this;
    }

    /**
     * Returns the chat messages set with {@link #addMessage(GPTMessageRole, String)}.
     */
    @Nonnull
    public List<GPTChatMessage> getMessages() {
        return messages;
    }

    /**
     * Optionally, sets the maximum number of tokens (approx. 0.75 words).
     */
    public GPTChatRequest setMaxTokens(Integer maxTokens) {
        if (maxTokens != null && maxTokens > 0) {
            this.maxTokens = maxTokens;
        }
        return this;
    }

    /**
     * Returns a new request with either maxTokens set if that's given, or just an empty request.
     */
    @Nonnull
    public static GPTChatRequest ofMaxTokens(@Nullable Integer maxTokens) {
        GPTChatRequest result = new GPTChatRequest();
        if (maxTokens != null && maxTokens > 0) {
            result.setMaxTokens(maxTokens);
        }
        return result;
    }

    /**
     * Optionally, sets the configuration.
     */
    public GPTChatRequest setConfiguration(@Nullable GPTConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Sets the LLM configuration
     */
    @Nullable
    public GPTConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Optionally the maximum number of tokens (approx. 0.75 words).
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * Merges in additional parameters: maxtokens overwrites, if there is a system message it's appended to the
     * current one, and the other messages are added at the back.
     *
     * @throws IllegalArgumentException if we already have a configuration and the additional parameters have a different one
     */
    public void mergeIn(@Nullable GPTChatRequest additionalParameters) {
        if (additionalParameters != null) {
            if (additionalParameters.getMaxTokens() != null) {
                setMaxTokens(additionalParameters.getMaxTokens());
            }

            if (additionalParameters.getConfiguration() != null) {
                setConfiguration(GPTConfiguration.merge(getConfiguration(), additionalParameters.getConfiguration()));
            }

            if (additionalParameters.getMessages() != null) {
                for (GPTChatMessage message : additionalParameters.getMessages()) {
                    if (message.getRole() == GPTMessageRole.SYSTEM) {
                        String currentSystemMessage = messages.stream()
                                .filter(m -> m.getRole() == GPTMessageRole.SYSTEM)
                                .map(GPTChatMessage::getContent)
                                .reduce((a, b) -> a + "\n\n" + b)
                                .orElse(null);
                        String addSystemMessage = message.getContent();
                        String newSystemMessage = currentSystemMessage == null ? addSystemMessage :
                                currentSystemMessage + "\n\n" + addSystemMessage;
                        if (currentSystemMessage != null) {
                            messages.removeIf(m -> m.getRole() == GPTMessageRole.SYSTEM);
                        }
                        messages.add(0, new GPTChatMessage(GPTMessageRole.SYSTEM, newSystemMessage));
                    } else {
                        addMessages(Arrays.asList(message));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "GPTChatRequest{" +
                "messages=" + messages +
                ", maxTokens=" + maxTokens +
                ", configuration=" + configuration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTChatRequest)) return false;
        GPTChatRequest that = (GPTChatRequest) o;
        return Objects.equals(getMessages(), that.getMessages()) && Objects.equals(getMaxTokens(), that.getMaxTokens()) && Objects.equals(getConfiguration(), that.getConfiguration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMessages(), getMaxTokens(), getConfiguration());
    }
}
