package com.composum.ai.backend.slingbase.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mockito;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;

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
        when(service.chatCompletionService.htmlToMarkdown(Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", "This is a test string.");
        parameters.put("title", "This is a test heading");
        Resource component = createMockResource("res", parameters);
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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("jcr:title", "unlabelled");
        parameters.put("asecond", "Should be the second labelled attribute");
        parameters.put("thefirst", "the first labelled attribute");
        parameters.put("unmentioned", "other lattr");
        parameters.put("is:ignored", "denied");
        Resource component = createMockResource("nt:unstructured", parameters);

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
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("thefirst", "this is there");
        parameters.put("ignoredValue1", "true");
        parameters.put("ignoredValue2", "123");
        parameters.put("is:ignored", "denied");
        Resource component = createMockResource("nt:unstructured", parameters);

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

    @Test
    public void testGetComponentLinks() {
        // Setup Mock Resources
        Resource rootResource = context.create().resource("/content/parent/path/jcr:content/res",
                Collections.singletonMap("link1", "/content/parent/path/res1"));
        context.create().resource("/content/parent/path/jcr:content/res/child",
                Collections.singletonMap("link2", "/content/parent/path/child1"));
        // set up resources for the links, one with a title, one with a jcr:title
        context.create().resource("/content/parent/path/res1",
                Collections.singletonMap("title", "res1"));
        context.create().resource("/content/parent/path/child1");
        context.create().resource("/content/parent/path/child1/jcr:content",
                Collections.singletonMap("title", "child1"));

        // Execute Method
        List<ApproximateMarkdownService.Link> links = service.getComponentLinks(rootResource);

        // Assertions
        ec.checkThat(links.size(), is(2)); // Check if two links are returned
        ec.checkThat(links.get(0).getPath(), is("/content/parent/path/res1")); // Check first link path
        ec.checkThat(links.get(0).getTitle(), is("res1")); // Check first link title
        ec.checkThat(links.get(1).getPath(), is("/content/parent/path/child1")); // Check second link path
        ec.checkThat(links.get(1).getTitle(), is("child1")); // Check second link title
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoWhitelist() throws URISyntaxException, IOException {
        service.getMarkdown(new URI("http://example.com"));
    }

    @Test
    public void testUrlBlacklisting() throws URISyntaxException, IOException, NoSuchMethodException {
        // in method setup we make sure the default is returned by config
        when(config.urlSourceWhitelist()).thenReturn(new String[]{".*"});
        service.activate(config);
        // service.getMarkdown(new URI("http://example.com")); // doesn't work without internet connection
        for (String url : new String[]
                {"http://localhost/", "https://localhost/", "http://1.2.3.4/", "http://[::1]/", "http://1.2.3.4/:8080", "http://[::1]/:8080"}) {
            ec.checkThrows(IllegalArgumentException.class, () -> service.getMarkdown(new URI(url)));
        }
    }

    @Test
    @Ignore("Doesn't work without internet connection")
    public void testUrlWhitelisting() throws URISyntaxException, IOException, NoSuchMethodException {
        when(config.urlSourceWhitelist()).thenReturn(new String[]{"https://www.example.net/.*"});
        service.activate(config);
        service.getMarkdown(new URI("https://www.example.net/"));
        ec.checkThrows(IllegalArgumentException.class, () -> service.getMarkdown(new URI("http://example.org")));
    }

}
