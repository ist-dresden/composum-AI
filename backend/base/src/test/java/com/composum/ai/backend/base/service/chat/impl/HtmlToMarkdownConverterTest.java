package com.composum.ai.backend.base.service.chat.impl;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Ignore;
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
        ec.checkThat(markdown, is("\n```\npublic static void main(String[] args) {\n System.out.println(\"Hello World!\");\n}\n```\n"));
    }

    @Test
    public void testConvertTagP() {
        String html = "<p>This is a paragraph.</p>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\nThis is a paragraph.\n"));
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
        ec.checkThat(markdown, is("_This is underlined text._"));
    }

    @Test
    public void testConvertTagUl() {
        String html = "<ul><li>One</li><li>Two</li><li>Three</li></ul>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n- One\n- Two\n- Three\n"));
    }

    @Test
    public void testConvertTagOl() {
        String html = "<ol><li>One</li><li>Two</li><li>Three</li></ol>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n1. One\n2. Two\n3. Three\n"));
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

    @Test
    public void testConvertTagEm() {
        String html = "<em>This is emphasized text.</em>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("_This is emphasized text._"));
    }

    @Test
    public void testConvertTagI() {
        String html = "<i>This is italic text.</i>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("*This is italic text.*"));
    }

    @Test
    public void testConvertTagDel() {
        String html = "<del>This is deleted text.</del>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("~~This is deleted text.~~"));
    }

    @Test
    public void testConvertTagS() {
        String html = "<s>This is strikethrough text.</s>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("~~This is strikethrough text.~~"));
    }

    @Test
    public void testConvertTagMixedSerialB_S_I() {
        String html = "This is <b>bold</b> <i>emphasized</i> text i<b>n</b>n<em>e</em>rwords";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("This is **bold** *emphasized* text i**n**n_e_rwords"));
    }

    @Test
    public void testConvertTagImg() {
        String html = "<img src=\"http://example.com/image.jpg\" alt=\"An example image\">";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("![An example image](http://example.com/image.jpg)"));
    }

    @Test
    public void testConvertTagH1() {
        String html = "<h1>This is a heading 1</h1>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("# This is a heading 1\n"));
    }

    @Test
    public void testConvertTagH2() {
        String html = "<h2>This is a heading 2</h2>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("## This is a heading 2\n"));
    }

    @Test
    public void testConvertTagH3() {
        String html = "<h3>This is a heading 3</h3>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("### This is a heading 3\n"));
    }

    @Test
    public void testConvertTagH4() {
        String html = "<h4>This is a heading 4</h4>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("#### This is a heading 4\n"));
    }

    @Test
    public void testConvertTagH5() {
        String html = "<h5>This is a heading 5</h5>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("##### This is a heading 5\n"));
    }

    @Test
    public void testConvertTagH6() {
        String html = "<h6>This is a heading 6</h6>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("###### This is a heading 6\n"));
    }

    @Test
    public void testConvertTagHr() {
        String html = "<hr>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n---\n"));
    }

    @Test
    public void testConvertTagInput() {
        String html = "<input type=\"text\" placeholder=\"Enter text\">";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("[Input: Type=text, Placeholder=Enter text]"));
    }

    @Test
    public void testConvertTagDlDtDd() {
        String html = "<dl><dt>Term 1</dt><dd>Definition 1</dd><dt>Term 2</dt><dd>Definition 2</dd></dl>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n<dl>\n  <dt>Term 1</dt>\n  <dd>Definition 1</dd>\n  <dt>Term 2</dt>\n  <dd>Definition 2</dd>\n</dl>\n"));
    }

    @Test
    public void testConvertTagBlockquote() {
        String html = "<blockquote>This is a blockquote.\nAnother line</blockquote>";
        String markdown = converter.convert(html);
        // unfortunately jsoup joins the lines.
        ec.checkThat(markdown, is("\n> This is a blockquote. Another line\n"));
    }

    @Test
    public void testConvertTagNesting() {
        String html = "<blockquote>This<blockquote>is a</blockquote>blockquote.<ul><li>one</li><li>two</li></ul></blockquote>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n" +
                "> This\n" +
                "> > is a\n" +
                "> blockquote.\n" +
                "> - one\n" +
                "> - two\n" +
                "\n"));
    }

    @Test
    public void testConvertTagListNesting() {
        String html = "<ol><li><ul><li>one</li><li>two</li></ul></li><li>three</li></ol>";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("\n" +
                "1. \n" +
                "   - one\n" +
                "   - two\n" +
                "\n" + // that is unfortunate but hard to avoid and doesn't seem to really hurt.
                "2. three\n"));
    }

    @Test
    public void testConvertTagMark() {
        String html = "<mark>This is marked text.</mark> and<mark>this</mark>too.";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("<mark>This is marked text.</mark> and<mark>this</mark>too."));
    }

    @Test
    public void testConvertTagSmall() {
        String html = "<small>This is small text.</small> and<small>this</small>too.";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("<small>This is small text.</small> and<small>this</small>too."));
    }

    @Test
    public void testConvertTagIns() {
        String html = "<ins>This is inserted text.</ins> and<ins>this</ins>too.";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("<ins>This is inserted text.</ins> and<ins>this</ins>too."));
    }

    @Test
    public void testConvertTagSub() {
        String html = "<sub>This is subscript text.</sub> and<sub>this</sub>too.";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("<sub>This is subscript text.</sub> and<sub>this</sub>too."));
    }

    @Test
    public void testConvertTagSup() {
        String html = "<sup>This is superscript text.</sup> and<sup>this</sup>too.";
        String markdown = converter.convert(html);
        ec.checkThat(markdown, is("<sup>This is superscript text.</sup> and<sup>this</sup>too."));
    }

    @Test
    public void testEndsWith() {
        converter.sb.setLength(0);
        converter.ensureEmptyOrEndsWith("");
        ec.checkThat(converter.sb.toString(), is(""));

        converter.sb.setLength(0);
        converter.sb.append("x");
        converter.ensureEmptyOrEndsWith("a");
        ec.checkThat(converter.sb.toString(), is("xa"));
        converter.ensureEmptyOrEndsWith("a");
        ec.checkThat(converter.sb.toString(), is("xa"));

        converter.sb.setLength(0);
        converter.sb.append("x");
        converter.ensureEmptyOrEndsWith("ab");
        ec.checkThat(converter.sb.toString(), is("xab"));
        converter.ensureEmptyOrEndsWith("ab");
        ec.checkThat(converter.sb.toString(), is("xab"));

        converter.sb.setLength(0);
        converter.sb.append("x");
        converter.sb.append("ab");
        converter.ensureEmptyOrEndsWith("abcd");
        ec.checkThat(converter.sb.toString(), is("xabcd"));
    }


}
