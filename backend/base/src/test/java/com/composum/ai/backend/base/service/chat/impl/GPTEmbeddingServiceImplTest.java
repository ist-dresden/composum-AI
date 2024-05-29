package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;

public class GPTEmbeddingServiceImplTest {

    private GPTEmbeddingServiceImpl service;
    private GPTChatCompletionService chatCompletionService;

    private Map<String, String> embeddingsCacheMap = new java.util.HashMap<>();
    private GPTEmbeddingService.EmbeddingsCache cache = new GPTEmbeddingService.EmbeddingsCache() {
        @Override
        public String getCachedEmbedding(String text) {
            return embeddingsCacheMap.get(text);
        }

        @Override
        public void putCachedEmbedding(String text, String embedding) {
            embeddingsCacheMap.put(text, embedding);
        }
    };

    @Before
    public void setUp() {
        chatCompletionService = mock(GPTChatCompletionService.class);
        service = new GPTEmbeddingServiceImpl();
        service.chatCompletionService = chatCompletionService;
    }

    @Test
    public void getEmbeddingsReturnsCorrectEmbeddings() {
        List<String> texts = Arrays.asList("text1", "text2", "text3", "text4");
        float[] embedding1 = new float[]{1.0f, 2.0f};
        float[] embedding2 = new float[]{3.0f, 4.0f};
        embeddingsCacheMap.put("text1", GPTEmbeddingServiceImpl.encodeFloatArray(embedding1));
        embeddingsCacheMap.put("text2", GPTEmbeddingServiceImpl.encodeFloatArray(embedding2));

        float[] embedding3 = {1.0f, 2.1f};
        float[] embedding4 = {1.1f, 1.9f};
        when(chatCompletionService.getEmbeddings(Arrays.asList("text3", "text4"), null)).thenReturn(Arrays.asList(embedding3, embedding4));

        List<float[]> embeddings = service.getEmbeddings(texts, null, cache);

        assertNotNull(embeddings);
        assertEquals(4, embeddings.size());
        assertArrayEquals(embedding1, embeddings.get(0), 0.0001f);
        assertArrayEquals(embedding2, embeddings.get(1), 0.0001f);
        assertEquals(embeddingsCacheMap.get("text3"), GPTEmbeddingServiceImpl.encodeFloatArray(embedding3));
        assertEquals(embeddingsCacheMap.get("text4"), GPTEmbeddingServiceImpl.encodeFloatArray(embedding4));
    }

    @Test
    public void findMostRelatedReturnsCorrectStrings() {
        List<String> comparedStrings = Arrays.asList("string1", "string2", "string3");
        float[] embedding1 = new float[]{1.0f, 2.0f};
        float[] embedding2 = new float[]{3.0f, 4.0f};
        float[] embedding3 = new float[]{5.0f, 6.0f};
        float[] queryEmbedding = new float[]{1.0f, 2.1f};
        embeddingsCacheMap.put("string1", GPTEmbeddingServiceImpl.encodeFloatArray(embedding1));
        embeddingsCacheMap.put("string2", GPTEmbeddingServiceImpl.encodeFloatArray(embedding2));
        embeddingsCacheMap.put("string3", GPTEmbeddingServiceImpl.encodeFloatArray(embedding3));
        embeddingsCacheMap.put("query", GPTEmbeddingServiceImpl.encodeFloatArray(queryEmbedding));


        List<String> relatedStrings = service.findMostRelated("query", comparedStrings, 2, null, cache);

        assertNotNull(relatedStrings);
        assertEquals(2, relatedStrings.size());
        assertEquals("string1", relatedStrings.get(0));
        assertEquals("string2", relatedStrings.get(1));
    }
}
