package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation Configuration",
        description = "Configures rollout details for automatic translation.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateCaConfig {

    @Property(label = "Additional Instructions", description = "Additional instructions for the automatic translation.")
    String additionalInstructions();

    @Property(label = "Prefer High Intelligence Model", description = "If set, the high intelligence model will be used for translation.")
    boolean preferHighIntelligenceModel();

    @Property(label = "Prefer Standard Model", description = "If set, the standard model will be used for translation. Opposite of 'Prefer High Intelligence Model'.")
    boolean preferStandardModel();

}
