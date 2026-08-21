package com.composum.ai.composum.bundle;


import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.base.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GPTChatRequestSerializationTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testSerializeGPTChatRequest() throws Exception {
        ObjectMapper objectMapper = JsonUtil.newObjectMapper();

        GPTChatMessage chatMessage1 = new GPTChatMessage(GPTMessageRole.ASSISTANT, "Answer 1");
        GPTChatMessage chatMessage2 = new GPTChatMessage(GPTMessageRole.USER, "Another question");
        List<GPTChatMessage> messages = List.of(chatMessage1, chatMessage2);

        String json = objectMapper.writeValueAsString(messages);
        // System.out.println(json);

        // deserialize explicitly as List<GPTChatMessage>
        List<GPTChatMessage> messagesDeser = objectMapper.readValue(json, new TypeReference<ArrayList<GPTChatMessage>>() {
        });
        // System.out.println(messagesDeser);
        ec.checkThat(messagesDeser.size(), is(2));
        ec.checkThat(messagesDeser.get(0).getRole(), is(GPTMessageRole.ASSISTANT));
        ec.checkThat(messagesDeser.get(0).getContent(), is("Answer 1"));
        ec.checkThat(messagesDeser.get(1).getRole(), is(GPTMessageRole.USER));
        ec.checkThat(messagesDeser.get(1).getContent(), is("Another question"));
    }

}
