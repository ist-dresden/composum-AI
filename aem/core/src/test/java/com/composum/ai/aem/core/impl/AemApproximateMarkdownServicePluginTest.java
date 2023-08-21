package com.composum.ai.aem.core.impl;

import static junitx.framework.Assert.assertEquals;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_TITLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
                when(chatCompletionService.htmlToMarkdown(anyString()))
                        .then(invocation -> invocation.getArgument(0));
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
                "The best page!\n";
        assertThat(writer.toString(), is(expectedOutput));
    }

    private Resource createMockResource(String resourceType, Map<String, Object> attributes) {
        Map<String, Object> props = new HashMap<>(attributes);
        props.put("sling:resourceType", resourceType);
        return context.create().resource("/content/parent/path/res", props);
    }

    private Resource createMockResource(String path, String json) {
        return context.load(true).json(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), path);
    }

    @Test
    public void testTeaser() {
        Resource teaser = createMockResource("/content/parent/path/res/jcr:content/teaser", "{\n" +
                "  \"cq:panelTitle\": \"Panel Downhill Skiing Wyoming\",\n" +
                "  \"fileReference\": \"/content/dam/wknd/en/adventures/downhill-skiing-wyoming/AdobeStock_185234795.jpeg\",\n" +
                "  \"jcr:description\": \"\\u003cp\\u003eA skiers paradise far from crowds and close to nature with terrain so vast it appears uncharted.\\u003c/p\\u003e\\r\\n\",\n" +
                "  \"jcr:primaryType\": \"nt:unstructured\",\n" +
                "  \"titleFromPage\": \"true\",\n" +
                "  \"descriptionFromPage\": \"true\",\n" +
                "  \"sling:resourceType\": \"core/wcm/components/teaser/v1/teaser\",\n" +
                "  \"actions\": {\n" +
                "    \"jcr:primaryType\": \"nt:unstructured\",\n" +
                "    \"item0\": {\n" +
                "      \"jcr:primaryType\": \"nt:unstructured\",\n" +
                "      \"link\": \"/content/wknd/us/en/adventures/downhill-skiing-wyoming\",\n" +
                "      \"text\": \"View Trip\"\n" +
                "    }\n" +
                "  }\n" +
                "}");
        Resource page = createMockResource("/content/wknd/us/en/adventures/downhill-skiing-wyoming", "{\n" +
                "  \"jcr:primaryType\": \"cq:Page\",\n" +
                "  \"jcr:content\": {\n" +
                "    \"jcr:description\": \"A skiers paradise far from crowds and close to nature with terrain so vast it appears uncharted.  With 2,500 acres of legendary terrain, unmatched levels of snowfall each winter, and unparalleled backcountry access, Jackson Hole offers a truly unique skiing experience.\",\n" +
                "    \"jcr:primaryType\": \"cq:PageContent\",\n" +
                "    \"jcr:title\": \"Downhill Skiing Wyoming\",\n" +
                "    \"sling:resourceType\": \"wknd/components/page\"\n" +
                "  }\n" +
                "}");
        service.approximateMarkdown(teaser, printWriter);
        String expectedOutput = "Downhill Skiing Wyoming\n" +
                "<p>A skiers paradise far from crowds and close to nature with terrain so vast it appears uncharted.</p>\n" +
                "[View Trip](/content/wknd/us/en/adventures/downhill-skiing-wyoming)\n" +
                "\n" +
                "A skiers paradise far from crowds and close to nature with terrain so vast it appears uncharted.  With 2,500 acres of legendary terrain, unmatched levels of snowfall each winter, and unparalleled backcountry access, Jackson Hole offers a truly unique skiing experience.\n";
        assertThat(writer.toString(), is(expectedOutput));
    }


    @Test
    public void testExperienceFragment() {
        context.create().resource("/content/experience-fragments/foo/master/jcr:content/root", ImmutableMap.of(JCR_TITLE, "thetitle"));

        component = createMockResource("core/wcm/components/experiencefragment/v1/experiencefragment",
                ImmutableMap.of("fragmentVariationPath", "/content/experience-fragments/foo/master"));

        service.approximateMarkdown(component, printWriter);
        String expectedOutput = "## thetitle\n\n";
        assertEquals(expectedOutput, writer.toString());
    }

}
