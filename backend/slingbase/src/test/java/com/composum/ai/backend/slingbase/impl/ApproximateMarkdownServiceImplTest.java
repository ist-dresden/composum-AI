package com.composum.ai.backend.slingbase.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;

/**
 * Test for {@link ApproximateMarkdownServiceImpl}, mostly generated with the AI and then fixed.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApproximateMarkdownServiceImplTest {

    private ApproximateMarkdownServiceImpl service;
    private StringWriter writer;
    private PrintWriter printWriter;

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() {
        service = new ApproximateMarkdownServiceImpl();
        // mock service.chatCompletionService
        service.chatCompletionService = mock(GPTChatCompletionService.class);
        writer = new StringWriter();
        printWriter = new PrintWriter(writer);
        service.plugins = Collections.emptyList();
    }

    @Test
    public void testMarkdownWithNullResource() {
        service.approximateMarkdown(null, printWriter);
        assertEquals("", writer.toString());
    }

    @Test
    public void testApproximateMarkdownForSuccess() {
        Resource component = createMockResource("res", Map.of("text", "This is a test string.", "title", "This is a test heading"));
        service.approximateMarkdown(component, printWriter);
        assertEquals("## This is a test heading\n" +
                "This is a test string.\n" +
                "\n", writer.toString());
    }

    @Test
    public void testGetMarkdownWithNullInput() {
        String markdown = service.approximateMarkdown(null);
        assertTrue(markdown.isEmpty());
    }

    @Test
    public void testGetMarkdownWithNonHtmlInput() {
        String str = "This is a test string.";
        String markdown = service.getMarkdown(str);
        assertEquals(str.trim(), markdown);
    }

    @Test
    public void testGetMarkdownWithHtmlInput() {
        String str = "<strong>This</strong> is <em>a</em> test string.";
        when(service.chatCompletionService.htmlToMarkdown(str)).thenReturn("**This** is *a* test string.");
        String markdown = service.getMarkdown(str);
        assertEquals("**This** is *a* test string.", markdown.trim());
    }

    private Resource createMockResource(String resourceType, Map<String, Object> attributes) {
        Map<String, Object> props = new HashMap<>(attributes);
        props.put("sling:resourceType", resourceType);
        return context.create().resource("/content/parent/path/res", props);
    }

}
