package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.util.Collections;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.common.io.Resources;

/**
 * Asks ChatGPT about an image.
 */
public class RunGPTChatCompletionServiceImgAnalysisImpl extends AbstractGPTRunner implements GPTCompletionCallback {

    StringBuilder buffer = new StringBuilder();
    private boolean isFinished;

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceImgAnalysisImpl instance = new RunGPTChatCompletionServiceImgAnalysisImpl();
        instance.setup();
        instance.run();
        instance.teardown();
        System.out.println("Done.");
    }

    private void run() throws InterruptedException, IOException {
        GPTChatRequest request = new GPTChatRequest();
        GPTChatMessage imgMsg = makeImageChatMessage();
        request.addMessages(Collections.singletonList(imgMsg));
        request.addMessage(GPTMessageRole.USER, "Describe the image at great length - at least one paragraph.");
        request.setMaxTokens(400);
        chatCompletionService.streamingChatCompletion(request, this);
        System.out.println("Call returned.");
        while (!isFinished) Thread.sleep(1000);
        System.out.println("Complete response:");
        System.out.println(buffer);
    }

    protected GPTChatMessage makeImageChatMessage() throws IOException {
        // GPTChatMessage imgMsg = new GPTChatMessage(GPTMessageRole.USER, null, "https://www.composum.com/assets/pages/composum-pages-edit-view.jpg");
        byte[] imageBytes = Resources.toByteArray(getClass().getResource("/imgtest/imgtest.png"));
        String imageUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
        GPTChatMessage imgMsg = new GPTChatMessage(GPTMessageRole.USER, null, imageUrl);
        return imgMsg;
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
