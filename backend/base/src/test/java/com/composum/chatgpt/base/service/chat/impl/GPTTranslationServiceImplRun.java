package com.composum.chatgpt.base.service.chat.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to ChatGPT. Since that costs money (though much less than a cent), needs a secret key and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class GPTTranslationServiceImplRun {

    private GPTChatCompletionServiceImpl chatCompletionService;
    private GPTTranslationServiceImpl translationService;

    public static void main(String[] args) throws Exception {
        GPTTranslationServiceImplRun instance = new GPTTranslationServiceImplRun();
        instance.setup();
        instance.run();
    }

    private void run() {
        doTranslation("Hello", "en", "de");
        doTranslation("Hello", "Deutsch", "Englisch");
        doTranslation("MUl.s8s8w34rl2", "fr", "en_UK");
        doTranslation("Guten Morgen!", "asdfasdf", "erwew");
        // translate a text of two short paragraphs of two sentences each.
        doTranslation("Hello. How are you?\n\nI am fine. How are you?\nThe translation service seems to work fine now!", "en", "de");
    }

    private void doTranslation(String text, String from, String to) {
        String result = translationService.singleTranslation(text, from, to);
        // print parameters and result
        System.out.printf("%n%ntranslation of '%s' from %s to %s: %s%n%n", text, from, to, result);
    }

    private void setup() throws IOException {
        chatCompletionService = new GPTChatCompletionServiceImpl();
        translationService = new GPTTranslationServiceImpl();
        translationService.chatCompletionService = chatCompletionService;

        // read key from file ~/.openaiapi
        Path filePath = Paths.get(System.getProperty("user.home"), ".openaiapi");
        String apiKey = Files.readString(filePath);

        chatCompletionService.activate(new GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                throw new UnsupportedOperationException("Not implemented yet: .annotationType");
            }

            @Override
            public String openAiApiKey() {
                return apiKey;
            }

            @Override
            public String defaultModel() {
                return "gpt-3.5-turbo";
            }
        });
    }

}
