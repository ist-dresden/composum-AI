package com.composum.chatgpt.base.service.chat.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractGPTRunner {

    protected GPTChatCompletionServiceImpl chatCompletionService;

    protected void setup() throws IOException {
        chatCompletionService = new GPTChatCompletionServiceImpl();
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
