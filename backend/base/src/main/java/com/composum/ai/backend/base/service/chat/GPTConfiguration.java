package com.composum.ai.backend.base.service.chat;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A configuration to use for accessing the external LLM service. That currently contains the API key for ChatGPT,
 * but could later include information about which LLM to use, template language, rate limiting, etc.
 * This can be a separate parameter to a request, or implicitly contained in a passed GPTChatRequest.
 */
public class GPTConfiguration {

    public enum AnswerType {
        /**
         * The default if nothing is requested - markdown is the "native" output form at least of ChatGPT.
         */
        MARKDOWN,
        /**
         * Real HTML or, more likely, richtext as simplified HTML.
         */
        HTML,
        /**
         * JSON is not yet supported by all models, so a bit dangerous.
         */
        JSON
    }

    public enum Mode {
        /**
         * Uses a system prompt appropriate for text generation.
         */
        GENERATE,
        /**
         * Uses a system prompt appropriate for a chat.
         */
        CHAT
    }

    private final String apiKey;

    private final String organizationId;

    private final AnswerType answerType;

    private final String additionalInstructions;

    private final Mode mode;

    private final Boolean highIntelligenceNeeded;

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType) {
        this(apiKey, organizationId, answerType, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions) {
        this(apiKey, organizationId, answerType, additionalInstructions, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode) {
        this(apiKey, organizationId, answerType, additionalInstructions, mode, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode, @Nullable Boolean highIntelligenceNeeded) {
        this.apiKey = apiKey;
        this.answerType = answerType;
        this.organizationId = organizationId;
        this.additionalInstructions = additionalInstructions;
        this.mode = mode;
        this.highIntelligenceNeeded = highIntelligenceNeeded;
    }

    /**
     * The API key to use with ChatGPT or another service. If this isnot set, we will try to fall back to global configurations.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * The organization id to use with ChatGPT. If this is not set, we will try to fall back to global configurations.
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * The type of answer we want from the LLM.
     */
    public AnswerType getAnswerType() {
        return answerType;
    }

    /**
     * Optionally, additional instructions to add to the system prompt.
     */
    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public boolean isHtml() {
        return answerType == AnswerType.HTML;
    }

    public Mode getMode() {
        return mode;
    }

    /** Uses the slower and more expensive high intelligence model - use sparingly for more challenging tasks. */
    public Boolean isHighIntelligenceNeeded() {
        return highIntelligenceNeeded;
    }

    /**
     * Creates a configuration that joins the values.
     *
     * @throws IllegalArgumentException if values conflict
     */
    public GPTConfiguration merge(@Nullable GPTConfiguration other) throws IllegalArgumentException {
        if (other == null) {
            return this;
        }
        String apiKey = this.apiKey != null ? this.apiKey : other.apiKey;
        if (this.apiKey != null && other.apiKey != null && !this.apiKey.equals(other.apiKey)) {
            throw new IllegalArgumentException("Cannot merge conflicting API keys: " + this.apiKey + " vs. " + other.apiKey);
        }
        String organizationId = this.organizationId != null ? this.organizationId : other.organizationId;
        if (this.organizationId != null && other.organizationId != null && !this.organizationId.equals(other.organizationId)) {
            throw new IllegalArgumentException("Cannot merge conflicting organization ids: " + this.organizationId + " vs. " + other.organizationId);
        }
        AnswerType answerType = this.answerType != null ? this.answerType : other.answerType;
        if (this.answerType != null && other.answerType != null && !this.answerType.equals(other.answerType)) {
            throw new IllegalArgumentException("Cannot merge conflicting answer types: " + this.answerType + " vs. " + other.answerType);
        }
        String additionalInstructions = this.additionalInstructions == null ? other.additionalInstructions :
                other.additionalInstructions == null ? this.additionalInstructions : this.additionalInstructions + "\n\n" + other.additionalInstructions;
        Mode mode = this.mode != null ? this.mode : other.mode;
        return new GPTConfiguration(apiKey, organizationId, answerType, additionalInstructions, mode);
    }

    /**
     * {@link #merge(GPTConfiguration)} several configurations.
     */
    @Nullable
    public static GPTConfiguration merge(@Nullable GPTConfiguration first, @Nullable GPTConfiguration second) {
        return first != null ? first.merge(second) : second;
    }

    /**
     * Returns {@link #HTML} if richText is true, {@link #MARKDOWN} otherwise.
     */
    @Nonnull
    public static GPTConfiguration ofRichText(boolean richText) {
        return new GPTConfiguration(null, null, richText ? AnswerType.HTML : AnswerType.MARKDOWN);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GPTConfiguration{");
        if (apiKey != null) {
            sb.append("apiKey='").append(apiKey).append('\'');
        }
        if (organizationId != null) {
            sb.append(", organizationId='").append(organizationId).append('\'');
        }
        if (answerType != null) {
            sb.append(", answerType=").append(answerType);
        }
        if (additionalInstructions != null) {
            sb.append(", additionalInstructions='").append(additionalInstructions).append('\'');
        }
        if (mode != null) {
            sb.append(", mode=").append(mode);
        }
        if (highIntelligenceNeeded != null) {
            sb.append(", highIntelligenceNeeded=").append(highIntelligenceNeeded);
        }
        return sb.append('}').toString();
    }

    public static final GPTConfiguration MARKDOWN = new GPTConfiguration(null, null, AnswerType.MARKDOWN);
    public static final GPTConfiguration HTML = new GPTConfiguration(null, null, AnswerType.HTML);
    public static final GPTConfiguration JSON = new GPTConfiguration(null, null, AnswerType.JSON);
    public static final GPTConfiguration CHAT = new GPTConfiguration(null, null, null, null, Mode.CHAT);
    public static final GPTConfiguration GENERATE = new GPTConfiguration(null, null, null, null, Mode.GENERATE);
    /** Requests slower and more expensive "high intelligence" model - use sparingly. */
    public static final GPTConfiguration HIGH_INTELLIGENCE = new GPTConfiguration(null, null, null, null, null, true);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPTConfiguration)) return false;
        GPTConfiguration that = (GPTConfiguration) o;
        return Objects.equals(getApiKey(), that.getApiKey()) && getAnswerType() == that.getAnswerType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApiKey(), getAnswerType());
    }
}
