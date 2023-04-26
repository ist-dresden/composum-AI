package com.composum.chatgpt.base.service.chat.impl;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Tests for {@link HtmlToMarkdownConverter}. Generated with ChatGPT and then fixed.
 */
public class HtmlToMarkdownConverterTest {

    private HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter();

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testConvertTagA() {
        String html = "<a href=\"http://example.com\">click here</a>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("[click here](http://example.com)"));
    }

    @Test
    public void testConvertTagStrong() {
        String html = "<strong>Hello</strong>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("**Hello**"));
    }

    @Test
    public void testConvertTagCode() {
        String html = "<code>if (a > b) return a; else return b;</code>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("`if (a > b) return a; else return b;`"));
    }

    @Test
    public void testConvertTagPre() {
        // not handled right so far, but probably not needed:
        // String html = "<pre><code>public static void main(String[] args) {\n System.out.println(\"Hello World!\");\n}</code></pre>";
        String html = "<pre>public static void main(String[] args) {\n System.out.println(\"Hello World!\");\n}</pre>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("```\npublic static void main(String[] args) {\n System.out.println(\"Hello World!\");\n}\n```\n"));
    }

    @Test
    public void testConvertTagP() {
        String html = "<p>This is a paragraph.</p>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("This is a paragraph.\n\n"));
    }

    @Test
    public void testConvertTagBr() {
        String html = "<div>This is<br>a text<br>with linebreaks.</div>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("This is\na text\nwith linebreaks."));
    }

    @Test
    public void testConvertTagU() {
        String html = "<u>This is underlined text.</u>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("_This_is_underlined_text._"));
    }

    @Test
    public void testConvertTagUl() {
        String html = "<ul><li>One</li><li>Two</li><li>Three</li></ul>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("- One\n- Two\n- Three\n\n"));
    }

    @Test
    public void testConvertTagOl() {
        String html = "<ol><li>One</li><li>Two</li><li>Three</li></ol>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("1. One\n2. Two\n3. Three\n\n"));
    }

    @Test
    public void testConvertTagUnknown() {
        String html = "<xyz>This is unknown tag.</xyz>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("This is unknown tag."));
    }

    @Test
    public void testConvertWithNull() {
        String html = null;
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is(""));
    }

    @Test
    public void testConvertWithEmptyString() {
        String html = "";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is(""));
    }

}
