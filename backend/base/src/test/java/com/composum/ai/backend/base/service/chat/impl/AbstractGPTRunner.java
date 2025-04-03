package com.composum.ai.backend.base.service.chat.impl;

import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_EMBEDDINGS_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_HIGH_INTELLIGENCE_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_IMAGE_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.DEFAULT_MODEL;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.OPENAI_EMBEDDINGS_URL;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.composum.ai.backend.base.service.chat.GPTBackendConfiguration;
import com.composum.ai.backend.base.service.chat.GPTBackendsService;

public abstract class AbstractGPTRunner {

    @Mock
    protected GPTBackendsService backendsService;

    @Mock
    protected GPTBackendConfiguration openAIBackend;

    @InjectMocks
    protected GPTChatCompletionServiceImpl chatCompletionService = new GPTChatCompletionServiceImpl();

    protected String chatCompletionUrl = "https://api.openai.com/v1/chat/completions";

    protected String apiKey;

    private AutoCloseable mocks;

    protected void setup() throws IOException {
        // read key from file ~/.openai-api-key.txt
        Path filePath = Paths.get(System.getProperty("user.home"), ".openai-api-key.txt");
        apiKey = System.getenv("OPENAI_API_KEY");
        if (Files.isReadable(filePath)) {
            apiKey = new String(Files.readAllBytes(filePath));
        }

        this.mocks = MockitoAnnotations.openMocks(this);
        when(backendsService.getConfigurationForModel(DEFAULT_MODEL)).thenReturn(openAIBackend);
        when(backendsService.getModelNameInBackend(DEFAULT_MODEL)).thenReturn(DEFAULT_MODEL);
        when(openAIBackend.backendId()).thenReturn("OpenAI");
        when(openAIBackend.apiEndpoint()).thenReturn("https://api.openai.com/v1/chat/completions");
        when(openAIBackend.additionalHeader1Key()).thenReturn("Authorization");
        when(openAIBackend.additionalHeader1Value()).thenReturn("Bearer " + apiKey);
        when(openAIBackend.models()).thenReturn(DEFAULT_MODEL);

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

    protected void teardown() throws Exception {
        chatCompletionService.deactivate();
        mocks.close();
    }

}
