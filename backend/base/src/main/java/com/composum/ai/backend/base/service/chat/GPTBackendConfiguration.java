package com.composum.ai.backend.base.service.chat;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for a generic LLM backend used by Composum AI.
 * This configuration is defined via OSGi and allows setting API endpoint,
 * authentication, request settings, and model configurations.
 */
@ObjectClassDefinition(
        name = "Composum AI Backend Configuration",
        description = "Configuration for an LLM backend in Composum AI. " +
                "Allows setting API endpoint, authentication, and request settings."
)
public @interface GPTBackendConfiguration {

    @AttributeDefinition(name = "Backend ID", required = true,
            description = "A unique identifier for this backend configuration, e.g. OpenAI")
    String backendId();

    @AttributeDefinition(name = "Disable", description = "Disable this Backend", required = false)
    boolean disabled() default false;

    @AttributeDefinition(name = "API Endpoint", required = true,
            description = "The base URL for the LLM API endpoint, e.g. https://api.openai.com/v1/chat/completions")
    String apiEndpoint();

    @AttributeDefinition(name = "Models",
            description = "Comma-separated list of supported models for this backend, e.g. gpt-4o,gpt-4o-mini,gpt-4.5-preview")
    String models();

    @AttributeDefinition(name = "Additional Header 1 Key, e.g. Authorization", required = false,
            description = "An optional additional HTTP header key for authentication or other purposes.")
    String additionalHeader1Key();

    @AttributeDefinition(name = "Additional Header 1 Value", required = false,
            description = "The value for the first additional HTTP header, e.g. Bearer sk-xyz...")
    String additionalHeader1Value();

    @AttributeDefinition(name = "Additional Header 2 Key, e.g. OpenAI-Organization", required = false,
            description = "An optional second additional HTTP header key.")
    String additionalHeader2Key();

    @AttributeDefinition(name = "Additional Header 2 Value", required = false,
            description = "The value for the second additional HTTP header. e.g. org-xyz")
    String additionalHeader2Value();

    @AttributeDefinition(name = "Additional Header 3 Key", required = false,
            description = "An optional second additional HTTP header key.")
    String additionalHeader3Key();

    @AttributeDefinition(name = "Additional Header 3 Value", required = false,
            description = "The value for the second additional HTTP header.")
    String additionalHeader3Value();

    @AttributeDefinition(name = "Comment", description = "Optional Notes (for your documentation, not used by the application", required = false)
    String comment();

}
