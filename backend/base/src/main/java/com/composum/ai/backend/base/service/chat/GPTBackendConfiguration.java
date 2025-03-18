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

    @AttributeDefinition(name = "Backend ID",
            description = "A unique identifier for this backend configuration.")
    String backendId();

    @AttributeDefinition(name = "API Endpoint",
            description = "The base URL for the LLM API endpoint.")
    String apiEndpoint();

    @AttributeDefinition(name = "API Key",
            description = "The API key used for authentication with the LLM backend.")
    String apiKey();

    @AttributeDefinition(name = "Connection Timeout",
            description = "The connection timeout in seconds.",
            defaultValue = "30")
    int connectionTimeout();

    @AttributeDefinition(name = "Request Timeout",
            description = "The request timeout in seconds.",
            defaultValue = "300")
    int requestTimeout();

    @AttributeDefinition(name = "Requests Per Minute",
            description = "The maximum number of requests allowed per minute.")
    int requestsPerMinute();

    @AttributeDefinition(name = "Requests Per Hour",
            description = "The maximum number of requests allowed per hour.")
    int requestsPerHour();

    @AttributeDefinition(name = "Requests Per Day",
            description = "The maximum number of requests allowed per day.")
    int requestsPerDay();

    @AttributeDefinition(name = "Models",
            description = "Comma-separated list of supported models for this backend.")
    String models();

    @AttributeDefinition(name = "Additional Header 1 Key",
            description = "An optional additional HTTP header key for authentication or other purposes.")
    String additionalHeader1Key();

    @AttributeDefinition(name = "Additional Header 1 Value",
            description = "The value for the first additional HTTP header.")
    String additionalHeader1Value();

    @AttributeDefinition(name = "Additional Header 2 Key",
            description = "An optional second additional HTTP header key.")
    String additionalHeader2Key();

    @AttributeDefinition(name = "Additional Header 2 Value",
            description = "The value for the second additional HTTP header.")
    String additionalHeader2Value();
}
