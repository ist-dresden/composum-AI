package com.composum.ai.aem.core.impl.autotranslate.rollout;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation POC Rollout Configuration",
        description = "Configures rollout details for automatic translation. This is a demo and not yet fully functional.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateLiveActionConfig {

    @Property(label = "Additional Instructions", description = "Additional instructions for the automatic translation.")
    String additionalInstructions();

}
