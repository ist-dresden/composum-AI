package com.composum.ai.aem.core.impl.autotranslate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AutoTranslatePropertyTest {

    @Test
    void testGetPath() {
        // Given
        String path = "/content/example";
        AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(null, null, "exampleProperty");
        AutoTranslateMergeService.AutoTranslateProperty property = 
            new AutoTranslateMergeService.AutoTranslateProperty(path, wrapper);

        // When
        String actualPath = property.getPath();

        // Then
        assertEquals(path, actualPath);
    }
}