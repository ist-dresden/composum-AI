package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nullable;

import com.composum.ai.backend.base.service.GPTException;

/**
 * Methods related to embeddings. Based on the low level service {@link GPTChatCompletionService#getEmbeddings(List, GPTConfiguration)}
 * but with a more convenient API.
 */
public interface GPTEmbeddingService {

    /**
     * Get embeddings for the given texts.
     *
     * @param texts         the texts to get embeddings for
     * @param configuration the configuration to use
     * @return the embeddings for the given texts
     * @throws GPTException if an error occurs
     */
    List<float[]> getEmbeddings(List<String> texts, @Nullable GPTConfiguration configuration, @Nullable EmbeddingsCache cache) throws GPTException;

    /**
     * Determines the at most limit to query semantically closest of the comparedStrings according to the embedding service.
     */
    List<String> findMostRelated(String query, List<String> comparedStrings, int limit, @Nullable GPTConfiguration configuration, @Nullable EmbeddingsCache cache) throws GPTException;

    /**
     * Optional cache for embedding values. Tbe embeddings are encoded as a String.
     */
    static interface EmbeddingsCache {

        String getCachedEmbedding(String text);

        void putCachedEmbedding(String text, String embedding);

    }

}
