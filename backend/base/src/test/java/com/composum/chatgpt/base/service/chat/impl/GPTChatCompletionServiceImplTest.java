package com.composum.chatgpt.base.service.chat.impl;

import static org.junit.Assert.*;

import java.net.http.HttpResponse;

import org.junit.Test;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;

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
        String shortenedText = service.shorten(text, 4);
        assertEquals("This is ... enough.", shortenedText);
    }

    @Test
    public void testShortenTextWithOddMaxWords() {
        String text = "This is a text with odd max words which we shorten.";
        String shortenedText = service.shorten(text, 3);
        assertEquals("This ... shorten.", shortenedText);
    }

    @Test
    public void testShortenTextWithEvenMaxWords() {
        String text = "This is a text with even max words which we shorten.";
        String shortenedText = service.shorten(text, 4);
        assertEquals("This is ... shorten.", shortenedText);
    }

    @Test
    public void testShortenEmptyText() {
        String text = "";
        String shortenedText = service.shorten(text, 10);
        assertEquals(text, shortenedText);
    }

    @Test
    public void testShortenTextWithOddNumberOfWords() {
        String text = "This is a text with odd wordcount.";
        String shortenedText = service.shorten(text, 4);
        assertEquals("This is ... wordcount.", shortenedText);
    }

    @Test
    public void testShortenTextWithEvenNumberOfWords() {
        String text = "This is a text with even number of words.";
        String shortenedText = service.shorten(text, 4);
        assertEquals("This is ... words.", shortenedText);
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

}
