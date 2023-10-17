package com.composum.ai.backend.base.service.chat.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A quick HTML markdown converter that handles the tags [a, strong, code, em, p, br, u, ul, li, ol] used in rich text editor.
 * Not threadsafe, use only once.
 * <p>
 * We do not want to use a library since the libraries doing this have many parts and are quite some work to deploy, and
 * we only need to convert a few tags from richtext editors.
 * Original generated by ChatGPT with "Please make an HTML to Markdown converter that handles the tags [a, strong, code, em, p, br, u, ul, li, ol] . Use the jsoup library for that."
 * but some heavy rewrite.
 */
public class HtmlToMarkdownConverter {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlToMarkdownConverter.class);

    private static Set<String> missingTags = new ConcurrentSkipListSet<>();

    private static final Map<String, String> HEADER_TAGS = Map.of("h1", "# ", "h2", "## ", "h3",
            "### ", "h4", "#### ", "h5", "##### ", "h6", "###### ");

    // continued indentation. Two spaces since four would be code block
    private final String indentStep = "  ";

    // continued indentation that is inserted before a continuation line
    private String continuedIndentation = "";

    private StringBuilder sb = new StringBuilder();

    @Nonnull
    public String convert(@Nullable String html) {
        if (html != null) {
            Document doc = Jsoup.parseBodyFragment(html);
            convertElement(doc.body());
        }
        return sb.toString();
    }

    private void convertNode(Node node) {
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            insertText(textNode.text());
        } else if (node instanceof Element) {
            Element element = (Element) node;
            convertElement(element);
        } else {
            throw new UnsupportedOperationException("Unknown node type " + node.getClass());
        }
    }

    /**
     * Split text into lines to add indentation before each line.
     */
    protected void insertText(String text) {
        if (text != null) {
            String splitText = text.lines()
                    .collect(Collectors.joining(continuedIndentation + "\n"));
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                // only happens if we have mixed text and block level elements within a block level element
                sb.append(continuedIndentation);
            }
            sb.append(splitText);
        }
    }

    protected void convertChildren(Node node) {
        for (Node child : node.childNodes()) {
            convertNode(child);
        }
    }

    /**
     * Convention: a block level element has to print a newline before itself, also after itself.
     */
    private void convertElement(Element element) {
        String tagName = element.tagName().toLowerCase();
        String oldindentation;
        switch (tagName) {
            case "a":
                sb.append("[");
                convertChildren(element);
                sb.append("](");
                sb.append(element.attr("href"));
                sb.append(")");
                break;

            case "em":
            case "u":
                sb.append("_");
                convertChildren(element);
                sb.append("_");
                break;

            case "b":
            case "strong":
                sb.append("**");
                convertChildren(element);
                sb.append("**");
                break;

            case "i":
                sb.append("*");
                convertChildren(element);
                sb.append("*");
                break;

            case "del":
            case "s":
                sb.append("~~");
                convertChildren(element);
                sb.append("~~");
                break;

            case "code":
                sb.append("`");
                convertChildren(element);
                sb.append("`");
                break;

            case "pre": // TODO: a pre code nesting would be wrong.
                sb.append("\n```\n");
                sb.append(element.html().replaceAll("\\s+$", ""));
                sb.append("\n```\n");
                break;

            case "p":
                sb.append("\n");
                convertChildren(element);
                sb.append("\n");
                break;

            case "br":
                sb.append("\n");
                break;

            case "ul":
                oldindentation = continuedIndentation;
                continuedIndentation += indentStep;
                for (Element li : element.children()) {
                    sb.append("\n" + oldindentation + "- ");
                    convertChildren(li);
                }
                sb.append("\n");
                continuedIndentation = oldindentation;
                break;

            case "ol":
                oldindentation = continuedIndentation;
                int i = 1;
                for (Element li : element.children()) {
                    String prefix = (i++) + ". ";
                    continuedIndentation = oldindentation + prefix.replaceAll(".", " ");
                    sb.append("\n" + oldindentation + prefix);
                    convertChildren(li);
                }
                sb.append("\n");
                continuedIndentation = oldindentation;
                break;

            case "li":
                throw new UnsupportedOperationException("Bug: li outside of ul or ol");

            case "img":
                sb.append("![");
                sb.append(element.attr("alt"));
                sb.append("](");
                sb.append(element.attr("src"));
                sb.append(")");
                break;

            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                String prefix = HEADER_TAGS.get(tagName);
                sb.append(prefix);
                convertChildren(element);
                sb.append("\n");
                break;

            case "hr":
                sb.append("\n");
                sb.append(continuedIndentation);
                sb.append("---\n");
                break;

            case "input":
                String type = element.attr("type");
                String placeholder = element.attr("placeholder");
                sb.append("[Input: Type=");
                sb.append(type.isEmpty() ? "text" : type);
                if (!placeholder.isEmpty()) {
                    sb.append(", Placeholder=");
                    sb.append(placeholder);
                }
                sb.append("]");
                break;

            case "dl": // there is no markdown for dl, so we just use embedded HTML. Possibly it could also be
                // **term**
                // definition
                //
                oldindentation = continuedIndentation;
                continuedIndentation += indentStep;
                sb.append("\n" + oldindentation + "<dl>");
                convertChildren(element);
                sb.append("\n" + oldindentation + "</dl>\n");
                continuedIndentation = oldindentation;
                break;

            case "dt":
                sb.append("\n" + continuedIndentation + "<dt>");
                convertChildren(element);
                sb.append("</dt>");
                break;

            case "dd":
                sb.append("\n" + continuedIndentation + "<dd>");
                convertChildren(element);
                sb.append("</dd>");
                break;

            case "blockquote":
                oldindentation = continuedIndentation;
                continuedIndentation += "> ";
                sb.append("\n");
                sb.append(continuedIndentation);
                convertChildren(element);
                sb.append("\n");
                continuedIndentation = oldindentation;
                break;

            case "mark":
            case "small":
            case "ins":
            case "sub":
            case "sup":
                // use HTML syntax
                sb.append("<").append(tagName).append(">");
                convertChildren(element);
                sb.append("</").append(tagName).append(">");
                break;

            case "#root":
            case "html":
            case "body":
            case "span":
            case "div":
                // ignore the tag
                convertChildren(element);
                break;

            case "noscript":
            case "meta":
            case "nav":
                // ignore the content, too
                break;

            default:
                // ignore tags we do not know
                LOG.warn("Unknown tag {}", tagName);
                missingTags.add(tagName);
                LOG.warn("Currently unsupported tags: {}", missingTags);
                convertChildren(element);
                break;
        }
    }
}
