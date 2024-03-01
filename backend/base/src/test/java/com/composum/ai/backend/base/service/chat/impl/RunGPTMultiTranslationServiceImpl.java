package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

/**
 * Tries an actual call to ChatGPT. Since that costs money (though much less than a cent), needs a secret key and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class RunGPTMultiTranslationServiceImpl extends AbstractGPTRunner {

    private GPTTranslationServiceImpl translationService;

    public static void main(String[] args) throws Exception {
        RunGPTMultiTranslationServiceImpl instance = new RunGPTMultiTranslationServiceImpl();
        instance.setup();
        instance.run();
    }

    private void run() {
        // checkTranslation(Arrays.asList());
        // checkTranslation(Arrays.asList("Hi!", "Good morning"));
        checkTranslation(Arrays.asList("Hi!", "Good morning", "How are you?", "More", "Let's translate as translate can."));
        if (0 == 0) {
            List<String> veryLongArray = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                veryLongArray.add("This is a very long text intended to hit the token limit and lead to a complaint by ChatGPT and see what happens then.");
            }
            checkTranslation(veryLongArray);
        }
        System.out.println("DONE");
    }

    private List<String> checkTranslation(List<String> totranslate) {
        List<String> result = translationService.fragmentedTranslation(totranslate, "de", null);
        System.out.println("translation of\n" + totranslate + "\nto de:\n" + result + "\n\n");
        return result;
    }

    protected void setup() throws IOException {
        super.setup();
        translationService = new GPTTranslationServiceImpl();
        translationService.chatCompletionService = chatCompletionService;
        translationService.activate(Mockito.mock(GPTTranslationServiceImpl.Config.class));
    }

}
