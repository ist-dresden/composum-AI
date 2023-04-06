package com.composum.chatgpt.base.service.chat.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;
import com.composum.chatgpt.base.service.chat.impl.GPTChatCompletionServiceImpl;

/**
 * Tries an actual call to ChatGPT. Since that costs money (though much less than a cent), needs a secret key and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class GPTChatCompletionServiceImplRun {

    private GPTChatCompletionServiceImpl impl;

    public static void main(String[] args) throws Exception {
        GPTChatCompletionServiceImplRun instance = new GPTChatCompletionServiceImplRun();
        instance.setup();
        instance.run();
    }

    private void run() {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Hello");
        String result = impl.getSingleChatCompletion(request);
        System.out.println("result = " + result);
    }

    private void setup() throws IOException {
        impl = new GPTChatCompletionServiceImpl();
        // read key from file ~/.openaiapi
        Path filePath = Paths.get(System.getProperty("user.home"), ".openaiapi");
        String apiKey = Files.readString(filePath);

        impl.activate(new GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig() {
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
