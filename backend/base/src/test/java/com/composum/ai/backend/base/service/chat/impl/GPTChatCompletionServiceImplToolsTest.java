package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionToolCall;

/**
 * Tests for {@link GPTChatCompletionService} with tool calls.
 */
public class GPTChatCompletionServiceImplToolsTest {

    protected GPTChatCompletionServiceImpl service = new GPTChatCompletionServiceImpl();
    private GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig config =
            mock(GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class);
    private BundleContext bundleContext = mock(BundleContext.class);

    @Before
    public void setUp() {
        Mockito.when(config.openAiApiKey()).thenReturn("sk-abcdefg");
        service.activate(config, bundleContext);
    }

    /**
     * Streaming test from real example.
     */
    @Test
    public void testHandleStreamingEventWithToolCall() {
        String[] lines = ("" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":null},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_bZrlWEFQ7jbTybVQYHg0hDSn\",\"type\":\"function\",\"function\":{\"name\":\"wobbler\",\"arguments\":\"\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"to\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"wobbl\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"e\\\": \\\"h\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"i\\\"}\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":1,\"id\":\"call_om4hHNguyKYCOkhiZu6wo9xA\",\"type\":\"function\",\"function\":{\"name\":\"wobbler\",\"arguments\":\"\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":1,\"function\":{\"arguments\":\"{\\\"to\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":1,\"function\":{\"arguments\":\"wobbl\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":1,\"function\":{\"arguments\":\"e\\\": \\\"h\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":1,\"function\":{\"arguments\":\"o\\\"}\"}}]},\"logprobs\":null,\"finish_reason\":null}]}\n" +
                "{\"id\":\"chatcmpl-ANM95HYArFdhLHNSljHzHawXXAiXT\",\"object\":\"chat.completion.chunk\",\"created\":1730130035,\"model\":\"gpt-4o-mini-2024-07-18\",\"system_fingerprint\":\"fp_8bfc6a7dc2\",\"choices\":[{\"index\":0,\"delta\":{},\"logprobs\":null,\"finish_reason\":\"tool_calls\"}]}\n" +
                "[DONE]"
        ).split("\n");
        GPTCompletionCallback.GPTCompletionCollector callback = new GPTCompletionCallback.GPTCompletionCollector();
        for (String line : lines) {
            service.handleStreamingEvent(callback, 123, "data: " + line);
        }
        assertEquals(GPTFinishReason.TOOL_CALLS, callback.getFinishReason());
        List<ChatCompletionToolCall> calls = callback.getToolCalls();
        assertEquals(2, calls.size());
        assertEquals("call_bZrlWEFQ7jbTybVQYHg0hDSn", calls.get(0).getId());
        assertEquals("wobbler", calls.get(0).getFunction().getName());
        assertEquals("{\"towobble\": \"hi\"}", calls.get(0).getFunction().getArguments());
        assertEquals("call_om4hHNguyKYCOkhiZu6wo9xA", calls.get(1).getId());
        assertEquals("wobbler", calls.get(1).getFunction().getName());
        assertEquals("{\"towobble\": \"ho\"}", calls.get(1).getFunction().getArguments());
    }

}
