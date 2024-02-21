package com.composum.ai.aem.core.impl.autotranslate;


import static com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateServiceImpl.isTranslatableProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AutoPageTranslateServiceImplTest {

    @Test
    public void testIsTranslatableProperty() {
        // Test with certainly translatable properties
        for (String propertyName : AutoPageTranslateServiceImpl.CERTAINLY_TRANSLATABLE_PROPERTIES) {
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
        assertFalse(isTranslatableProperty(AutoPageTranslateServiceImpl.AI_PREFIX + "propertyName" + AutoPageTranslateServiceImpl.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AutoPageTranslateServiceImpl.AI_PREFIX + "propertyName" + AutoPageTranslateServiceImpl.AI_ORIGINAL_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AutoPageTranslateServiceImpl.LC_PREFIX + "propertyName" + AutoPageTranslateServiceImpl.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AutoPageTranslateServiceImpl.LC_PREFIX + "propertyName" + AutoPageTranslateServiceImpl.AI_ORIGINAL_SUFFIX, "Some value"));

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

    }

}
