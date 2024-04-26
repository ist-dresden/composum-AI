package com.composum.ai.backend.base.service.chat;

import java.util.List;

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
    List<float[]> getEmbeddings(List<String> texts, GPTConfiguration configuration) throws GPTException;

}
