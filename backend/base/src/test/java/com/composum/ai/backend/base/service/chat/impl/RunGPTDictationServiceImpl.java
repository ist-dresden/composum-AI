package com.composum.ai.backend.base.service.chat.impl;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.mockito.Mockito;

public class RunGPTDictationServiceImpl {

    public static void main(String[] args) throws IOException, URISyntaxException {
        GPTDictationServiceImpl service = new GPTDictationServiceImpl();
        try {
            GPTDictationServiceImpl.GPTDictationServiceConfig config = Mockito.mock(GPTDictationServiceImpl.GPTDictationServiceConfig.class);
            when(config.enabled()).thenReturn(true);
            when(config.maxRequestSize()).thenReturn(1000000);
            service.activate(config);

            InputStream stream = RunGPTDictationServiceImpl.class.getResourceAsStream("/translatetest/test.wav");
            String result = service.transcribe(stream, "audio/wav", "de", null, null);
            System.out.println("transcription: " + result);
        } finally {
            service.deactivate();
        }
        System.out.printf("DONE");
    }
}
