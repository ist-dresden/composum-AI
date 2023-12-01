package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.common.collect.ImmutableMap;

public class GPTChatMessagesTemplateTest {

    public static final String TEMPLATE = "testing123chat";


    /**
     * We load the template {@link #TEMPLATE} and replace the placeholders "this" and "that" with some values,
     * and then check whether the result is replaced properly. The template is:
     * # comment to ignore
     * ---------- system ----------
     * system message content
     * ---------- user ----------
     * user message template
     * ---------- assistant ----------
     * assistant message template
     * ---------- user ----------
     * data with placeholder ${this} or ${that}
     */
    @Test
    public void getMessages() {
        GPTChatMessagesTemplate template = new GPTChatMessagesTemplate(GPTChatMessagesTemplate.class.getClassLoader(), TEMPLATE);
        List<GPTChatMessage> result = template.getMessages(ImmutableMap.of("this", "thisvalue", "that", "that value"));
        assertEquals("Actual result: " + result, 5, result.size());
        // just compare the tostring values of the messages with the expected values
        assertEquals("GPTChatMessage{role=system, text='system message content'}", result.get(0).toString());
        assertEquals("GPTChatMessage{role=user, text='user message template'}", result.get(1).toString());
        assertEquals("GPTChatMessage{role=assistant, text='assistant message template'}", result.get(2).toString());
        assertEquals("GPTChatMessage{role=user, text='data with placeholder thisvalue or that value'}", result.get(3).toString());
    }


    @Test
    public void testGetMessages() {
        GPTChatMessagesTemplate template = new GPTChatMessagesTemplate(GPTChatMessagesTemplate.class.getClassLoader(), TEMPLATE);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("this", "foo");
        placeholders.put("that", "bar");
        List<GPTChatMessage> messages = template.getMessages(placeholders);
        assertEquals(5, messages.size());
        assertEquals(GPTMessageRole.SYSTEM, messages.get(0).getRole());
        assertEquals("system message content", messages.get(0).getContent());
        assertEquals(GPTMessageRole.USER, messages.get(1).getRole());
        assertEquals("user message template", messages.get(1).getContent());
        assertEquals(GPTMessageRole.ASSISTANT, messages.get(2).getRole());
        assertEquals("assistant message template", messages.get(2).getContent());
        assertEquals(GPTMessageRole.USER, messages.get(3).getRole());
        assertEquals("data with placeholder foo or bar", messages.get(3).getContent());
        assertEquals(GPTMessageRole.ASSISTANT, messages.get(4).getRole());
        assertEquals("", messages.get(4).getContent());
    }

    @Test(expected = GPTException.class)
    public void testGetMessages_missingPlaceholder() throws NoSuchFieldException, IllegalAccessException {
        Logger log = GPTChatMessagesTemplate.LOG;
        Field field = SimpleLogger.class.getDeclaredField("currentLogLevel");
        field.setAccessible(true);
        int oldLogLevel = field.getInt(log);
        field.set(log, 1000); // annoying way to remove logging of the expected error
        try {
            GPTChatMessagesTemplate template = new GPTChatMessagesTemplate(GPTChatMessagesTemplate.class.getClassLoader(), TEMPLATE);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("this", "foo");
            template.getMessages(placeholders);
        } finally {
            field.set(log, oldLogLevel);
        }
    }

    @Test(expected = GPTException.class)
    public void testGetMessagesWithNonexistentTemplate() {
        new GPTChatMessagesTemplate(GPTChatMessagesTemplate.class.getClassLoader(), "nonexistent-template");
    }
}
