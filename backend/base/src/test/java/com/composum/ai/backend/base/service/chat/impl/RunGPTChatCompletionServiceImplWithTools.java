package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.Map;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.base.service.chat.GPTTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tries an actual call to ChatGPT with the streaming interface. Since that costs money (though much less than a cent),
 * needs a secret key and takes a couple of seconds, we don't do that as an JUnit test.
 */
public class RunGPTChatCompletionServiceImplWithTools extends AbstractGPTRunner implements GPTCompletionCallback {

    StringBuilder buffer = new StringBuilder();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private boolean isFinished;

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceImplWithTools instance = new RunGPTChatCompletionServiceImplWithTools();
        instance.setup();
        instance.run();
        instance.teardown();
        System.out.println("Done.");
    }

    private void run() throws InterruptedException {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Make 2 haiku about the weather and then wobble them.");
        request.setConfiguration(GPTConfiguration.ofTools(Arrays.asList(wobbler)));
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

    protected GPTTool wobbler = new GPTTool() {
        @Override
        public String getName() {
            return "wobbler";
        }

        @Override
        public String getToolDeclaration() {
            return "{\n" +
                    "  \"type\": \"function\",\n" +
                    "  \"function\": {\n" +
                    "    \"name\": \"wobbler\",\n" +
                    "    \"description\": \"wobbles the string given as argument\",\n" +
                    "    \"parameters\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"properties\": {\n" +
                    "        \"towobble\": {\n" +
                    "          \"type\": \"string\",\n" +
                    "          \"description\": \"The string to wobble.\"\n" +
                    "        }\n" +
                    "      },\n" +
                    "      \"required\": [\"towobble\"],\n" +
                    "      \"additionalProperties\": false\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"strict\": true\n" +
                    "}";
        }

        @Override
        public String execute(String arguments) {
            Map parsedArguments = gson.fromJson(arguments, Map.class);
            String towobble = (String) parsedArguments.get("towobble");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < towobble.length(); i++) {
                char c = towobble.charAt(i);
                if (i % 2 == 0) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
            return result.toString();
        }
    };

}
