package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Assert;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;

/**
 * Tries an actual call to OpenAI Embeddings Service. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunOpenAIEmbeddings extends AbstractGPTRunner {

    @Spy
    private GPTEmbeddingCache cache = new GPTEmbeddingCache() {

        private Map<String, float[]> cacheMap = new HashMap<>();

        @Nullable
        @Override
        public float[] get(@Nullable String text) {
            return cacheMap.get(text);
        }

        @Override
        public void put(@Nullable String text, @Nonnull float[] embedding) {
            cacheMap.put(text, embedding);
        }

        @Override
        public void clearIfNotModel(String model) {
            cacheMap.clear();
        }
    };

    @InjectMocks
    private GPTEmbeddingService embeddingsService = new GPTEmbeddingServiceImpl();

    public static void main(String[] args) throws Exception {
        RunOpenAIEmbeddings instance = new RunOpenAIEmbeddings();
        instance.setup();
        MockitoAnnotations.openMocks(instance);
        instance.run();
        instance.teardown();
    }

    private void run() {
        List<String> comparedStrings = Arrays.asList(new String[]{
                "Hello, how are you? I'm just chatting with you.",
                "I'm a computer program, I don't have feelings.",
                "Composum Pages is a great CMS."
        });
        String compared = "What is Composum Pages?";
        GPTConfiguration configuration = null;
        List<String> topN = embeddingsService.findMostRelated(compared, comparedStrings, 2, configuration);
        Assert.assertEquals(2, topN.size());
        for (String s : topN) {
            System.out.println("Most related to '" + compared + "': " + s);
        }
    }

}
