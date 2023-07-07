package com.composum.ai.backend.base.service.chat;


import static org.hamcrest.CoreMatchers.is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class GPTChatRequestTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testMergeIn() {
        // Create two instances of GPTChatRequest
        GPTChatRequest chatRequest1 = new GPTChatRequest();
        GPTChatRequest chatRequest2 = new GPTChatRequest();

        // Populate the instances with different system messages and other messages
        // Add system messages and other messages
        chatRequest1.addMessage(GPTMessageRole.SYSTEM, "System message 1");
        chatRequest1.addMessage(GPTMessageRole.USER, "User message 1");
        chatRequest2.addMessage(GPTMessageRole.SYSTEM, "System message 2");
        chatRequest2.addMessage(GPTMessageRole.ASSISTANT, "Assistant message 2");

        // Call the mergeIn method on one instance, passing the other instance as an argument
        chatRequest1.mergeIn(chatRequest2);

        System.out.println(chatRequest1);

        // Verify that the system messages and other messages from the second instance have been correctly added to the first instance
        ec.checkThat(chatRequest1.getMessages().size(), is(3));
        ec.checkThat(chatRequest1.getMessages().get(0).getRole(), is(GPTMessageRole.SYSTEM));
        ec.checkThat(chatRequest1.getMessages().get(0).getContent(), is("System message 1\n\nSystem message 2"));
        ec.checkThat(chatRequest1.getMessages().get(1).getRole(), is(GPTMessageRole.USER));
        ec.checkThat(chatRequest1.getMessages().get(1).getContent(), is("User message 1"));
        ec.checkThat(chatRequest1.getMessages().get(2).getRole(), is(GPTMessageRole.ASSISTANT));
        ec.checkThat(chatRequest1.getMessages().get(2).getContent(), is("Assistant message 2"));
    }

    // ChatGPTTask: re-read this file and implement test cases for merging in the maxTokens parameter , all 4 cases wrt. being null

}
