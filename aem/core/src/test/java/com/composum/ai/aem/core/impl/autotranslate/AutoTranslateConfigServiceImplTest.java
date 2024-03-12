package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_PREFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.LC_PREFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigServiceImpl.isHeuristicallyTranslatableProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.wcm.testing.mock.aem.junit5.AemContext;

public class AutoTranslateConfigServiceImplTest {

    @Mock
    private AutoTranslateConfig config = Mockito.mock(AutoTranslateConfig.class);

    private AutoTranslateConfigServiceImpl service = new AutoTranslateConfigServiceImpl();

    @Test
    public void testTranslateableAttributes() {
        AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
        Resource res = context.create().resource("/foo", "sling:resourceType", "wknd/components/page", "heuristicAttr", "some text", "allowedAttr", "7", "deniedAttr", "some text");
        Resource sub = context.create().resource("/foo/bar", "subHeuristicAttr", "some text", "subAllowedAttr", "7", "subDeniedAttr", "some text");
        when(config.allowedAttributeRegexes()).thenReturn(
                new String[]{".*%allowedAttr", "wknd/components/page%bar/subAllowedAttr"});
        when(config.deniedAttributesRegexes()).thenReturn(
                new String[]{".*%deniedAttr", "wknd/components/page%bar/subDeniedAttr"});
        service.activate(config);
        List<String> translateable = service.translateableAttributes(res);
        Collections.sort(translateable);
        assertEquals(Arrays.asList("allowedAttr", "heuristicAttr"), translateable);
        translateable = service.translateableAttributes(sub);
        Collections.sort(translateable);
        assertEquals(Arrays.asList("subAllowedAttr", "subHeuristicAttr"), translateable);
    }


    @Test
    public void testIsTranslatableProperty() {
        // Test with certainly translatable properties
        for (String propertyName : AutoTranslateConfigServiceImpl.CERTAINLY_TRANSLATABLE_PROPERTIES) {
            assertTrue(isHeuristicallyTranslatableProperty(propertyName, "Some value"));
        }

        // Test with property name containing colon
        assertFalse(isHeuristicallyTranslatableProperty("property:name", "Some value"));

        // Test with non-string value
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", new Object()));

        // Test with string value starting with /content/, /apps/, /libs/, /mnt/
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/content/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/apps/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/libs/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/mnt/someValue"));

        // Test with property name starting with AI_PREFIX or LC_PREFIX and ending with AI_TRANSLATED_SUFFIX or AI_ORIGINAL_SUFFIX
        assertFalse(isHeuristicallyTranslatableProperty(AI_PREFIX + "propertyName" + AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(AI_PREFIX + "propertyName" + AI_ORIGINAL_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(LC_PREFIX + "propertyName" + AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(LC_PREFIX + "propertyName" + AI_ORIGINAL_SUFFIX, "Some value"));

        // Test with string value without whitespace and 4 letter sequence
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "abc"));

        // Test with string value with whitespace but without 4 letter sequence
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "a b c"));

        // Test with string value with 4 letter sequence but without whitespace
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "abcd"));

        // Test with string value with whitespace and 4 letter sequence
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efgh"));


        // Test with string value with multiple whitespace sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // Test with string value with multiple whitespace sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // that's likely a boolean property
        assertFalse(isHeuristicallyTranslatableProperty("displayPopupTitle", "true"));
        assertFalse(isHeuristicallyTranslatableProperty("displayPopupTitle", "false"));

        // dates are not translateable
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "2022-01-01"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "2022-01-01T00:00:00.000Z"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", new Date().toString()));

        // rather do not translate anything that looks like a boolean, even if it's a whitelisted property
        assertFalse(isHeuristicallyTranslatableProperty("shortDescription", "true"));
        assertFalse(isHeuristicallyTranslatableProperty("jcr:title", "false"));
    }

}
