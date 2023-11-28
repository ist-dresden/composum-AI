package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;

/**
 * Tests for some methods of {@link GPTChatCompletionService}.
 */
public class GPTChatCompletionServiceImplTest {

    protected GPTChatCompletionServiceImpl service = new GPTChatCompletionServiceImpl();

    @Test
    public void testShortenShortText() {
        String text = "This is a short text.";
        String shortenedText = service.shorten(text, 10);
        assertEquals(text, shortenedText);
    }

    @Test
    public void testShortenLongText() {
        String text = "This is a very long text that should be shortened. Well, not really very long, but long enough.";
        String shortenedText = service.shorten(text, 20);
        assertEquals(20, service.countTokens(shortenedText));
        assertEquals("This is a very long text ... (truncated) ...  very long, but long enough.", shortenedText);
    }

    @Test
    public void testShortenEmptyText() {
        String text = "";
        String shortenedText = service.shorten(text, 10);
        assertEquals(text, shortenedText);
    }

    @Test
    public void testRecalculateDelayWhenBodyContainsTryAgainIn() {
        String body = "The request could not be completed. Please try again in 27s. bla bla bla";
        long delay = 1000L;
        long actualDelay = service.recalculateDelay(body, delay);
        assertEquals(27000L, actualDelay);
    }

    @Test
    public void testRecalculateDelayWhenBodyDoesNotContainTryAgainIn() {
        String body = "The response is successful.";
        long delay = 1000L;
        long actualDelay = service.recalculateDelay(body, delay);
        assertEquals(2000L, actualDelay);
    }

    @Test
    public void testMarkdownToHTML() {
        String markdown = "This is a **bold** text.";
        String html = service.markdownToHtml(markdown);
        assertEquals("<p>This is a \n" +
                "\t<strong>bold</strong> text.\n" +
                "</p>", html);
    }

    @Ignore
    @Test
    public void testHtmlToMarkdown() {
        String html = "<p>This is a <strong>test</strong>.</p>"
                + "<p>Another paragraph with a <a href=\"http://example.com\">link</a>.</p>"
                + "<p>A paragraph with <b>bold is this</b>.</p>"
                + "<p>A paragraph with <u>underlined text</u>.</p>"
                + "<p>A paragraph with <code>code fragment</code>.</p>"
                + "<ul><li>Item 1</li><li>Item 2</li></ul>"
                + "<ol><li>Item 1</li><li>Item 2</li></ol>"
                + "<pre>\na code block with several lines\n</pre>";
        String expected = "\nThis is a **test**.\n" +
                "\n" +
                "Another paragraph with a [link](http://example.com).\n" +
                "\n" +
                "A paragraph with **bold is this**.\n" +
                "\n" +
                "A paragraph with _underlined text_.\n" +
                "\n" +
                "A paragraph with `code fragment`.\n" +
                "\n" +
                "- Item 1\n" +
                "- Item 2\n" +
                "\n" +
                "1. Item 1\n" +
                "2. Item 2\n" +
                "\n" +
                "```\n" +
                "a code block with several lines\n" +
                "```\n";
        String result = service.htmlToMarkdown(html);
        assertEquals(expected, result);
    }

    @Test
    public void testHtmlToMarkdown_empty() {
        String html = "";
        String expected = "";
        String result = service.htmlToMarkdown(html);
        assertEquals(expected, result);
    }

    @Test
    public void testHtmlToMarkdown_null() {
        String html = null;
        String expected = "";
        String result = service.htmlToMarkdown(html);
        assertEquals(expected, result);
    }

}
