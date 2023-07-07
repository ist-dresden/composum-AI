package com.composum.ai.backend.base.service.chat;


import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.CoreMatchers;
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

    @Test
    public void testMergeInMaxTokens() {
        // Both maxTokens are null
        GPTChatRequest chatRequest1 = new GPTChatRequest();
        GPTChatRequest chatRequest2 = new GPTChatRequest();
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMaxTokens(), CoreMatchers.nullValue());

        // Original maxTokens is null, new maxTokens is not
        chatRequest1 = new GPTChatRequest();
        chatRequest2 = new GPTChatRequest();
        chatRequest2.setMaxTokens(100);
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMaxTokens(), is(100));

        // Original maxTokens is not null, new maxTokens is null
        chatRequest1 = new GPTChatRequest();
        chatRequest2 = new GPTChatRequest();
        chatRequest1.setMaxTokens(200);
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMaxTokens(), is(200));

        // Neither maxTokens are null
        chatRequest1 = new GPTChatRequest();
        chatRequest2 = new GPTChatRequest();
        chatRequest1.setMaxTokens(300);
        chatRequest2.setMaxTokens(400);
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMaxTokens(), is(400));
    }

    @Test
    public void testMergeInSystemMessages() {
        // Case 1: Only the first instance has a system message
        GPTChatRequest chatRequest1 = new GPTChatRequest().addMessage(GPTMessageRole.SYSTEM, "System message 1");
        GPTChatRequest chatRequest2 = new GPTChatRequest();
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMessages().get(0).getContent(), is("System message 1"));

        // Case 2: Only the second instance has a system message
        chatRequest1 = new GPTChatRequest();
        chatRequest2 = new GPTChatRequest().addMessage(GPTMessageRole.SYSTEM, "System message 2");
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMessages().get(0).getContent(), is("System message 2"));

        // Case 3: Neither instance has a system message
        chatRequest1 = new GPTChatRequest();
        chatRequest2 = new GPTChatRequest();
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMessages().isEmpty(), is(true));

        // Case 4: Both instances have a system message
        chatRequest1 = new GPTChatRequest().addMessage(GPTMessageRole.SYSTEM, "System message 1");
        chatRequest2 = new GPTChatRequest().addMessage(GPTMessageRole.SYSTEM, "System message 2");
        chatRequest1.mergeIn(chatRequest2);
        ec.checkThat(chatRequest1.getMessages().get(0).getContent(), is("System message 1\n\nSystem message 2"));
    }

}
