package com.composum.ai.composum.bundle.model;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Possible values for the {@link LabelExtensionModel#ATTRIBUTE_AIVISIBLE}. To be flexible to allow only some assistants, we allow a comma separated list of values there and the first matching value tells the visibility. E.g. !create,all says all but create, translate,none says only translate. If nothing matches, the default is taken - currently true.
 */
public enum LabelExtensionVisibilityKey {

    /**
     * The ai is not visible.
     */
    NONE,
    /**
     * The ai is visible.
     */
    ALL,
    /**
     * Matches the translation assistant.
     */
    TRANSLATE,
    /**
     * Matches the creation dialog.
     */
    CREATE,
    /**
     * Matches the categorize dialog
     */
    CATEGORIZE;

    /**
     * A regex that matches all correct visibility declarations: a comma separated list (spaces permitted) of true, false, a name of one of the dialog constants or ! and the name of one of the dialog constants, case insensitive.
     */
    public static final Pattern VALID_EXPRESSIONS =
            Pattern.compile("\\s*(true|false|all|none|translate|create|categorize|!translate|!create|!categorize)(\\s*,\\s*(true|false|all|none|translate|create|categorize|!translate|!create|!categorize))*\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Default for visibility. Don't rely on that not changing.
     */
    public static final boolean DEFAULTVISIBILITY = true;

    private static final Logger LOG = LoggerFactory.getLogger(LabelExtensionVisibilityKey.class);

    public static boolean isVisible(@Nullable String value, @Nullable LabelExtensionVisibilityKey assistant) {
        if (value == null || value.isBlank()) {
            return DEFAULTVISIBILITY;
        }
        // log warning if the value is not valid
        if (!VALID_EXPRESSIONS.matcher(value).matches()) {
            LOG.warn("Invalid value for aivisible: {}", value);
        }

        // Translation of the regex:
        String[] values = value.split("\\s*,\\s*");

        for (String v : values) {
            if (v.equalsIgnoreCase(ALL.name()) || v.equalsIgnoreCase("true")) {
                return true;
            }
            // none and false
            if (v.equalsIgnoreCase(NONE.name()) || v.equalsIgnoreCase("false")) {
                return false;
            }
            if (v.equalsIgnoreCase(assistant.name())) {
                return true;
            }
            // !assistantname
            if (v.startsWith("!") && v.substring(1).equalsIgnoreCase(assistant.name())) {
                return false;
            }
        }
        return DEFAULTVISIBILITY;
    }

}
