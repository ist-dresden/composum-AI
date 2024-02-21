package com.composum.ai.aem.core.impl;

import static com.composum.ai.aem.core.impl.SelectorUtils.getLanguageName;
import static com.composum.ai.aem.core.impl.SelectorUtils.getLanguageSiblings;
import static com.composum.ai.aem.core.impl.SelectorUtils.isLocaleName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.Test;

import io.wcm.testing.mock.aem.junit5.AemContext;
import junit.framework.TestCase;

public class SelectorUtilsTest extends TestCase {

    @Test
    public void testGetLanguageName() {
        assertEquals("English", getLanguageName("en"));
        assertEquals("Deutsch", getLanguageName("de"));
        assertEquals("Deutsch", getLanguageName("de_DE"));
        assertEquals("English", getLanguageName("en_CA"));
        assertEquals("français", getLanguageName("fr_CA"));
    }

    @Test
    public void testIsLocaleName() {
        // Test with valid locale names
        assertTrue(isLocaleName("en"));
        assertTrue(isLocaleName("en_US"));
        assertTrue(isLocaleName("fr_CA"));
        assertTrue(isLocaleName("zh_CN"));

        // Test with invalid locale names
        assertFalse(isLocaleName("english"));
        assertFalse(isLocaleName("US"));
        assertFalse(isLocaleName("123"));
        assertFalse(isLocaleName("en_123"));
        assertFalse(isLocaleName(""));

        // Test with null
        assertFalse(isLocaleName(null));
    }

    @Test
    public void testGetLanguageSiblings() {
        AemContext context = new AemContext();
        context.create().resource("/content/ai/aem/core/en/foo/bar");
        context.create().resource("/content/ai/aem/core/de/foo/bar");
        context.create().resource("/content/ai/aem/core/es_ES/foo/bar");

        List<Resource> result = getLanguageSiblings(context.resourceResolver().getResource("/content/ai/aem/core/en/foo/bar"), "de");
        assertEquals(result.toString(), 1, result.size());
        assertEquals("/content/ai/aem/core/de/foo/bar", result.get(0).getPath());

        result = getLanguageSiblings(context.resourceResolver().getResource("/content/ai/aem/core/en/foo/bar"), "es");
        assertEquals(0, result.size());

        result = getLanguageSiblings(context.resourceResolver().getResource("/content/ai/aem/core/es_ES/foo/bar"), "de-DE");
        assertEquals(1, result.size());
        assertEquals("/content/ai/aem/core/de/foo/bar", result.get(0).getPath());

        result = getLanguageSiblings(context.resourceResolver().getResource("/content/ai/aem/core/es_ES/foo/bar"), "de_de");
        assertEquals(1, result.size());
        assertEquals("/content/ai/aem/core/de/foo/bar", result.get(0).getPath());
    }

}
