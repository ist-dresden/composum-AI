package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigServiceImpl.isTranslatableProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class AutoTranslateConfigServiceImplTest {

    @Test
    public void testIsTranslatableProperty() {
        // Test with certainly translatable properties
        for (String propertyName : AutoTranslateConfigServiceImpl.CERTAINLY_TRANSLATABLE_PROPERTIES) {
            assertTrue(isTranslatableProperty(propertyName, "Some value"));
        }

        // Test with property name containing colon
        assertFalse(isTranslatableProperty("property:name", "Some value"));

        // Test with non-string value
        assertFalse(isTranslatableProperty("propertyName", new Object()));

        // Test with string value starting with /content/, /apps/, /libs/, /mnt/
        assertFalse(isTranslatableProperty("propertyName", "/content/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/apps/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/libs/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/mnt/someValue"));

        // Test with property name starting with AI_PREFIX or LC_PREFIX and ending with AI_TRANSLATED_SUFFIX or AI_ORIGINAL_SUFFIX
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.AI_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.AI_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.LC_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.LC_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, "Some value"));

        // Test with string value without whitespace and 4 letter sequence
        assertFalse(isTranslatableProperty("propertyName", "abc"));

        // Test with string value with whitespace but without 4 letter sequence
        assertFalse(isTranslatableProperty("propertyName", "a b c"));

        // Test with string value with 4 letter sequence but without whitespace
        assertFalse(isTranslatableProperty("propertyName", "abcd"));

        // Test with string value with whitespace and 4 letter sequence
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh"));


        // Test with string value with multiple whitespace sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // Test with string value with multiple whitespace sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // that's likely a boolean property
        assertFalse(isTranslatableProperty("displayPopupTitle", "true"));
        assertFalse(isTranslatableProperty("displayPopupTitle", "false"));

        // dates are not translateable
        assertFalse(isTranslatableProperty("propertyName", "2022-01-01"));
        assertFalse(isTranslatableProperty("propertyName", "2022-01-01T00:00:00.000Z"));
        assertFalse(isTranslatableProperty("propertyName", new Date().toString()));

        // rather do not translate anything that looks like a boolean, even if it's a whitelisted property
        assertFalse(isTranslatableProperty("shortDescription", "true"));
        assertFalse(isTranslatableProperty("jcr:title", "false"));
    }

}
