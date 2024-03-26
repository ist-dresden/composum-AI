package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation POC Configuration",
        description = "Configures rollout details for automatic translation. This is a proof of concept and may not yet be fully functional.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateCaConfig {

    @Property(label = "Additional Instructions", description = "Additional instructions for the automatic translation.")
    String additionalInstructions();

}
