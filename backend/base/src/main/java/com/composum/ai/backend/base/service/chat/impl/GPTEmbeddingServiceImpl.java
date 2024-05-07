package com.composum.ai.backend.base.service.chat.impl;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected GPTEmbeddingCache cache;

    @Override
    public List<float[]> getEmbeddings(List<String> texts, GPTConfiguration configuration) throws GPTException {
        LOG.debug("Getting embeddings for {} texts", texts.size());
        if (cache != null) {
            Map<String, float[]> cached = cache.embeddingsMap(texts);

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
                cache.put(toFetch.get(i), toFetchEmbeddings.get(i));
                cached.put(toFetch.get(i), toFetchEmbeddings.get(i));
            }

            return texts.stream()
                    .map(cached::get)
                    .collect(Collectors.toList());
        }
        return chatCompletionService.getEmbeddings(texts, configuration);
    }

    @Override
    public List<String> findMostRelated(String query, List<String> comparedStrings, int limit, GPTConfiguration configuration) {
        List<float[]> embeddings = getEmbeddings(comparedStrings, configuration);
        float[] queryEmbedding = getEmbeddings(Collections.singletonList(query), configuration).get(0);
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

}
