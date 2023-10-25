package com.composum.ai.backend.base.service.chat.impl;

import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.COMPOSUM_AI_CHAT_GPT;

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

    protected ThreadPoolManager mockThreadPoolManager = Mockito.mock(ThreadPoolManager.class);

    protected DefaultThreadPool defaultThreadPool;

    protected void setup() throws IOException {
        chatCompletionService = new GPTChatCompletionServiceImpl() {{
            this.threadPoolManager = mockThreadPoolManager;
        }};
        Mockito.when(mockThreadPoolManager.get(COMPOSUM_AI_CHAT_GPT)).thenAnswer(invocation -> {
            if (defaultThreadPool == null) {
                defaultThreadPool = new DefaultThreadPool(COMPOSUM_AI_CHAT_GPT, null);
            }
            return defaultThreadPool;
        });
        // read key from file ~/.openaiapi
        Path filePath = Paths.get(System.getProperty("user.home"), ".openaiapi");
        String apiKey = Files.readString(filePath);

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
            public String openAiApiKeyFile() {
                return null;
            }

            @Override
            public String defaultModel() {
                return "gpt-3.5-turbo";
            }

            @Override
            public String temperature() {
                return null;
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

    protected void shutdown() {
        if (defaultThreadPool != null) {
            defaultThreadPool.shutdown();
        }
    }

}
