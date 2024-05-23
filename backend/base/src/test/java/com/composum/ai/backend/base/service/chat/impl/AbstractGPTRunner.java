package com.composum.ai.backend.base.service.chat.impl;

import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.CHAT_COMPLETION_URL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_EMBEDDINGS_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_HIGH_INTELLIGENCE_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_IMAGE_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.OPENAI_EMBEDDINGS_URL;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockito.Spy;

public abstract class AbstractGPTRunner {

    @Spy
    protected GPTChatCompletionServiceImpl chatCompletionService;

    protected String chatCompletionUrl = CHAT_COMPLETION_URL;

    protected String apiKey;

    protected void setup() throws IOException {

        chatCompletionService = new GPTChatCompletionServiceImpl();
        // read key from file ~/.openai-api-key.txt
        Path filePath = Paths.get(System.getProperty("user.home"), ".openai-api-key.txt");
        apiKey = System.getenv("OPENAI_API_KEY");
        if (Files.isReadable(filePath)) {
            apiKey = new String(Files.readAllBytes(filePath));
        }

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
            public String chatCompletionUrl() {
                return chatCompletionUrl;
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
                return DEFAULT_MODEL;
            }

            @Override
            public String highIntelligenceModel() {
                return DEFAULT_HIGH_INTELLIGENCE_MODEL;
            }

            @Override
            public String imageModel() {
                return DEFAULT_IMAGE_MODEL;
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
            public int maximumTokensPerResponse() {
                return 4096;
            }

            @Override
            public int requestTimeout() {
                return 60;
            }

            @Override
            public int requestsPerMinute() {
                return 20;
            }

            @Override
            public int requestsPerHour() {
                return 60;
            }

            @Override
            public int requestsPerDay() {
                return 120;
            }

            @Override
            public String embeddingsUrl() {
                return OPENAI_EMBEDDINGS_URL;
            }

            @Override
            public String embeddingsModel() {
                return DEFAULT_EMBEDDINGS_MODEL;
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
