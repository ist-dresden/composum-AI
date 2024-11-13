package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Property;

/**
 * A rule to be added to the Composum AI Automatic Translation Configuration
 * with translation instructions for pages matching the rule.
 * We have properties: pathRegex, contentRegex, additionalInstructions.
 */
public @interface AutoTranslateRuleConfig {

    @Property(label = "Path Regex", order = 1,
            description = "A regular expression matching the absolute path to the page, incl. jcr:content. " +
                    "E.g. .*/home/products/.* will match all pages under .../home/products/. If empty every page will match" +
                    "if the content pattern condition is met.")
    String pathRegex();

    @Property(label = "Content Pattern", order = 2,
            description = "A word or phrase that must be present in the content of the page for the rule to match. " +
                    "E.g. 'Product' will match all pages that contain the word 'Product', case-insensitive. Spaces will also match any whitespace. " +
                    "If it contains any of the regex meta characters []|()*+ it'll be treated as a regular expression." +
                    "If empty every page will match if the path condition is met.")
    String contentPattern();

    @Property(label = "Additional Instructions", order = 3,
            description = "Additional instructions for the automatic translation in case this rule matches.",
            property = {
                    "widgetType=textarea",
                    "textareaRows=5"
            })
    String additionalInstructions();

    @Property(label = "Optional Comment (for documentation, not used by AI)", order = 4,
            description = "An optional comment for the rule, for documentation purposes (not used by the translation).",
            property = {
                    "widgetType=textarea",
                    "textareaRows=2"
            })
    String comment();

}
