package com.composum.chatgpt.bundle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.sling.core.util.ResourceUtil;

/**
 * Implementation for {@link ApproximateMarkdownService}.
 */
@Component
public class ApproximateMarkdownServiceImpl implements ApproximateMarkdownService {

    public static final Map<String, String> ATTRIBUTE_TO_MARKDOWN_PREFIX = Map.of(
            "jcr:title", "## ",
            "title", "## ",
            "subtitle", "### "
            // , "code", "```" handled in extra method
    );
    public static final List<String> TEXT_ATTRIBUTES = List.of(
            "jcr:title", "title", "subtitle", "linkTitle", "jcr:description", "text",
            /* "code", */ "copyright", // code component; code is handled in extra method
            "defaultValue", "exampleCode", "suffix", "exampleResult", "footer" // for servlet component
    );
    private static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServiceImpl.class);
    /**
     * Pattern that matches an opening html tag and captures the tag name.
     */
    protected Pattern PATTERN_HTML_TAG = Pattern.compile("<\\s*(\\w+)(\\s+[^>]*)?>");
    @Reference
    protected GPTChatCompletionService chatCompletionService;
    protected Set<String> htmltags = new HashSet<>();

    protected static void logUnhandledAttributes(Resource resource) {
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (!TEXT_ATTRIBUTES.contains(value) && value.matches(".*\\s+.*\\s+.*\\s+.*")) {
                    // check whether we forgot something
                    LOG.info("Ignoring text attribute {} in {}", entry.getKey(), resource.getPath());
                }
            }
        }
    }

    @Nonnull
    @Override
    public String approximateMarkdown(@Nullable Resource resource) {
        try (StringWriter s = new StringWriter();
             PrintWriter p = new PrintWriter(s)) {
            approximateMarkdown(resource, p);
            p.close();
            return s.toString();
        } catch (IOException e) {
            // pretty much impossible for a StringWriter , no sensible handling.
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void approximateMarkdown(@Nullable Resource resource, @Nonnull PrintWriter out) {
        if (resource == null || resource.getName().equals("i18n")) {
            // The content of i18n nodes would be a duplication as it was already printed as "text" attribute in the parent node.
            return;
        }
        if (!ResourceUtil.normalize(resource.getPath()).startsWith("/content/")) {
            throw new IllegalArgumentException("For security reasons the resource must be in /content but is: " + resource.getPath());
        }
        boolean wasHandledAsPage = pageHandling(resource, out);
        boolean wasHandledAsTable = tableHandling(resource, out);
        boolean printEmptyLine = false;
        if (!wasHandledAsPage && !wasHandledAsTable) {
            handleCodeblock(resource, out);
            for (String attributename : TEXT_ATTRIBUTES) {
                String value = resource.getValueMap().get(attributename, String.class);
                if (value != null) {
                    String prefix = ATTRIBUTE_TO_MARKDOWN_PREFIX.getOrDefault(attributename, "");
                    String markdown = getMarkdown(value);
                    out.println(prefix + markdown);
                    printEmptyLine = true;
                }
            }
        }
        if (wasHandledAsPage || wasHandledAsTable || printEmptyLine) {
            out.println();
        }
        logUnhandledAttributes(resource);
        // the table method handles it's expected children, but the other methods do not.
        if (!wasHandledAsTable) {
            resource.getChildren().forEach(child -> approximateMarkdown(child, out));
        }
        if (wasHandledAsPage) {
            String path = resource.getParent().getPath(); // we don't want the content node's path but the parent's
            out.println("\nEnd of content of page " + path + "\n");
        }
    }

    @Nonnull
    protected String getMarkdown(@Nullable String value) {
        String markdown;
        if (value == null) {
            markdown = "";
        } else if (PATTERN_HTML_TAG.matcher(value).find()) {
            markdown = chatCompletionService.htmlToMarkdown(value).trim();
        } else {
            markdown = value.trim();
        }
        return markdown;
    }

    protected boolean pageHandling(Resource resource, PrintWriter out) {
        boolean isPage = ResourceUtil.isResourceType(resource, "composum/pages/components/page");
        if (isPage) {
            String path = resource.getParent().getPath(); // we don't want the content node's path but the parent's
            out.println("Content of page " + path + " in markdown syntax starts now:\n\n");

            String title = resource.getValueMap().get("jcr:title", String.class);
            String description = resource.getValueMap().get("jcr:description", String.class);
            List<String> categories = resource.getValueMap().get("category", List.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + getMarkdown(title));
            }
            if (categories != null && !categories.isEmpty()) {
                out.println("Categories: " + categories.stream().collect(Collectors.joining(", ")));
            }
            if (StringUtils.isNotBlank(description)) {
                out.println(getMarkdown(description));
            }
        }
        return isPage;
    }

    protected void handleCodeblock(Resource resource, PrintWriter out) {
        String code = resource.getValueMap().get("code", String.class);
        if (StringUtils.isNotBlank(code)) {
            out.println("```\n");
            out.println(code.trim());
            out.println("\n```\n");
        }
    }

    // debugging code; remove after it works.

    protected boolean tableHandling(Resource resource, PrintWriter out) {
        boolean isTable = ResourceUtil.isResourceType(resource, "composum/pages/components/composed/table");
        if (isTable) {
            String title = resource.getValueMap().get("title", String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("#### " + getMarkdown(title));
            }
            // for each child of type "row" we print a line with the values of the children of type "cell"
            StreamSupport.stream(resource.getChildren().spliterator(), true)
                    .filter(row -> ResourceUtil.isResourceType(row, "composum/pages/components/composed/table/row"))
                    .forEach(row -> {
                        out.print("| ");
                        StreamSupport.stream(row.getChildren().spliterator(), true)
                                .filter(cell -> ResourceUtil.isResourceType(cell, "composum/pages/components/composed/table/cell"))
                                .map(cell -> cell.getValueMap().get("text", String.class))
                                .forEach(text -> out.print(getMarkdown(text) + " | "));
                        out.println(" |");
                    });
            out.println();
        }
        return isTable;
    }

    /**
     * This is debugging code we needed to gather information for the implementation; we keep it around for now.
     * out.println("Approximated markdown for " + path);
     * traverseTreeForStructureGathering(resource, out, null, null);
     * out.println("DONE");
     * out.println("HTML tags found:" + htmltags);
     */
    protected void traverseTreeForStructureGathering(Resource resource, PrintWriter out, String outerResourceType, String subpath) {
        String resourceType = resource.getValueMap().get("sling:resourceType", String.class);
        final String resourceTypeForChildren = resourceType != null ? resourceType : outerResourceType;
        final String pathForChildren = resourceType != null ? "" : subpath + resource.getName();
        final String subpathForAttributes = resourceType != null ? "" : subpath;
        // iterate over all attributes of map resource.getValueMap() and write them to out if they contain several spaces.
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (value.matches(".*\\s+.*\\s+.*\\s+.*")) {
                    out.println(resource.getPath() + " [" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey() + ": " + value);
                    // out.println("[" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey() + ": " + value);
                    // out.println("[" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey());
                    // out.println(entry.getKey() + " [" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey());
                    captureHtmlTags(value);
                }
            }
        }
        // iterate over child resources: call traverseTree with the child resource and the resourceTypeForChildren
        // and pathForChildren.
        resource.getChildren().forEach(child -> traverseTreeForStructureGathering(child, out, resourceTypeForChildren, pathForChildren + "/"));
    }

    protected void captureHtmlTags(String value) {
        PATTERN_HTML_TAG.matcher(value).results()
                .map(matchResult -> matchResult.group(1))
                .forEach(htmltags::add);
        // -> found: [ext, a, sly, strong, code, em, language, type, p, br, div, path, u, ul, attributes, li, ol]
    }

}
