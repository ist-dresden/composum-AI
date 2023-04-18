package com.composum.chatgpt.base.service.chat.impl;

import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to ChatGPT. Since that costs money (though much less than a cent), needs a secret key and takes a couple of seconds,
 * we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceImpl extends AbstractGPTRunner {

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceImpl instance = new RunGPTChatCompletionServiceImpl();
        instance.setup();
        instance.run();
    }

    private void run() {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Hello");
        String result = chatCompletionService.getSingleChatCompletion(request);
        System.out.println("result = " + result);
    }

}
