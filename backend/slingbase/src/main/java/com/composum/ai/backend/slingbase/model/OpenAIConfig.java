package com.composum.ai.backend.slingbase.model;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI OpenAI Configuration",
        description = "Configurations for the OpenAI backend for Composum AI",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface OpenAIConfig {

    @Property(label = "OpenAI API Key", description = "OpenAI API Key from https://platform.openai.com/. If not given, this falls back to the OSGI configuration, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .")
    String openAiApiKey();

}
