package com.composum.ai.composum.bundle.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.impl.ApproximateMarkdownServiceImpl;

/**
 * Like ApproximateMarkdownServiceImplTest but with Composum specific stuff.
 */
public class ComposumApproximateMarkdownServicePluginTest {

    private ApproximateMarkdownServiceImpl service;
    private Resource component;
    private StringWriter writer;
    private PrintWriter printWriter;

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() {
        service = new ApproximateMarkdownServiceImpl() {
            {
                chatCompletionService = mock(GPTChatCompletionService.class);
                plugins = Collections.singletonList(new ComposumApproximateMarkdownServicePlugin());
            }
        };
        writer = new StringWriter();
        printWriter = new PrintWriter(writer);
    }

    @Test
    public void testTableHandlingWithNonTableResource() {
        component = createMockResource("not/table", new HashMap<>());
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
        String expectedOutput = "#### Test Table\n\n" +
                "| r1c1 | r1c2 |  |\n" +
                "| r2c1 | r2c2 |  |\n" +
                "\n";
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    public void testPageHandlingWithNonPageResource() {
        component = createMockResource("not/page", new HashMap<>());
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
                "# myPage\n\n" +
                "The best page!\n";
        assertEquals(expectedOutput, writer.toString());
    }

    private Resource createMockResource(String resourceType, Map<String, Object> attributes) {
        Map<String, Object> props = new HashMap<>(attributes);
        props.put("sling:resourceType", resourceType);
        return context.create().resource("/content/parent/path/res", props);
    }

}
