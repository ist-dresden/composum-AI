package com.composum.ai.aem.core.impl.autotranslate;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This serves both as OSGI configuration (default for the configurations) and, if used, as Sling CAConfig configuration.
 */
@ObjectClassDefinition(name = "Composum AI Autotranslate Configuration",
        description = "Configuration of the automatic translation of AEM pages." +
                "The OSGI configuration is only used if no Sling CAConfig configuration is found.")
public @interface AutoTranslateConfig {

    @AttributeDefinition(name = "Debugging UI",
            description = "Enable the Autotranslate debugging UI at at " +
                    "/apps/composum-ai/components/autotranslate/list/list.html , normally disabled. Only read from OSGI configuration.", defaultValue = "false")
    boolean pocUiEnabled() default false;

    @AttributeDefinition(name = "Disable", description = "Disable the Autotranslate service", defaultValue = "false")
    boolean disabled() default false;

    @AttributeDefinition(name = "Denied Resource Types",
            description = "Regexes for denied Resource Types - if the sling:resourceType matches that, " +
                    "then no attributes or child nodes are touched by the automatic translation.")
    String[] deniedResourceTypes() default {};

    @AttributeDefinition(name = "Allowed Additional Attributes",
            description = "Matches for Attributes that are explicitly allowed to be translated, in addition " +
                    "to standard attributes and heuristically recognized attributes. " +
                    "The heuristics is that the value has to have letters and whitespaces.\n" +
                    "Syntax: regular expressions that match resource type % attribute name - e.g. myapp/component/html%markup")
    String[] allowedAttributeRegexes() default {};

    @AttributeDefinition(name = "Denied Attributes",
            description = "Attributes that are explicitly denied to be translated. Can be used to override the " +
                    "heuristics / standard attributes. Same syntax as allowed attributes.")
    String[] deniedAttributesRegexes() default {};

    @AttributeDefinition(name = "Ignore Assets except Content Fragments",
            description = "If true, assets are ignored for translation, except for content fragments. " +
                    "(Otherwise the translator would translate metadata of images and videos.)")
    boolean ignoreAssetsExceptContentFragments() default true;

    @AttributeDefinition(name = "Use High Intelligence Model",
            description = "If true, the translator will use the 'high-intelligence model' (see OpenAI config) for translation. Default: true.")
    boolean useHighIntelligenceModel() default true;

    @AttributeDefinition(name = "Include Already Translated Values",
            description = "If a page is re-translated with only a few modified texts: " +
                    "if set we include the unchanged texts that don't have to be translated again, too, as they are in the target language." +
                    "This might guide the translation of the now (re-)translated properties.")
    boolean includeAlreadyTranslatedValues() default true;

}
