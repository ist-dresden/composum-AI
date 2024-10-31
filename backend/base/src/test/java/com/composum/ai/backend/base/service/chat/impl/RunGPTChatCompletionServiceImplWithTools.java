package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.base.service.chat.GPTTool;
import com.composum.ai.backend.base.service.chat.GPTToolCall;
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
    List<GPTToolCall> toolCalls;

    public static void main(String[] args) throws Exception {
        RunGPTChatCompletionServiceImplWithTools instance;
        if (0 == 1) {
            instance = new RunGPTChatCompletionServiceImplWithTools();
            instance.setup();
            instance.runWithoutAutomaticCall();
            instance.teardown();
            System.out.println("######################### Done without automatic call #########################");
        }

        instance = new RunGPTChatCompletionServiceImplWithTools();
        instance.setup();
        instance.runWithAutomaticCall();
        instance.teardown();
        System.out.println("######################### Done. ######################### ");
    }

    private void runWithoutAutomaticCall() throws InterruptedException {
        GPTChatRequest request = makeRequest();
        chatCompletionService.streamingChatCompletion(request, this);
        System.out.println("Call returned.");
        while (!isFinished) Thread.sleep(1000);
        System.out.println("Complete response:");
        System.out.println(buffer);
        System.out.println("Tool calls:");
        System.out.println(gson.toJson(toolCalls));
    }

    private void runWithAutomaticCall() throws InterruptedException {
        GPTChatRequest request = makeRequest();
        chatCompletionService.streamingChatCompletionWithToolCalls(request, this);
        System.out.println("Call returned.");
        while (!isFinished) Thread.sleep(1000);
        System.out.println("Complete response:");
        System.out.println(buffer);
    }

    private GPTChatRequest makeRequest() {
        GPTChatRequest request = new GPTChatRequest();
        request.addMessage(GPTMessageRole.USER, "Wobble the string 'hihihi' and the string 'houhou'.");
        request.setConfiguration(GPTConfiguration.ofTools(Arrays.asList(wobbler)));
        return request;
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
    public GPTToolExecutionContext getToolExecutionContext() {
        return null;
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

    @Override
    public void toolDelta(List<GPTToolCall> toolCalls) {
        this.toolCalls = GPTToolCall.mergeDelta(this.toolCalls, toolCalls);
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
        public String execute(String arguments, GPTToolExecutionContext context) {
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
// sequence of data for a tool call:
//  {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"role":"assistant","content":null,"tool_calls":[{"index":0,"id":"call_ZkuSWztTvaOxQeT6TFITm3sa","type":"function","function":{"name":"wobbler","arguments":""}}],"refusal":null},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\""}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"tow"}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"ob"}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"ble"}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\":\""}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"hi"}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\"}"}}]},"logprobs":null,"finish_reason":null}]}
// {"id":"chatcmpl-AIXBVjvIrBwLMcdUfqBsCEVwteQuK","object":"chat.completion.chunk","created":1728980829,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"tool_calls"}]}
// [DONE]

// or two calls:
//  {"id":"chatcmpl-AIgs1gFG3stX6MiLD2jQkeam50HJQ","object":"chat.completion.chunk","created":1729018061,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_g44GU4kWpf8Q8T3pCmB54GWa","type":"function","function":{"name":"wobbler","arguments":""}}]},"logprobs":null,"finish_reason":null}]}
// ... and then ...
//  {"id":"chatcmpl-AIgs1gFG3stX6MiLD2jQkeam50HJQ","object":"chat.completion.chunk","created":1729018061,"model":"gpt-4o-mini-2024-07-18","system_fingerprint":"fp_e2bde53e6e","choices":[{"index":0,"delta":{"tool_calls":[{"index":1,"function":{"arguments":"o\"}"}}]},"logprobs":null,"finish_reason":null}]}
