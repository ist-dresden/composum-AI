package com.composum.ai.backend.base.service.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private final Boolean debug;

    private final Double temperature;

    private final Integer seed;

    private final List<GPTContextInfo> contexts;


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
        this(apiKey, organizationId, answerType, additionalInstructions, mode, highIntelligenceNeeded, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode, @Nullable Boolean highIntelligenceNeeded, @Nullable Boolean debug) {
        this(apiKey, organizationId, answerType, additionalInstructions, mode, highIntelligenceNeeded, debug, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode, @Nullable Boolean highIntelligenceNeeded, @Nullable Boolean debug, @Nullable Double temperature) {
        this(apiKey, organizationId, answerType, additionalInstructions, mode, highIntelligenceNeeded, debug, temperature, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode, @Nullable Boolean highIntelligenceNeeded, @Nullable Boolean debug, @Nullable Double temperature, @Nullable Integer seed) {
        this(apiKey, organizationId, answerType, additionalInstructions, mode, highIntelligenceNeeded, debug, temperature, seed, null);
    }

    public GPTConfiguration(@Nullable String apiKey, @Nullable String organizationId, @Nullable AnswerType answerType, @Nullable String additionalInstructions, @Nullable Mode mode, @Nullable Boolean highIntelligenceNeeded, @Nullable Boolean debug, @Nullable Double temperature, @Nullable Integer seed, @Nullable List<GPTContextInfo> contexts) {
        this.apiKey = apiKey;
        this.answerType = answerType;
        this.organizationId = organizationId;
        this.additionalInstructions = additionalInstructions;
        this.mode = mode;
        this.highIntelligenceNeeded = highIntelligenceNeeded;
        this.debug = debug;
        this.temperature = temperature;
        this.seed = seed;
        this.contexts = contexts;
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

    /**
     * If true, this requires to uses the slower and more expensive high intelligence model - use sparingly for more challenging tasks.
     */
    public boolean highIntelligenceNeededIsSet() {
        return Boolean.TRUE.equals(highIntelligenceNeeded);
    }

    /**
     * If true, there is no information whether a "high intelligence model" is used - uses the default.
     */
    public boolean highIntelligenceNeededIsUnset() {
        return highIntelligenceNeeded == null;
    }

    /**
     * If this is set, then the services will not call the AI and return the JSON request instead of the AI response.
     */
    public Boolean getDebug() {
        return debug;
    }

    /**
     * The sampling temperature, between 0 and 1. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * If specified, our system will make a best effort to sample deterministically, such that repeated requests with
     * the same seed and parameters should return the same result. Determinism is not guaranteed, and you should refer
     * to the system_fingerprint response parameter to monitor changes in the backend. Beta feature of OpenAI.
     */
    public Integer getSeed() {
        return seed;
    }

    /**
     * Creates a configuration that joins the values.
     *
     * @throws IllegalArgumentException if values conflict - that's checked for apiKey and organizationId and answerType.
     */
    public GPTConfiguration merge(@Nullable GPTConfiguration other) throws IllegalArgumentException {
        return merge(other, false);
    }

    /**
     * Creates a configuration that joins the values.
     *
     * @param override if true, values set in this configuration will override values set in the other configuration.
     *                 Otherwise, a conflict will throw an exception.
     * @param other    the other configuration to merge with (optional)
     * @return a new configuration
     * @throws IllegalArgumentException if values conflict
     */
    public GPTConfiguration merge(@Nullable GPTConfiguration other, boolean override) throws IllegalArgumentException {
        if (other == null) {
            return this;
        }
        String apiKey = this.apiKey != null ? this.apiKey : other.apiKey;
        if (!override && this.apiKey != null && other.apiKey != null && !this.apiKey.equals(other.apiKey)) {
            throw new IllegalArgumentException("Cannot merge conflicting API keys: " + this.apiKey + " vs. " + other.apiKey);
        }
        String organizationId = this.organizationId != null ? this.organizationId : other.organizationId;
        if (!override && this.organizationId != null && other.organizationId != null && !this.organizationId.equals(other.organizationId)) {
            throw new IllegalArgumentException("Cannot merge conflicting organization ids: " + this.organizationId + " vs. " + other.organizationId);
        }
        AnswerType answerType = this.answerType != null ? this.answerType : other.answerType;
        if (!override && this.answerType != null && other.answerType != null && !this.answerType.equals(other.answerType)) {
            throw new IllegalArgumentException("Cannot merge conflicting answer types: " + this.answerType + " vs. " + other.answerType);
        }
        String additionalInstructions = this.additionalInstructions == null ? other.additionalInstructions :
                other.additionalInstructions == null ? this.additionalInstructions : this.additionalInstructions + "\n\n" + other.additionalInstructions;
        Mode mode = this.mode != null ? this.mode : other.mode;
        Boolean highIntelligenceNeeded = this.highIntelligenceNeeded != null ? this.highIntelligenceNeeded : other.highIntelligenceNeeded;
        Boolean debug = this.debug != null ? this.debug : other.debug;
        if (this.debug != null && other.debug != null && (this.debug || other.debug)) {
            debug = true;
        }
        Double temperature = this.temperature != null ? this.temperature : other.temperature;
        Integer seed = this.seed != null ? this.seed : other.seed;
        List<GPTContextInfo> contextInfos = new ArrayList<>();
        if (this.contexts != null) {
            contextInfos.addAll(this.contexts);
        }
        if (other.contexts != null) {
            contextInfos.addAll(other.contexts);
        }
        contextInfos = contextInfos.isEmpty() ? null : contextInfos;
        return new GPTConfiguration(apiKey, organizationId, answerType, additionalInstructions, mode, highIntelligenceNeeded, debug, temperature, seed, contextInfos);
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

    @Nonnull
    public static GPTConfiguration ofAdditionalInstructions(@Nullable String additionalInstructions) {
        return new GPTConfiguration(null, null, null, additionalInstructions);
    }

    @Nonnull
    public static GPTConfiguration ofContexts(@Nullable List<GPTContextInfo> contexts) {
        return new GPTConfiguration(null, null, null, null, null, null, null, null, null, contexts);
    }

    @Nonnull
    public static GPTConfiguration ofContext(@Nonnull String title, @Nonnull String text) {
        return ofContexts(Collections.singletonList(new GPTContextInfo(title, text)));
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
        if (debug != null) {
            sb.append(", debug=").append(debug);
        }
        if (temperature != null) {
            sb.append(", temperature=").append(temperature);
        }
        if (seed != null) {
            sb.append(", seed=").append(seed);
        }
        if (contexts != null) {
            sb.append(", contexts=").append(contexts);
        }
        return sb.append('}').toString();
    }

    public static final GPTConfiguration MARKDOWN = new GPTConfiguration(null, null, AnswerType.MARKDOWN);
    public static final GPTConfiguration HTML = new GPTConfiguration(null, null, AnswerType.HTML);
    public static final GPTConfiguration JSON = new GPTConfiguration(null, null, AnswerType.JSON);
    public static final GPTConfiguration CHAT = new GPTConfiguration(null, null, null, null, Mode.CHAT);
    public static final GPTConfiguration GENERATE = new GPTConfiguration(null, null, null, null, Mode.GENERATE);
    /**
     * Requests slower and more expensive "high intelligence" model - use sparingly.
     */
    public static final GPTConfiguration HIGH_INTELLIGENCE = new GPTConfiguration(null, null, null, null, null, true);
    /**
     * Requests faster and less expensive "normal intelligence" model.
     */
    public static final GPTConfiguration STANDARD_INTELLIGENCE = new GPTConfiguration(null, null, null, null, null, false);

    /**
     * If set, the AI services will not call the AI but return the JSON request as response, for debugging purposes.
     */
    public static final GPTConfiguration DEBUG = new GPTConfiguration(null, null, null, null, null, null, true);

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


    /**
     * Something that gives background information to the AI that isn't actually an instruction, like informative texts, RAG results, whatnot.
     * Contains a title and a text.
     */
    public static class GPTContextInfo {

        private final String title;
        private final String text;

        public GPTContextInfo(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "GPTContextInfo{" +
                    "title='" + title + '\'' +
                    ", text='" + text + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GPTContextInfo)) return false;
            GPTContextInfo that = (GPTContextInfo) o;
            return Objects.equals(getTitle(), that.getTitle()) && Objects.equals(getText(), that.getText());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTitle(), getText());
        }

    }

}
