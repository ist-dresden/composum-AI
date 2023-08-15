package com.composum.ai.aem.core.impl;

import static junitx.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.impl.ApproximateMarkdownServiceImpl;
import com.google.common.collect.ImmutableMap;

import io.wcm.testing.mock.aem.junit5.AemContext;

/**
 * Like ApproximateMarkdownServiceImplTest but with Composum specific stuff.
 */
public class AemApproximateMarkdownServicePluginTest {

    private ApproximateMarkdownServiceImpl service;
    private Resource component;
    private StringWriter writer;
    private PrintWriter printWriter;

    private AemContext context;

    @BeforeEach
    public void setUp() {
        context = new AemContext(ResourceResolverType.JCR_MOCK);
        service = new ApproximateMarkdownServiceImpl() {
            {
                chatCompletionService = mock(GPTChatCompletionService.class);
                plugins = Collections.singletonList(new AemApproximateMarkdownServicePlugin());
            }
        };
        writer = new StringWriter();
        printWriter = new PrintWriter(writer);
    }

    @Test
    public void testPageHandlingWithNonPageResource() {
        component = createMockResource("not/page", new HashMap<>());
        service.approximateMarkdown(component, printWriter);
        assertEquals("", writer.toString());
    }

    @Test
    public void testPageHandlingWithPageResource() {
        component = createMockResource("cq:PageContent",
                ImmutableMap.of("jcr:title", "myPage",
                        "jcr:description", "The best page!",
                        "category", "test, dummy"));

        service.approximateMarkdown(component, printWriter);
        String expectedOutput = "Content of page /content/parent/path in markdown syntax starts now:\n" +
                "\n" +
                "\n" +
                "# myPage\n\n" +
                "The best page!\n\n\n";
        assertThat(writer.toString(), is(expectedOutput));
    }

    private Resource createMockResource(String resourceType, Map<String, Object> attributes) {
        Map<String, Object> props = new HashMap<>(attributes);
        props.put("sling:resourceType", resourceType);
        return context.create().resource("/content/parent/path/res", props);
    }

}
