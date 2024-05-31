package com.composum.ai.backend.base.service.chat.impl;


import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;

@Component(service = GPTEmbeddingService.class)
public class GPTEmbeddingServiceImpl implements GPTEmbeddingService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTEmbeddingServiceImpl.class);

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    /**
     * Generates a map of the stored embeddings for texts.
     */
    @Nonnull
    Map<String, float[]> embeddingsMap(@Nullable List<String> texts , @Nullable EmbeddingsCache cache) {
        Map<String, float[]> result = new java.util.HashMap<>();
        if (cache != null && texts != null) {
            texts.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(text -> {
                        String cachedEmbedding = cache.getCachedEmbedding(text);
                        float[] embedding = decodeFloatArray(cachedEmbedding);
                        if (embedding != null) {
                            result.put(text, embedding);
                        }
                    });
        }
        return result;
    }


    @Override
    public List<float[]> getEmbeddings(List<String> texts, @Nullable GPTConfiguration configuration, @Nullable EmbeddingsCache cache) throws GPTException {
        LOG.debug("Getting embeddings for {} texts", texts.size());
        if (cache != null) {
            Map<String, float[]> cached = embeddingsMap(texts, cache);

            List<String> toFetch = texts.stream()
                    .filter(Objects::nonNull)
                    .filter(text -> !cached.containsKey(text))
                    .distinct()
                    .collect(Collectors.toList());
            List<float[]> toFetchEmbeddings = chatCompletionService.getEmbeddings(toFetch, configuration);
            if (toFetchEmbeddings == null || toFetch.size() != toFetchEmbeddings.size()) {
                throw new GPTException("BUG: Expected " + toFetch.size() + " embeddings, got " + toFetchEmbeddings.size());
            }

            for (int i = 0; i < toFetch.size(); i++) {
                cache.putCachedEmbedding(toFetch.get(i), encodeFloatArray(toFetchEmbeddings.get(i)));
                cached.put(toFetch.get(i), toFetchEmbeddings.get(i));
            }

            return texts.stream()
                    .map(cached::get)
                    .collect(Collectors.toList());
        } else { // uncached
            return chatCompletionService.getEmbeddings(texts, configuration);
        }
    }

    @Override
    public List<String> findMostRelated(String query, List<String> comparedStrings, int limit, @Nullable GPTConfiguration configuration, @Nullable EmbeddingsCache thecache) throws GPTException {
        List<float[]> embeddings = getEmbeddings(comparedStrings, configuration, thecache);
        float[] queryEmbedding = getEmbeddings(Collections.singletonList(query), configuration, thecache).get(0);
        Map<String, Double> withSimilarity = comparedStrings.stream()
                .collect(Collectors.toMap(s -> s,
                        s -> cosineSimilarity(queryEmbedding, embeddings.get(comparedStrings.indexOf(s)))));
        List<Map.Entry<String, Double>> entries = withSimilarity.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());
        if (LOG.isDebugEnabled()) {
            LOG.debug("cosine similarities: " + entries.stream().map(e -> e.getValue().floatValue()).collect(Collectors.toList()));
        }
        return entries.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    protected double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    protected static String encodeFloatArray(float[] floatArray) {
        if (floatArray == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(floatArray.length * 4); // Each float is 4 bytes
        for (float value : floatArray) {
            byteBuffer.putFloat(value);
        }
        byte[] byteArray = byteBuffer.array();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    protected static float[] decodeFloatArray(String encodedString) {
        if (encodedString == null || encodedString.isEmpty()) {
            return null;
        }
        try {
            byte[] byteArray = Base64.getDecoder().decode(encodedString);
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);

            float[] floatArray = new float[byteArray.length / 4]; // Each float is 4 bytes
            for (int i = 0; i < floatArray.length; i++) {
                floatArray[i] = byteBuffer.getFloat();
            }
            return floatArray;
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not decode float array from {}", encodedString.substring(0, Math.min(encodedString.length(), 80)), e);
            return null;
        }
    }

}
