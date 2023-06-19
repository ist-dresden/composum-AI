package com.composum.ai.composum.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
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
    private Resource component;
    private StringWriter writer;
    private PrintWriter printWriter;

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() throws Exception {
        service = new ApproximateMarkdownServiceImpl();
        // mock service.chatCompletionService
        service.chatCompletionService = mock(GPTChatCompletionService.class);
        writer = new StringWriter();
        printWriter = new PrintWriter(writer);
    }

    @After
    public void tearDown() throws Exception {
        printWriter.close();
        writer.close();
    }

    @Test
    public void testMarkdownWithNullResource() {
        service.approximateMarkdown(null, printWriter);
        assertEquals("", writer.toString());
    }

    @Test
    public void testTableHandlingWithNonTableResource() {
        component = createMockResource("not/table", new HashMap<String, Object>());
        service.approximateMarkdown(component, printWriter);
        assertEquals("", writer.toString());
    }

    @Test
    public void testTableHandlingWithTableResource() {
        ResourceBuilder tableBuilder = context.build().resource("/content/test", "sling:resourceType", "composum/pages/components/composed/table", "title", "Test Table");
        Resource table = tableBuilder.getCurrentParent();
        ResourceBuilder row1Builder = tableBuilder.resource("row1", "sling:resourceType", "composum/pages/components/composed/table/row");
        row1Builder.resource("r1c1", "sling:resourceType", "composum/pages/components/composed/table/cell", "text", "r1c1");
        row1Builder.resource("r1c2", "sling:resourceType", "composum/pages/components/composed/table/cell", "text", "r1c2");
        ResourceBuilder row2Builder = tableBuilder.resource("row2", "sling:resourceType", "composum/pages/components/composed/table/row");
        row2Builder.resource("r2c1", "sling:resourceType", "composum/pages/components/composed/table/cell", "text", "r2c1");
        row2Builder.resource("r2c2", "sling:resourceType", "composum/pages/components/composed/table/cell", "text", "r2c2");

        service.approximateMarkdown(table, printWriter);
        String expectedOutput = "#### Test Table\n" +
                "| r1c1 | r1c2 |  |\n" +
                "| r2c1 | r2c2 |  |\n" +
                "\n" +
                "\n";
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    public void testPageHandlingWithNonPageResource() {
        component = createMockResource("not/page", new HashMap<String, Object>());
        service.approximateMarkdown(component, printWriter);
        assertEquals("", writer.toString());
    }

    @Test
    public void testPageHandlingWithPageResource() {
        component = createMockResource("composum/pages/components/page", Map.of("jcr:title", "myPage",
                "jcr:description", "The best page!",
                "category", "test, dummy"));

        service.approximateMarkdown(component, printWriter);
        String expectedOutput = "Content of page /content/parent/path in markdown syntax starts now:\n" +
                "\n" +
                "\n" +
                "# myPage\n" +
                "The best page!\n" +
                "\n" +
                "\n" +
                "End of content of page /content/parent/path\n" +
                "\n";
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    public void testApproximateMarkdownForSuccess() {
        component = createMockResource("res", Map.of("text", "This is a test string.", "title", "This is a test heading"));
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
        Resource resource = context.create().resource("/content/parent/path/res", props);
        return resource;
    }

}
