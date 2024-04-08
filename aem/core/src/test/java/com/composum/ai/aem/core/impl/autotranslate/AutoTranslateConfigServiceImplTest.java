package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_PREFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.LC_PREFIX;
import static com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigServiceImpl.isHeuristicallyTranslatableProperty;
import static com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigServiceImpl.isHtmlButNotRichtext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.wcm.testing.mock.aem.junit5.AemContext;

public class AutoTranslateConfigServiceImplTest {

    @Mock
    private AutoTranslateConfig config = Mockito.mock(AutoTranslateConfig.class);

    private AutoTranslateConfigServiceImpl service = new AutoTranslateConfigServiceImpl();

    @Test
    public void testTranslateableAttributes() {
        AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
        Resource res = context.create().resource("/foo", "sling:resourceType", "wknd/components/page", "heuristicAttr", "some texts", "allowedAttr", "7", "deniedAttr", "some texts");
        Resource sub = context.create().resource("/foo/bar", "subHeuristicAttr", "some texts", "subAllowedAttr", "7", "subDeniedAttr", "some texts");
        when(config.allowedAttributeRegexes()).thenReturn(
                new String[]{".*%allowedAttr", "wknd/components/page%bar/subAllowedAttr"});
        when(config.deniedAttributesRegexes()).thenReturn(
                new String[]{".*%deniedAttr", "wknd/components/page%bar/subDeniedAttr"});
        service.activate(config);
        List<String> translateable = service.translateableAttributes(res);
        Collections.sort(translateable);
        assertEquals(Arrays.asList("allowedAttr", "heuristicAttr"), translateable);
        translateable = service.translateableAttributes(sub);
        Collections.sort(translateable);
        assertEquals(Arrays.asList("subAllowedAttr", "subHeuristicAttr"), translateable);
    }


    @Test
    public void testIsTranslatableProperty() {
        // Test with certainly translatable properties
        for (String propertyName : AutoTranslateConfigServiceImpl.CERTAINLY_TRANSLATABLE_PROPERTIES) {
            assertTrue(isHeuristicallyTranslatableProperty(propertyName, "Some value"));
        }

        // Test with property name containing colon
        assertFalse(isHeuristicallyTranslatableProperty("property:name", "Some value"));

        // Test with non-string value
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", new Object()));

        // Test with string value starting with /content/, /apps/, /libs/, /mnt/
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/content/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/apps/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/libs/someValue"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "/mnt/someValue"));

        // Test with property name starting with AI_PREFIX or LC_PREFIX and ending with AI_TRANSLATED_SUFFIX or AI_ORIGINAL_SUFFIX
        assertFalse(isHeuristicallyTranslatableProperty(AI_PREFIX + "propertyName" + AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(AI_PREFIX + "propertyName" + AI_ORIGINAL_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(LC_PREFIX + "propertyName" + AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isHeuristicallyTranslatableProperty(LC_PREFIX + "propertyName" + AI_ORIGINAL_SUFFIX, "Some value"));

        // Test with string value without whitespace and 4 letter sequence
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "abc"));

        // Test with string value with whitespace but without 4 letter sequence
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "a b c"));

        // Test with string value with 4 letter sequence but without whitespace
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "abcd"));

        // Test with string value with whitespace and 4 letter sequence
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efghi"));


        // Test with string value with multiple whitespace sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efghi"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efgh ijklm"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh  ijklm"));

        // Test with string value with multiple whitespace sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efghi"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd efgh ijklm"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isHeuristicallyTranslatableProperty("propertyName", "abcd  efgh  ijklm"));

        // that's likely a boolean property
        assertFalse(isHeuristicallyTranslatableProperty("displayPopupTitle", "true"));
        assertFalse(isHeuristicallyTranslatableProperty("displayPopupTitle", "false"));

        // dates are not translateable
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "2022-01-01"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "2022-01-01T00:00:00.000Z"));
        assertFalse(isHeuristicallyTranslatableProperty("propertyName", "Mon Apr 08 09:43:43 CEST 2024"));

        // rather do not translate anything that looks like a boolean, even if it's a whitelisted property
        assertFalse(isHeuristicallyTranslatableProperty("shortDescription", "true"));
        assertFalse(isHeuristicallyTranslatableProperty("jcr:title", "false"));
    }

    @Test
    public void testIsHtmlButNotRichtext() {
        assertFalse(isHtmlButNotRichtext("propertyName", null));
        assertFalse(isHtmlButNotRichtext("propertyName", ""));
        assertFalse(isHtmlButNotRichtext("propertyName", "1"));
        assertFalse(isHtmlButNotRichtext("propertyName", 17));
        assertFalse(isHtmlButNotRichtext("propertyName", "hallo hallo test test"));
        assertFalse(isHtmlButNotRichtext("propertyName", "<p>hi!</p>"));
        assertFalse(isHtmlButNotRichtext("richtext", "&lt;ul>&lt;li>the &lt;em>Nodes&lt;/em> module enables the exploring and manipulation of the entire repository on a raw (JCR) level extended with Sling features&lt;/li>&lt;li>the &lt;em>Console Browser&lt;/em> provides a quick view to the Sling components and the component based content&lt;/li>&lt;li>the &lt;em>Nodes&lt;/em> module provides also a package management UI and service for content and code export and import via UI or HTTP&lt;/li>&lt;li>a simple user management and the management of the access control rules is also part of the &lt;em>Nodes&lt;/em> module&lt;/li>&lt;/ul>"));
        String someHTML = "<div class=\"composum-pages-components-element-teaser_variation_bg-image composum-pages-components-element-teaser\"\n" +
                "     style=\"background-image:url(/assets/background/synchlotron.jpg)\">\n" +
                "    <a href=\"/home/pages/editing.html\" class=\"composum-pages-components-element-teaser_link\" title=\"Content Pages\">\n" +
                "        <div class=\"composum-pages-components-element-teaser_content\">\n" +
                "            <div class=\"composum-pages-components-element-teaser_text-block\">\n" +
                "                <h1 class=\"composum-pages-components-element-teaser_title\">Content Pages</h1>\n" +
                "\n" +
                "                <div class=\"composum-pages-components-element-teaser_text\"><p>\n" +
                "                    <ul>\n" +
                "                        <li>various views to to navigate content structure</li>\n" +
                "                        <li>content editing using component related dialogs</li>\n" +
                "                        <li>add, remove or reorder elements in the tree, the page or in the context view</li>\n" +
                "                        <li>add component instances or assets via drag and drop from their selections views</li>\n" +
                "                    </ul>\n" +
                "                    </p></div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </a>\n" +
                "</div>\n" +
                "\n";
        assertTrue(isHtmlButNotRichtext("something", someHTML));
        assertTrue(isHtmlButNotRichtext("something", someHTML.replaceAll("<", "&lt;")));
        assertTrue(isHtmlButNotRichtext("something", StringEscapeUtils.escapeHtml(someHTML)));
    }

}
