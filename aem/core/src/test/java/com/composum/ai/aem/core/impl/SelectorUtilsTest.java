package com.composum.ai.aem.core.impl;

import static com.composum.ai.aem.core.impl.SelectorUtils.getLanguageName;
import static com.composum.ai.aem.core.impl.SelectorUtils.getLanguageSiblings;
import static com.composum.ai.aem.core.impl.SelectorUtils.isLocaleName;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.jupiter.api.Test;

import com.adobe.granite.ui.components.ds.DataSource;
import io.wcm.testing.mock.aem.junit5.AemContext;
import junit.framework.TestCase;

public class SelectorUtilsTest extends TestCase {

    @Test
    public void testGetLanguageName() {
        assertEquals("English", getLanguageName("en", null));
        assertEquals("English", getLanguageName("en", Locale.ENGLISH));

        assertEquals("Deutsch", getLanguageName("de", null));
        assertEquals("German", getLanguageName("de", Locale.ENGLISH));

        assertEquals("Deutsch (Deutschland)", getLanguageName("de_DE", null));
        assertEquals("German (Germany)", getLanguageName("de_DE", Locale.ENGLISH));

        assertEquals("English (Canada)", getLanguageName("en_CA", null));
        assertEquals("English (Canada)", getLanguageName("en_CA", Locale.ENGLISH));

        assertEquals("français (Canada)", getLanguageName("fr_CA", null));
        assertEquals("French (Canada)", getLanguageName("fr_CA", Locale.ENGLISH));
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

    @Test
    public void testFindLanguageMockito() {
        AemContext context = new AemContext();
        // Create parent resource with jcr:language property set to "es-ar"
        Map<String, Object> parentProps = new LinkedHashMap<>();
        parentProps.put(org.apache.jackrabbit.JcrConstants.JCR_LANGUAGE, "es-ar");
        Resource parentResource = context.create().resource("/content/foo/ar/es-ar", parentProps);
        // Create child resource under the parent path
        Resource pageResource = context.create().resource("/content/foo/ar/es-ar/bar");

        String language = SelectorUtils.findLanguage(pageResource);
        assertEquals("es-ar", language);
    }

    @Test
    public void testTransformToDatasource() {
        AemContext context = new AemContext();
        SlingHttpServletRequest request = context.request();
        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("en", "English");
        prompts.put("de", "German");

        DataSource ds = SelectorUtils.transformToDatasource(request, prompts);
        List<Resource> resources = new ArrayList<>();
        ds.iterator().forEachRemaining(resources::add);

        // Assert two resources are created preserving insertion order
        assertEquals(2, resources.size());
        assertEquals("en", resources.get(0).getValueMap().get("value", String.class));
        assertEquals("English", resources.get(0).getValueMap().get("text", String.class));
        assertEquals("de", resources.get(1).getValueMap().get("value", String.class));
        assertEquals("German", resources.get(1).getValueMap().get("text", String.class));
    }

    @Test
    public void testReplaceLanguagePlaceholder() {
        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("Translate to TARGETLANGUAGE", "translate");
        Map<String, String> result = SelectorUtils.replaceLanguagePlaceholder(prompts, "en");
        // Assert that "TARGETLANGUAGE" is replaced by the language name "English"
        assertEquals("translate", result.get("Translate to English"));
    }
}
