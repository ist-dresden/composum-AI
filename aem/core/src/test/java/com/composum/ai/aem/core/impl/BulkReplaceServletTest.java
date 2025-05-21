package com.composum.ai.aem.core.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkReplaceServletTest {

    @Spy
    BulkReplaceServlet servlet = new BulkReplaceServlet();

    protected String abbreviateSurroundings(String text, String search) {
        return servlet.abbreviateSurroundings(text, Pattern.compile(Pattern.quote(search)), 8);
    }

    @Test
    public void testAbbreviateSurroundings() {
        assertAll(
                () -> assertEquals("", abbreviateSurroundings("", "notcontained")),
                () -> assertEquals("", abbreviateSurroundings("foo", "notcontained")),
                // Single occurrence at the start
                () -> assertEquals("foobar", abbreviateSurroundings("foobar", "foo")),
                () -> assertEquals("foobaran...", abbreviateSurroundings("foobarandlong", "foo")),
                // Single occurrence at the end
                () -> assertEquals("barfoo", abbreviateSurroundings("barfoo", "foo")),
                () -> assertEquals("...ndbarfoo", abbreviateSurroundings("longandbarfoo", "foo")),
                // Single occurrence in the middle
                () -> {
                    String text = "The quick brown fox jumps over the lazy dog";
                    assertEquals("...rown fox jump...", abbreviateSurroundings(text, "fox"));
                },
                // Multiple occurrences
                () -> assertEquals("foo bar foo", abbreviateSurroundings("foo bar foo", "foo")),
                () -> assertEquals("foo bar ...\n...baz foo",
                        abbreviateSurroundings("foo bar long intermediate baz foo", "foo"))
        );
    }

    @Test
    public void testCreateExcerpt() {
        assertAll(
                () -> assertEquals("... you <span class=\"foundsearchstringmarker\">foo</span>!",
                        servlet.createExcerpt("<p>hallo <i>you</i> foo!", Pattern.compile("foo"), 8)),
                () -> assertEquals("... the <span class=\"foundsearchstringmarker\">foo</span> that...",
                        servlet.createExcerpt("this is the foo that is found", Pattern.compile("foo"), 8))
        );
    }

    @Test
    public void testWhitespaceLenientPattern() {
        Pattern pp = servlet.whitespaceLenientPattern(" a");
        assertAll(
                () -> assertTrue(servlet.whitespaceLenientPattern("a b c").matcher("a b c").matches()),
                () -> assertTrue(servlet.whitespaceLenientPattern("a b c").matcher("a   b    c").matches()),
                () -> assertTrue(servlet.whitespaceLenientPattern("a b c").matcher("a\tb\nc").matches()),
                () -> assertFalse(servlet.whitespaceLenientPattern("a b c").matcher("  a b c  ").matches()),
                () -> assertFalse(servlet.whitespaceLenientPattern("a b c").matcher("a b").matches()),
                () -> assertFalse(servlet.whitespaceLenientPattern("a b c").matcher("b a c").matches()),
                () -> assertTrue(servlet.whitespaceLenientPattern("abc").matcher("abc").matches()),
                () -> assertTrue(servlet.whitespaceLenientPattern("a.b c?").matcher("a.b   c?").matches()),

                () -> assertTrue(servlet.whitespaceLenientPattern("a").matcher("ba").find()),
                () -> assertFalse(pp.matcher("ba").find()),
                () -> assertTrue(pp.matcher("b a").find()),

                () -> assertFalse(servlet.whitespaceLenientPattern("a ").matcher("ab").find()),
                () -> assertTrue(servlet.whitespaceLenientPattern("a ").matcher("a").find()),
                () -> assertTrue(servlet.whitespaceLenientPattern("a ").matcher("a ").find()),

                () -> assertEquals("", servlet.whitespaceLenientPattern("").pattern())
        );
    }

    @Test
    public void testToPlaintext() {
        assertAll(
                () -> assertEquals("", servlet.toPlaintext("")),
                () -> assertEquals("foo", servlet.toPlaintext("foo")),
                () -> assertEquals("foo", servlet.toPlaintext("<p>foo</p>")),
                () -> assertEquals("foo", servlet.toPlaintext("<div>foo</div>")),
                () -> assertEquals("foo", servlet.toPlaintext("<span>foo</span>")),
                () -> assertEquals("foo", servlet.toPlaintext("<a href=\"http://example.com\">foo</a>")),
                () -> assertEquals("", servlet.toPlaintext("<img src=\"http://example.com/foo.png\" alt=\"foo\"/>")),
                () -> assertEquals("<barf", servlet.toPlaintext("<barf")),
                () -> assertEquals("barf>", servlet.toPlaintext("<broken>ba</rr>rf>"))
        );
    }

}
