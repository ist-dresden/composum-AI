package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

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
        String richtext = "<h2>Strong focus on customer orientation</h2><p>We combine <a title=\"Automation, manufacturing and connection technologies\" href=\"/content/site/es/es-es/technologies.html\" target=\"_self\" rel=\"noopener noreferrer\">resources</a>, technology and <a href=\"http://www.example.net/intelligent/products/and/solutions\" target=\"_blank\" title=\"intelligent products and solutions\">people</a> to jointly create a positive change</p>";
        checkTranslation(Arrays.asList(richtext));
        if (0 == 1) {
            List<String> veryLongArray = new ArrayList<>();
            for (int i = 0; i < 30; i++) { // Well, it doesn't hit the token limit anymore.
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
