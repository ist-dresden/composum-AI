package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AITranslatePropertyWrapperTest {

    private ModifiableValueMapDecorator sourceMap;
    private ModifiableValueMapDecorator targetMap;
    private AITranslatePropertyWrapper wrapper;

    @BeforeEach
    void setUp() {
        // Initialize mock data for source and target maps
        sourceMap = new ModifiableValueMapDecorator(new HashMap<>());
        targetMap = new ModifiableValueMapDecorator(new HashMap<>());

        // Mock data
        sourceMap.put("exampleProperty", "Original text");
        targetMap.put("exampleProperty.ai_original", "Original copy");
        targetMap.put("exampleProperty.ai_newOriginal", "New original text");
        targetMap.put("exampleProperty.ai_newTranslated", "New translated text");
        targetMap.put("exampleProperty.textIsRich", true);

        // Initialize the wrapper
        wrapper = new AITranslatePropertyWrapper(sourceMap, targetMap, "exampleProperty");
    }

    @Test
    void testGetOriginal() {
        assertEquals("Original text", wrapper.getOriginal());
    }

    @Test
    void testGetAndSetOriginalCopy() {
        wrapper.setOriginalCopy("Updated original copy");
        assertEquals("Updated original copy", wrapper.getOriginalCopy());
    }

    @Test
    void testGetAndSetNewOriginalCopy() {
        wrapper.setNewOriginalCopy("Updated new original copy");
        assertEquals("Updated new original copy", wrapper.getNewOriginalCopy());
    }

    @Test
    void testGetAndSetNewTranslatedCopy() {
        wrapper.setNewTranslatedCopy("Updated new translated copy");
        assertEquals("Updated new translated copy", wrapper.getNewTranslatedCopy());
    }

    @Test
    void testGetAndSetCurrentValue() {
        wrapper.setCurrentValue("Updated current value");
        assertEquals("Updated current value", wrapper.getCurrentValue());
    }

    @Test
    void testEncodePropertyName() {
        String encoded = AITranslatePropertyWrapper.encodePropertyName("ai_", "exampleProperty", "_suffix");
        assertEquals("ai_exampleProperty_suffix", encoded);
    }

    @Test
    void testDecodePropertyName() {
        String encoded = "ai_exampleProperty_suffix";
        Resource resource = new MockResource("/content/example", Collections.singletonMap("exampleProperty", "Original text"), null);
        String decoded = AITranslatePropertyWrapper.decodePropertyName("ai_", encoded, "_suffix", resource);
        assertEquals("exampleProperty", decoded);
    }

    @Test
    void testHasSavedTranslation() {
        wrapper.setOriginalCopy("New original text");
        wrapper.setTranslatedCopy("New translated text");
        assertTrue(wrapper.hasSavedTranslation());
    }

    @Test
    void testIsRichText() {
        targetMap.put("exampleProperty", "<p>something</p>");
        assertTrue(wrapper.isRichText());

        targetMap.put("exampleProperty", "Plain text");
        assertFalse(wrapper.isRichText());
    }
}
