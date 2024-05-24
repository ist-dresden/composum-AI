package com.composum.ai.composum.bundle;


import static org.hamcrest.CoreMatchers.is;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class GsonGPTChatRequestTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testSerializeGPTChatRequest() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        GPTChatMessage chatMessage1 = new GPTChatMessage(GPTMessageRole.ASSISTANT, "Answer 1");
        GPTChatMessage chatMessage2 = new GPTChatMessage(GPTMessageRole.USER, "Another question");
        List<GPTChatMessage> messages = List.of(chatMessage1, chatMessage2);

        String json = gson.toJson(messages);
        // System.out.println(json);

        // deserialize explicitly as List<GPTChatMessage> . To that, we need to pass the type information
        Type listOfMyClassObject = new TypeToken<ArrayList<GPTChatMessage>>() {
        }.getType();

        List<GPTChatMessage> messagesDeser = gson.fromJson(json, listOfMyClassObject);
        // System.out.println(messagesDeser);
        ec.checkThat(messagesDeser.size(), is(2));
        ec.checkThat(messagesDeser.get(0).getRole(), is(GPTMessageRole.ASSISTANT));
        ec.checkThat(messagesDeser.get(0).getContent(), is("Answer 1"));
        ec.checkThat(messagesDeser.get(1).getRole(), is(GPTMessageRole.USER));
        ec.checkThat(messagesDeser.get(1).getContent(), is("Another question"));
    }

}
