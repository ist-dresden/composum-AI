package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;

public class GPTEmbeddingServiceImplTest {

    private GPTEmbeddingServiceImpl service;
    private GPTChatCompletionService chatCompletionService;
    private GPTEmbeddingCache cache;

    @Before
    public void setUp() {
        chatCompletionService = mock(GPTChatCompletionService.class);
        cache = mock(GPTEmbeddingCache.class);
        service = new GPTEmbeddingServiceImpl();
        service.chatCompletionService = chatCompletionService;
        service.cache = cache;
        when(cache.embeddingsMap(any())).thenCallRealMethod();
    }

    @Test
    public void getEmbeddingsReturnsCorrectEmbeddings() {
        List<String> texts = Arrays.asList("text1", "text2", "text3", "text4");
        float[] embedding1 = new float[]{1.0f, 2.0f};
        float[] embedding2 = new float[]{3.0f, 4.0f};
        when(cache.get("text1")).thenReturn(embedding1);
        when(cache.get("text2")).thenReturn(embedding2);

        float[] embedding3 = {1.0f, 2.1f};
        float[] embedding4 = {1.1f, 1.9f};
        when(chatCompletionService.getEmbeddings(Arrays.asList("text3", "text4"), null)).thenReturn(Arrays.asList(embedding3, embedding4));

        List<float[]> embeddings = service.getEmbeddings(texts, null);

        assertNotNull(embeddings);
        assertEquals(4, embeddings.size());
        assertEquals(embedding1, embeddings.get(0));
        assertEquals(embedding2, embeddings.get(1));
        verify(cache).put("text3", embedding3);
        verify(cache).put("text4", embedding4);
    }

    @Test
    public void findMostRelatedReturnsCorrectStrings() {
        List<String> comparedStrings = Arrays.asList("string1", "string2", "string3");
        float[] embedding1 = new float[]{1.0f, 2.0f};
        float[] embedding2 = new float[]{3.0f, 4.0f};
        float[] embedding3 = new float[]{5.0f, 6.0f};
        float[] queryEmbedding = new float[]{1.0f, 2.1f};
        when(cache.get("string1")).thenReturn(embedding1);
        when(cache.get("string2")).thenReturn(embedding2);
        when(cache.get("string3")).thenReturn(embedding3);
        when(cache.get("query")).thenReturn(queryEmbedding);

        List<String> relatedStrings = service.findMostRelated("query", comparedStrings, 2, null);

        assertNotNull(relatedStrings);
        assertEquals(2, relatedStrings.size());
        assertEquals("string1", relatedStrings.get(0));
        assertEquals("string2", relatedStrings.get(1));
    }
}
