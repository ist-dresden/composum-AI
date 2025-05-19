package com.composum.ai.aem.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkReplaceServletTest {

    @Spy
    BulkReplaceServlet servlet = new BulkReplaceServlet();

    @Test
    public void testCreateExcerpt() {
        assertAll(
            () -> assertEquals("", servlet.createExcerpt("", "notcontained")),
            () -> assertEquals("", servlet.createExcerpt("foo", "notcontained")),
            // Single occurrence at the start
            () -> assertEquals("bar", servlet.createExcerpt("foobar", "foo")),
            // Single occurrence at the end
            () -> assertEquals("foo", servlet.createExcerpt("barfoo", "foo")),
            // Single occurrence in the middle
            () -> {
                String text = "The quick brown fox jumps over the lazy dog";
                assertEquals("quick brown fox jumps over the lazy", servlet.createExcerpt(text, "fox"));
            },
            // Multiple occurrences - should excerpt around the first
            () -> {
                String multi = "foo bar foo baz foo";
                assertEquals("foo bar foo baz", servlet.createExcerpt(multi, "foo"));
            }
        );
    }

}
