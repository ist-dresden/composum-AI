package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Property;

/**
 * A rule to be added to the translation instructions for pages matching the rule.
 * We have properties: pathRegex, contentRegex, additionalInstructions.
 */
public @interface AutoTranslateRuleConfig {

    @Property(label = "Path Regex", description = "A regular expression matching the absolute path to the page. " +
            "E.g. .*/home/products/.* will match all pages under .../home/products/. If empty every page will match.", order = 1)
    String pathRegex();

    @Property(label = "Content match", description = "A word or phrase that must be present in the content of the page for the rule to match. " +
            "E.g. 'Product' will match all pages that contain the word 'Product'. Spaces will also match any whitespace.", order = 2)
    String contentRegex();

    @Property(label = "Additional Instructions", description = "Additional instructions for the automatic translation in case this rule matches.", order = 3)
    String additionalInstructions();

}
