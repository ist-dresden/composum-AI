package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AutoTranslateMergeService.AutoTranslateProperty.wrapExcludingHTMLTags;
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
        property = new AutoTranslateMergeService.AutoTranslateProperty("/content/example", wrapper, "component", "component text");
    }

    @Test
    void testGetOriginalCopyDiffsHTML() {
        wrapper.setOriginalCopy("Original text");
        wrapper.setNewOriginalCopy("New original text with additions");

        assertEquals("<del>O</del><span class=\"ins\">New o</span>riginal text<span class=\"ins\"> with additions</span>", property.getOriginalCopyDiffsHTML());
        assertEquals("<del>O</del>riginal text", property.getOriginalCopyInsertionsMarked());
        assertEquals("<span class=\"ins\">New o</span>riginal text<span class=\"ins\"> with additions</span>", property.getNewOriginalCopyInsertionsMarked());
    }

    @Test
    void testHTMLDiffs() {
        wrapper.setOriginalCopy("<b>o</b>riginal");
        wrapper.setNewOriginalCopy("The <b>o</b>rriginal");

        String diffsHtml = property.getOriginalCopyDiffsHTML();

        assertEquals("<span class=\"ins\">The </span><b>o</b><span class=\"ins\">r</span>riginal", diffsHtml);
    }


    @Test
    void testWrapExcludingHTMLTags() {
        // Test case 1: No HTML tags
        assertEquals("<span class=\"ins\">This is a test.</span>", wrapExcludingHTMLTags("This is a test.", "<span class=\"ins\">", "</span>"));

        // Test case 2: With HTML tags
        assertEquals("<span class=\"ins\">This is</span> <b><span class=\"ins\">a</span></b> <span class=\"ins\">test.</span>", wrapExcludingHTMLTags("This is <b>a</b> test.", "<span class=\"ins\">", "</span>"));

        // Test case 3: Nested HTML tags
        assertEquals("<span class=\"ins\">This is</span> <b><i><span class=\"ins\">a</span></i></b> <span class=\"ins\">test.</span>", wrapExcludingHTMLTags("This is <b><i>a</i></b> test.", "<span class=\"ins\">", "</span>"));

        // Test case 4: Multiple HTML tags
        assertEquals("<p><del>This is</del> <b><del>a</del></b> <i><del>test</del></i><del>.</del></p>", wrapExcludingHTMLTags("<p>This is <b>a</b> <i>test</i>.</p>", "<del>", "</del>"));

        // Test case 5: Empty string
        assertEquals("", wrapExcludingHTMLTags("", "<span class=\"ins\">", "</span>"));

        // Test case 6: to not mark whitespace.
        assertEquals("<del>a</del> <b> <i> <del>XXX</del> </i> </b> <del>c </del>", wrapExcludingHTMLTags("a <b> <i> XXX </i> </b> c ", "<del>", "</del>"));
    }

}
