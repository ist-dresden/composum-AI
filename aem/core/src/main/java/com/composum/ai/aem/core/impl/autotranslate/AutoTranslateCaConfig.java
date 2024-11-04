package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation Configuration",
        description = "Configures rollout details for automatic translation.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateCaConfig {

    @Property(label = "Additional Instructions", order = 1,
            description = "Additional instructions for the automatic translation.")
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

    // String values "true" and "false" are used for boolean properties since we have three states: true, false, and unset.
    @Property(label = "Include Full Page during Retranslation", order = 5,
            description = "If true we do not only provide changed texts to the AI during re-translating a page with some changes," +
                    "but give the entire page to provide better context. That is a bit slower and a bit more expensive, but likely" +
                    "improves the result. This overrides the default from OSGI configuration.",
            property = {
                    "widgetType=dropdown",
                    "dropdownOptions=["
                            + "{'value':'','description':'Default from global OSGI configuration'},"
                            + "{'value':'true','description':'Always include the full page text during retranslation to provide context'},"
                            + "{'value':'false','description':'Only include changed texts during retranslation (faster but probably less quality)'}"
                            + "]"
            }
    )
    String includeFullPageInRetranslation();

    // String values "true" and "false" are used for boolean properties since we have three states: true, false, and unset.
    @Property(label = "Include Existing Translations in Retranslation", order = 6,
            description = "If true, when retranslating a page with some changes we provide" +
                    "the existing translations of that page to the AI as well as additional context with examples. " +
                    "That is a bit slower and a bit more expensive, but likely improves the result." +
                    "This overrides the default from OSGI configuration.",
            property = {
                    "widgetType=dropdown",
                    "dropdownOptions=["
                            + "{'value':'','description':'Default from global OSGI configuration'},"
                            + "{'value':'true','description':'During retranslation give the current translated text in the target page as context'},"
                            + "{'value':'false','description':'Do not give the current translated text as context (faster but probably less quality)'}"
                            + "]"
            })
    String includeExistingTranslationsInRetranslation();

    @Property(label = "Optional Comment", order = 7,
            description = "An optional comment about the configuration, for documentation purposes (not used by the translation).",
            property = {
                    "widgetType=textarea",
                    "textareaRows=2"
            })
    String comment();

    @Property(label = "Temperature", order = 8,
            description = "Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0")
    String temperature();

}
