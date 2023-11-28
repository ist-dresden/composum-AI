package com.composum.ai.backend.slingbase.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mockito;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.google.common.collect.ImmutableMap;

/**
 * Test for {@link ApproximateMarkdownServiceImpl}, mostly generated with the AI and then fixed.
 */
public class ApproximateMarkdownServiceImplTest {

    private ApproximateMarkdownServiceImpl.Config config;
    private ApproximateMarkdownServiceImpl service;
    private StringWriter writer;
    private PrintWriter printWriter;
    private SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
    private SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() {
        service = new ApproximateMarkdownServiceImpl();
        config = mock(ApproximateMarkdownServiceImpl.Config.class,
                withSettings().defaultAnswer(invocation -> invocation.getMethod().getDefaultValue()));
        when(config.labelledAttributeOrder()).thenReturn(new String[]{"thefirst", "asecond"});
        service.activate(config);

        service.chatCompletionService = mock(GPTChatCompletionService.class);
        writer = new StringWriter();
        printWriter = new PrintWriter(writer);
        service.plugins = Collections.emptyList();
    }

    @Test
    public void testMarkdownWithNullResource() {
        service.approximateMarkdown(null, printWriter, request, response);
        assertEquals("", writer.toString());
    }

    @Test
    public void testApproximateMarkdownForSuccess() {
        Resource component = createMockResource("res", Map.of("text", "This is a test string.", "title", "This is a test heading"));
        service.approximateMarkdown(component, printWriter, request, response);
        assertEquals("## This is a test heading\n" +
                "This is a test string.\n" +
                "\n", writer.toString());
    }

    @Test
    public void testGetMarkdownWithNullInput() {
        String markdown = service.approximateMarkdown(null, request, response);
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


    @Test
    public void testLabelledAttributes() {
        Resource component = createMockResource("nt:unstructured",
                ImmutableMap.of("jcr:title", "unlabelled",
                        "asecond", "Should be the second labelled attribute",
                        "thefirst", "the first labelled attribute",
                        "unmentioned", "other lattr",
                        "is:ignored", "denied"
                ));

        service.approximateMarkdown(component, printWriter, request, response);
        String expectedOutput =
                "## unlabelled\n" +
                        "\n" +
                        "thefirst: the first labelled attribute <br>\n" +
                        "asecond: Should be the second labelled attribute <br>\n" +
                        "unmentioned: other lattr <br>\n" +
                        "\n";
        assertThat(writer.toString(), is(expectedOutput));
    }

    @Test
    public void testLabelledAttributesIgnoredValues() {
        Resource component = createMockResource("nt:unstructured",
                ImmutableMap.of(
                        "thefirst", "this is there",
                        "ignoredValue1", "true",
                        "ignoredValue2", "123",
                        "is:ignored", "denied"
                ));

        service.approximateMarkdown(component, printWriter, request, response);
        String expectedOutput =
                "thefirst: this is there <br>\n" +
                        "\n";
        assertThat(writer.toString(), is(expectedOutput));
    }

    private Resource createMockResource(String resourceType, Map<String, Object> attributes) {
        Map<String, Object> props = new HashMap<>(attributes);
        props.put("sling:resourceType", resourceType);
        return context.create().resource("/content/parent/path/res", props);
    }

}