package com.composum.chatgpt.base.service.chat.impl;

import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTCompletionCallback;
import com.composum.chatgpt.base.service.chat.GPTFinishReason;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to ChatGPT with the streaming interface. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceStreamingImpl extends AbstractGPTRunner implements GPTCompletionCallback {

    StringBuilder buffer = new StringBuilder();

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceStreamingImpl instance = new RunGPTChatCompletionServiceStreamingImpl();
        instance.setup();
        instance.run();
    }

    private void run() {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Make a haiku about the weather");
        chatCompletionService.streamingChatCompletion(request, this);
        System.out.println("Finished");
        System.out.println("Complete response:");
        System.out.println(buffer);
    }

    @Override
    public void receiveNextData(String data) {
        buffer.append(data);
        System.out.println(data);
    }

    @Override
    public void receiveFinish(GPTFinishReason finishReason) {
        System.out.println("Finished: " + finishReason);
    }
}
