package com.composum.ai.aem.ui.apps.dialogcreator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * An attempt at a DSL to create Granite UI Foundation Server-side dialogs, but not a very successful one. That's not really better than just writing the XML directly, and Java isn't really the best language for this.
 *
 * @see "https://developer.adobe.com/experience-manager/reference-materials/6-5/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/server.html"
 */
public class DialogDSL {

    StringBuilder sb = new StringBuilder();

    String indent = "";

    Runnable empty = () -> {
    };

    void node(String nodename, String attributes, Runnable content) {
        sb.append(indent).append("<").append(nodename).append(" ").append(attributes).append(">\n");
        String oldindent = indent;
        indent += "    ";
        content.run();
        indent = oldindent;
        sb.append(indent).append("</").append(nodename).append(">\n");
    }

    /**
     * @param attributes a list that is attribute 1 name, attribute 1 value, attribute 2 name, attribute 2 value, ...
     */
    void node(String nodename, List<String> attributes, Runnable content) {
        StringBuilder attrs = new StringBuilder();
        for (int i = 0; i < attributes.size(); i += 2) {
            attrs.append(" ").append(attributes.get(i)).append("=\"").append(attributes.get(i + 1)).append("\"");
        }
        node(nodename, attrs.toString(), content);
    }

    void unstructured(String nodename, String resourceType, List<String> attributes, Runnable content) {
        List<String> allattributes = new ArrayList<>();
        allattributes.addAll(asList("jcr:primaryType", "nt:unstructured", "sling:resourceType", resourceType));
        allattributes.addAll(attributes);
        node(nodename, allattributes, content);
    }

    void unstructured(String nodename, String resourceType, Runnable content) {
        unstructured(nodename, resourceType, emptyList(), content);
    }

    void unstructured(String nodename, Runnable content) {
        unstructured(nodename, "nt:unstructured", content);
    }

    void unstructured(String nodename, List<String> attributes, Runnable content) {
        unstructured(nodename, "nt:unstructured", attributes, content);
    }

    void foundation(String nodename, String foundationType, List<String> otherattributes, Runnable content) {
        unstructured(nodename, "granite/ui/components/coral/foundation/" + foundationType, otherattributes, content);
    }

    void foundation(String nodename, String foundationType, Runnable content) {
        foundation(nodename, foundationType, emptyList(), content);
    }

    void dialog(String dialogname, Runnable content) {
        unstructured("jcr:root", "cq/gui/components/authoring/dialog", asList(
                "xmlns:sling", "http://sling.apache.org/jcr/sling/1.0",
                "xmlns:jcr", "http://www.jcp.org/jcr/1.0",
                "xmlns:nt", "http://www.jcp.org/jcr/nt/1.0",
                "xmlns:cq", "http://www.day.com/jcr/cq/1.0",
                "jcr:title", dialogname, "mode", "edit"), content);
    }

    private void createDialog(String dialogname, String filename, Runnable content) throws IOException {
        dialog(dialogname, content);
        System.out.println(sb.toString());
        Path path = FileSystems.getDefault().getPath(filename).toAbsolutePath();
        System.out.println(path);
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void main(String[] args) throws IOException {
        new DialogDSL().createCreationDialog();
    }

    private void createCreationDialog() throws IOException {
        createDialog("ChatGPT Content Creation", "src/test/content/jcr_root/apps/composum-ai/test/components/contentcreation/_cq_dialog/.content.new.xml",
                () -> foundation("content", "container",
                        () -> unstructured("items",
                                () -> {
                                    // Actual dialog content starts here
                                    foundation("promptGroup", "fieldset", asList("jcr:title", "Prompt Group"),
                                            () -> unstructured("items",
                                                    () -> {
                                                        foundation("promptDetails", "fieldset", asList("jcr:title", "Prompt Details", "layout", "horizontal"),
                                                                () -> unstructured("items",
                                                                        () -> {
                                                                            foundation("predefinedPrompts", "form/select", asList("fieldLabel", "Predefined Prompts", "name", "./predefinedPrompts"),
                                                                                    () -> {
                                                                                        unstructured("summary", asList("text", "Summary", "value", "summary"), empty);
                                                                                        unstructured("improve", asList("text", "Improve", "value", "improve"), empty);
                                                                                        unstructured("extend", asList("text", "Extend", "value", "extend"), empty);
                                                                                        unstructured("titleGeneration", asList("text", "Title Generation", "value", "titleGeneration"), empty);
                                                                                    }
                                                                            );
                                                                            foundation("contentSelector", "form/select", asList("fieldLabel", "Content Selector", "name", "./contentSelector"),
                                                                                    () -> {
                                                                                        // Options for content selector can be added here
                                                                                    }
                                                                            );
                                                                            foundation("textLengthSelector", "form/select", asList("fieldLabel", "Text Length Selector", "name", "./textLengthSelector"),
                                                                                    () -> {
                                                                                        unstructured("oneLine", asList("text", "One Line", "value", "oneLine"), empty);
                                                                                        unstructured("oneSentence", asList("text", "One Sentence", "value", "oneSentence"), empty);
                                                                                        unstructured("oneParagraph", asList("text", "One Paragraph", "value", "oneParagraph"), empty);
                                                                                        unstructured("severalParagraphs", asList("text", "Several Paragraphs", "value", "severalParagraphs"), empty);
                                                                                    }
                                                                            );
                                                                        }
                                                                )
                                                        );
                                                        foundation("promptArea", "form/textarea", asList("fieldLabel", "Prompt Area", "name", "./promptArea", "rows", "5"), empty);
                                                    }
                                            )
                                    );
                                    // Other dialog elements like Generation Control, Content Preview, etc. can be added in a similar manner below.
                                    // Actual dialog content ends here
                                }
                        )
                )
        );
    }

}
