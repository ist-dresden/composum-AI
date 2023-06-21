package com.composum.ai.backend.base.service.chat.impl;

import java.util.concurrent.Flow;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;

/**
 * Tries an actual call to ChatGPT with the streaming interface. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceStreamingImpl extends AbstractGPTRunner implements GPTCompletionCallback {

    StringBuilder buffer = new StringBuilder();
    private Flow.Subscription subscription;
    private boolean isFinished;

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceStreamingImpl instance = new RunGPTChatCompletionServiceStreamingImpl();
        instance.setup();
        instance.run();
    }

    private void run() throws InterruptedException {
        GPTChatRequest request = new GPTChatRequest();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 10000; i++) buffer.append("hallo "); // add that to requets to provoke error
        request.addMessage(GPTMessageRole.USER, "Make 5 haiku about the weather" + buffer);
        chatCompletionService.streamingChatCompletion(request, this);
        System.out.println("Call returned.");
        while (!isFinished) Thread.sleep(1000);
        System.out.println("Complete response:");
        System.out.println(buffer);
    }

    @Override
    public void onFinish(GPTFinishReason finishReason) {
        System.out.println("Finished: " + finishReason);
    }

    @Override
    public void onComplete() {
        System.out.println("Completed");
        GPTCompletionCallback.super.onComplete();
        isFinished = true;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("Subscribed");
        this.subscription = subscription;
    }

    @Override
    public void onNext(String item) {
        buffer.append(item);
        System.out.println(item);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}
