package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

/**
 * Tries an actual call to OpenAI Embeddings Service. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunGPTOpenAIEmbedding extends AbstractGPTRunner {

    public static void main(String[] args) throws Exception {
        RunGPTOpenAIEmbedding instance = new RunGPTOpenAIEmbedding();
        instance.setup();
        instance.run();
        instance.teardown();
    }

    private void run() {
        List<String> request = Arrays.asList(new String[]{"Hello", "World"});
        List<float[]> response = chatCompletionService.getEmbeddings(request, null);
        Assert.assertEquals(request.size(), response.size());
        for (int i = 0; i < request.size(); i++) {
            Assert.assertNotNull(response.get(i));
            System.out.println("embedding for '" + request.get(i) + "':\n" + Arrays.toString(response.get(i)));
        }
    }

}
