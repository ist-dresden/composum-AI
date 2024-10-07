package com.composum.ai.backend.slingbase.model;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Composum AI Prompt Library Configuration", description = "Location for the prompt library for Composum AI. " +
        "There can be multiple configurations, and the allowed services are aggregated.\n" +
        "There is a fallback configuration that is used if no other configuration is found, " +
        "and a factory for multiple configurations which override the fallback configuration if present. " +
        "If configured, Sling Context Aware Configuration takes precedence over OSGI configuration")
@Configuration(label = "Composum AI Prompt Library Configuration",
        description = "Location for the prompt library for Composum AI",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface GPTPromptLibrary {

    @AttributeDefinition(name = "Content Creation Prompts Path",
            description = "Path to the content creation prompts. Either a JSON file, or a page.")
    @Property(label = "Content Creation Prompts Path",
            description = "Path to the content creation prompts. Either a JSON file, or a page.",
            property = {"widgetType=pathbrowser"}
    )
    String contentCreationPromptsPath();

    @AttributeDefinition(name = "Side Panel Prompts Path",
            description = "Path to the side panel prompts. Either a JSON file, or a page.")
    @Property(label = "Side Panel Prompts Path",
            description = "Path to the side panel prompts. Either a JSON file, or a page.",
            property = {"widgetType=pathbrowser"})
    String sidePanelPromptsPath();

}
