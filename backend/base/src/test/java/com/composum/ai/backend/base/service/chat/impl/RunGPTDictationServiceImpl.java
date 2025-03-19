package com.composum.ai.backend.base.service.chat.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.message.BasicHttpRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

public class RunGPTDictationServiceImpl {

    public static void main(String[] args) throws IOException, URISyntaxException {
        GPTDictationServiceImpl service = new GPTDictationServiceImpl() {{
            openAIHelper = mock(GPTInternalOpenAIHelper.class);
            GPTInternalOpenAIHelper.GPTInternalOpenAIHelperInst inst = new GPTInternalOpenAIHelper.GPTInternalOpenAIHelperInst() {
                @Override
                void initRequest(@Nonnull BasicHttpRequest request, @Nullable GPTConfiguration configuration) {
                    String apiKey = System.getenv("OPENAI_API_KEY");
                    request.addHeader("Authorization", "Bearer " + apiKey);
                }
            };
            when(openAIHelper.getInstance()).thenReturn(inst);
            when(openAIHelper.isEnabled(any())).thenReturn(true);
        }};
        try {
            GPTDictationServiceImpl.GPTDictationServiceConfig config = mock(GPTDictationServiceImpl.GPTDictationServiceConfig.class);
            when(config.disabled()).thenReturn(false);
            when(config.maxRequestSize()).thenReturn(1000000);
            service.activate(config);

            InputStream stream = RunGPTDictationServiceImpl.class.getResourceAsStream("/translatetest/test.wav");
            String result = service.transcribe(stream, "audio/wav", null, null, null);
            System.out.println("transcription: " + result);
        } finally {
            service.deactivate();
        }
        System.out.printf("DONE");
    }
}
