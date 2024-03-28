package com.composum.ai.backend.base.service.chat.impl;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to ChatGPT with the streaming interface. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceStreamingLocalModelImpl extends AbstractGPTRunner implements GPTCompletionCallback {

    StringBuilder buffer = new StringBuilder();
    private boolean isFinished;

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceStreamingLocalModelImpl instance = new RunGPTChatCompletionServiceStreamingLocalModelImpl();
        instance.chatCompletionUrl = "http://localhost:1234/v1/chat/completions";
        instance.setup();
        instance.run();
        instance.teardown();
        System.out.println("Done.");
    }

    private void run() throws InterruptedException {
        GPTChatRequest request = new GPTChatRequest();
        StringBuilder requestElongator = new StringBuilder();
        // for (int i = 0; i < 10000; i++) requestElongator.append("hallo "); // add that to requets to provoke error
        request.addMessage(GPTMessageRole.USER, "Make 2 haiku about the weather" + requestElongator);
        chatCompletionService.streamingChatCompletion(request, this);
        System.out.println("Call returned.");
        while (!isFinished) Thread.sleep(1000);
        System.out.println("Complete response:");
        System.out.println(buffer);
    }

    @Override
    public void onFinish(GPTFinishReason finishReason) {
        isFinished = true;
        System.out.println();
        System.out.println("Finished: " + finishReason);
    }

    @Override
    public void setLoggingId(String loggingId) {
        System.out.println("Logging ID: " + loggingId);
    }

    @Override
    public void onNext(String item) {
        buffer.append(item);
        System.out.print(item);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace(System.err);
        isFinished = true;
    }
}
