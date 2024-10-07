package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation Configuration",
        description = "Configures rollout details for automatic translation.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateCaConfig {

    @Property(label = "Additional Instructions", order = 1,
            description = "Additional instructions for the automatic translation.",
            property = {
                    "widgetType=textarea",
                    "textareaRows=8"
            })
    String additionalInstructions();

    @Property(label = "Rules for additional Instructions", order = 2,
            description = "Rules that give additional instructions for translation if certain words or phrases are present in the page.")
    AutoTranslateRuleConfig[] rules() default {};

    @Property(label = "Prefer High Intelligence Model", order = 3,
            description = "If set, the high intelligence model will be used for translation.")
    boolean preferHighIntelligenceModel();

    @Property(label = "Prefer Standard Model", order = 4,
            description = "If set, the standard model will be used for translation. Opposite of 'Prefer High Intelligence Model'.")
    boolean preferStandardModel();

    @Property(label = "Include Full Page during Retranslation", order = 5,
            description = "If true we do not only provide changed texts to the AI during re-translating a page with some changes," +
                    "but give the entire page to provide better context. That is a bit slower and a bit more expensive, but likely" +
                    "improves the result. This overrides the default from OSGI configuration.")
    boolean[] includeFullPageInRetranslation();

    @Property(label = "Include Existing Translations in Retranslation", order = 6,
            description = "If true, when retranslating a page with some changes we provide" +
                    "the existing translations of that page to the AI as well as additional context with examples. " +
                    "That is a bit slower and a bit more expensive, but likely improves the result." +
                    "This overrides the default from OSGI configuration.")
    boolean[] includeExistingTranslationsInRetranslation() default true;

    @Property(label = "Dropdown Param", description = "Parameter with dropdown list.", property = {
            "widgetType=dropdown",
            "dropdownOptions=["
                    + "{'value':'','description':'First option'},"
                    + "{'value':'true','description':'Second option'},"
                    + "{'value':'false','description':'Third option'}"
                    + "]"
    })
    String dropdownParam();
}
