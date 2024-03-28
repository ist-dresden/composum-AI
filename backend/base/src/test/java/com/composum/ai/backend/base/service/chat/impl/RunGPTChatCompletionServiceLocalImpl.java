package com.composum.ai.backend.base.service.chat.impl;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to a local model, as e.g. set up with LM Studio.
 * Since that needs setup and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceLocalImpl extends AbstractGPTRunner {

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceLocalImpl instance = new RunGPTChatCompletionServiceLocalImpl();
        instance.chatCompletionUrl = "http://localhost:1234/v1/chat/completions";
        instance.setup();
        instance.run();
        instance.teardown();
    }

    private void run() {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Hello");
        String result = chatCompletionService.getSingleChatCompletion(request);
        System.out.println("result = " + result);
    }

}
