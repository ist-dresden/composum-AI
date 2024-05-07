package com.composum.ai.backend.base.service.chat.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An optional cache used for caching embeddings. This is dependent on the application how it's done and whether it's done at all.
 */
public interface GPTEmbeddingCache {

    /**
     * Returns a cached embedding for the text with the given model.
     */
    @Nullable
    float[] get(@Nullable String text);

    /**
     * Caches the given embedding for the text with the given model.
     */
    void put(@Nullable String text, @Nonnull float[] embedding);

    /**
     * Clears the cache if the cached model is not the given model.
     */
    void clearIfNotModel(String model);

    /**
     * Generates a map of the stored embeddings for texts.
     */
    @Nonnull
    default Map<String, float[]> embeddingsMap(@Nullable List<String> texts) {
        Map<String, float[]> result = new java.util.HashMap<>();
        if (texts != null) {
            texts.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(text -> {
                        float[] embedding = get(text);
                        if (embedding != null) {
                            result.put(text, embedding);
                        }
                    });
        }
        return result;
    }

}
