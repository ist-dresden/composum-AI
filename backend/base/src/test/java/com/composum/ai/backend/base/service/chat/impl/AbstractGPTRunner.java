package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.commons.threads.impl.DefaultThreadPool;
import org.mockito.Mockito;

public abstract class AbstractGPTRunner {

    protected GPTChatCompletionServiceImpl chatCompletionService;

    protected void setup() throws IOException {
        chatCompletionService = new GPTChatCompletionServiceImpl();
        // read key from file ~/.openaiapi
        Path filePath = Paths.get(System.getProperty("user.home"), ".openaiapi");
        String apiKey = new String(Files.readAllBytes(filePath));

        chatCompletionService.activate(new GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                throw new UnsupportedOperationException("Not implemented yet: .annotationType");
            }

            @Override
            public boolean disabled() {
                return false;
            }

            @Override
            public String openAiApiKey() {
                return apiKey;
            }

            @Override
            public String openAiOrganizationId() {
                return null;
            }

            @Override
            public String openAiApiKeyFile() {
                return null;
            }

            @Override
            public String defaultModel() {
                return "gpt-3.5-turbo";
            }

            @Override
            public String imageModel() {
                return "gpt-4-vision-preview";
            }

            @Override
            public String temperature() {
                return null;
            }

            @Override
            public int maximumTokensPerRequest() {
                return 0;
            }

            @Override
            public int requestTimeout() {
                return 60;
            }

            @Override
            public int connectionTimeout() {
                return 20;
            }
        }, null);
    }

    protected void teardown() {
        chatCompletionService.deactivate();
    }

}
