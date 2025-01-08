package com.composum.ai.aem.core.impl.autotranslate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;

class AutoTranslatePropertyTest {

    private ModifiableValueMapDecorator sourceMap;
    private ModifiableValueMapDecorator targetMap;
    private AITranslatePropertyWrapper wrapper;
    private AutoTranslateMergeService.AutoTranslateProperty property;

    @BeforeEach
    void setUp() {
        sourceMap = new ModifiableValueMapDecorator(new HashMap<>());
        targetMap = new ModifiableValueMapDecorator(new HashMap<>());
        wrapper = new AITranslatePropertyWrapper(sourceMap, targetMap, "exampleProperty");
        property = new AutoTranslateMergeService.AutoTranslateProperty("/content/example", wrapper, "component");
    }

    @Test
    void testGetOriginalCopyDiffsHTML() {
        wrapper.setOriginalCopy("Original text");
        wrapper.setNewOriginalCopy("New original text with additions");

        String diffsHtml = property.getOriginalCopyDiffsHTML();

        assertEquals("<del>O</del><span class=\"ins\">New o</span>riginal text<span class=\"ins\"> with additions</span>", diffsHtml);
    }

    @Test
    void testHTMLDiffs() {
        wrapper.setOriginalCopy("<b>o</b>riginal");
        wrapper.setNewOriginalCopy("The <b>o</b>rriginal");

        String diffsHtml = property.getOriginalCopyDiffsHTML();

        assertEquals("<span class=\"ins\">The </span><b>o</b><span class=\"ins\">r</span>riginal", diffsHtml);
    }
}
