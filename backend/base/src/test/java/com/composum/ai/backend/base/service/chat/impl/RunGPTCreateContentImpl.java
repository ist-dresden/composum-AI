package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;

/**
 * Tries an actual call to ChatGPT. Since that costs money (though much less than a cent), needs a secret key and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class RunGPTCreateContentImpl extends AbstractGPTRunner {

    private GPTContentCreationServiceImpl service;

    public static void main(String[] args) throws Exception {
        RunGPTCreateContentImpl instance = new RunGPTCreateContentImpl();
        instance.setup();
        instance.run();
    }

    private void run() throws InterruptedException {
        executePrompt("Create a haiku about the weather.", 20);
        executePrompt("Tell a long joke.", 100);
    }

    private void executePrompt(String prompt, int maxTokens) {
        String result = service.executePrompt(prompt, GPTChatRequest.ofMaxTokens(maxTokens));
        // print parameters and result
        System.out.printf("%nExecuting '%s': %n%s%n%n", prompt, result);
    }

    protected void setup() throws IOException {
        super.setup();
        service = new GPTContentCreationServiceImpl();
        service.chatCompletionService = chatCompletionService;
    }

}
