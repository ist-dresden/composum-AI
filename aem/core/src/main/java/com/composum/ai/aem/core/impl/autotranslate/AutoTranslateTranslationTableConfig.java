package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.caconfig.annotation.Property;

/**
 * Configures a translation table for the automatic translation - an XLS or CSV file of terms and their translations.
 * Properties: path to file resource or DAM asset, sheet index, start row, key column, value column.
 */
public @interface AutoTranslateTranslationTableConfig {

    @Property(label = "Path to XLS or CSV File", order = 1,
            description = "The JCR path to the XLS or CSV file containing the translation table. " +
                    "Can be either a file resource or an AEM asset.")
    String path();

    @Property(label = "Sheet Index", order = 2,
            description = "The index of the sheet in the XLS file containing the translation table. " +
                    "The first sheet is 1. Ignored for CSV files.")
    int sheetIndex();

    @Property(label = "Start Row", order = 3,
            description = "The row in the sheet where the translation table starts. " +
                    "The first row is 1, following Excel conventions.")
    int startRow();

    @Property(label = "Key Column", order = 4,
            description = "The column in the sheet containing the keys (terms to be translated). " +
                    "The first column is A (following Excel conventions) or 1.")
    String keyColumn();

    @Property(label = "Value Column", order = 5,
            description = "The column in the sheet containing the values (translations). " +
                    "The first column is A (following Excel conventions) or 1.")
    String valueColumn();

    @Property(label = "Optional Comment (for documentation, not used by AI)", order = 4,
            description = "An optional comment for the rule, for documentation purposes (not used by the translation).",
            property = {
                    "widgetType=textarea",
                    "textareaRows=2"
            })
    String comment();

}
