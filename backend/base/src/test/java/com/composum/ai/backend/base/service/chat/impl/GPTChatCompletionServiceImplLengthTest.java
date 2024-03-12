package com.composum.ai.backend.base.service.chat.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;

/**
 * Integration tests that check what happens if the text is too long.
 * These tests actually access ChatGPT, but are only error cases so don't cost money.
 * It's important to verify that the error cases are handled correctly, though, since they seem a bit brittle and
 * they'd break translation of long texts.
 */
public class GPTChatCompletionServiceImplLengthTest {

    private static final Logger LOG = LoggerFactory.getLogger(GPTChatCompletionServiceImplLengthTest.class);
    private final Logger LOG_IMPL = (SimpleLogger) LoggerFactory.getLogger(GPTChatCompletionServiceImpl.class);

    protected GPTChatCompletionServiceImpl service = new GPTChatCompletionServiceImpl();
    protected GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig config =
            mock(GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class);
    protected BundleContext bundleContext = mock(BundleContext.class);

    public static final List<String> models = Arrays.asList("gpt-3.5-turbo", "gpt-3.5-turbo-16k", "gpt-4", "gpt-4-turbo-preview", "gpt-4-vision-preview");

    @Before
    public void setUp() throws NoSuchFieldException {
        String key = System.getenv("OPENAI_API_KEY");
        assumeNotNull(key); // locally that's often set and is required for the test.
        when(config.openAiApiKey()).thenReturn(key);
        when(config.connectionTimeout()).thenReturn(2);
        when(config.requestTimeout()).thenReturn(5);
        service.activate(config, bundleContext);
    }

    @After
    public void tearDown() {
        service.deactivate();
    }

    @Test
    @Ignore("Takes some time and might fail if OpenAI has trouble.")
    public void testRequireTooManyTokens() {
        for (String model : models) {
            LOG.error("Testing model " + model);
            when(config.defaultModel()).thenReturn(model);
            service.activate(config, bundleContext);
            try {
                GPTChatRequest request = new GPTChatRequest();
                request.setMaxTokens(99999999); // make ChatGPT complain
                request.addMessage(GPTMessageRole.USER, "Say Hi!");
                service.getSingleChatCompletion(request);
                fail("Should have thrown GPTException.GPTContextLengthExceededException for model " + model +
                        "\nFIX THIS! If that doesn't trigger the error, this costs money!");
            } catch (GPTException.GPTContextLengthExceededException e) {
                System.out.println("Received expected exception for model " + model + " : " + e.toString());
            } finally {
                service.deactivate();
            }

        }
    }

    @Test
    @Ignore("Rather play safe since that cost money if it goes through accidentially.")
    public void testTextTooLong() throws NoSuchFieldException, IllegalAccessException {
        // switch off logger temporarily since the request is huge.
        Field currentLevelField = SimpleLogger.class.getDeclaredField("currentLogLevel");
        currentLevelField.setAccessible(true);
        int oldLevel = currentLevelField.getInt(LOG_IMPL);

        // do NOT test this with gpt-4, since a maximum context length request would cost > 10 euro if it goes through!
        when(config.defaultModel()).thenReturn("gpt-3.5-turbo");
        service.activate(config, bundleContext);

        try {
            currentLevelField.setInt(LOG_IMPL, 999);

            GPTChatRequest request = new GPTChatRequest();
            StringBuilder hugetext = new StringBuilder();
            for (int i = 0; i < 50000; i++) {
                // hopefully enough to trigger the error.
                // as of 22.2.2024, the gpt-4-turbo-preview has 128,000 tokens, which would cost 1.28 euro!
                // I guess we'd rather not test that for GPT-4. :-)
                hugetext.append("§:");
            }
            request.addMessage(GPTMessageRole.USER, hugetext.toString());
            service.getSingleChatCompletion(request);
            fail("FIX THIS!!!!! Should have thrown GPTException.GPTContextLengthExceededException." +
                    "If that doesn't trigger the error, this costs money!");
        } catch (GPTException.GPTContextLengthExceededException e) {
            System.out.println("Received expected exception: " + e.toString());
            assertTrue(e.getMessage().contains("context_length_exceeded"));
        } finally {
            currentLevelField.setInt(LOG_IMPL, oldLevel);
        }
    }

}
