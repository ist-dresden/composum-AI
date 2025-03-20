package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI Automatic Translation Configuration",
        description = "Configures rollout details for automatic translation.",
        property = {"category=Composum-AI"})
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface AutoTranslateCaConfig {

    @Property(label = "Additional Instructions (Deprecated)", order = 1,
            description = "Additional instructions for the automatic translation. Deprecated, please use 'Rules for additional Instructions' instead - if you do not give a path regex nor a content pattern the instructions will be used everywhere.")
    String additionalInstructions();

    @Property(label = "Rules for additional Instructions", order = 5,
            description = "Rules that give additional instructions for translation if certain words or phrases are present in the page.")
    AutoTranslateRuleConfig[] rules() default {};

    @Property(label = "Translation Tables", order = 10,
            description = "Translation tables for the automatic translation - XLS or CSV files of terms and their translations. This is an alternative to translation rules if there are many 'Translate X as Y' rules.")
    AutoTranslateTranslationTableConfig[] translationTables() default {};

    @Property(label = "Model", order = 20,
            description = "The model to use for translation, if different form the global default. ")
    String model();

    // String values "true" and "false" are used for boolean properties since we have three states: true, false, and unset.
    @Property(label = "Include Full Page during Retranslation", order = 30,
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
    @Property(label = "Include Existing Translations in Retranslation", order = 40,
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

    @Property(label = "Optional Notes (for your documentation, not used by the application)", order = 100,
            description = "An optional comment about the configuration, for documentation purposes (not used by the translation).",
            property = {
                    "widgetType=textarea",
                    "textareaRows=2"
            })
    String comment();

    @Property(label = "Temperature", order = 25,
            description = "Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0")
    String temperature();

    @Property(label = "Translation Table Rule Text", order = 50,
            description = "Optional pattern to create translation rules from translation tables. " +
                    "{0} is the word in the source language, {1} the word in the target language. " +
                    "If not given we use a default: Translate '{0}' as '{1}'.")
    String translationTableRuleText();

}
