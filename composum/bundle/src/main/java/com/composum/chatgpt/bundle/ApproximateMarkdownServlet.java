package com.composum.chatgpt.bundle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.util.ResourceUtil;

/**
 * Renders an approximate markdown representation of the text content of a page / resource.
 * The Sling way would be to create markdown.jsp for each component, but that would be quite an effort with all existing
 * Pages components, and since the markdown representation is only for retrieving text for suggesting keywords and
 * summarizing, keywording etc. we just go with a simple approach for now, which just might be good enough.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum ChatGPT Approximated Markdown Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/platform/chatgpt/approximated.markdown",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
// curl -u admin:admin http://localhost:9090/bin/cpm/platform/chatgpt/approximated.markdown.md/content/ist/composum/home
public class ApproximateMarkdownServlet extends SlingSafeMethodsServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServlet.class);

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        RequestPathInfo info = request.getRequestPathInfo();
        String path = info.getSuffix();
        Resource resource = request.getResourceResolver().getResource(path);
        response.setContentType("text/plain");
        try (Writer w = response.getWriter();
             PrintWriter out = new PrintWriter(w)) {
            out.println("Page content in markdown syntax for " + path + ":\n");
            traverseTreeAndPrintMarkdown(resource, out);
            out.println("Attributes with HTML:");
            out.println(htmltags);
        }
    }

    public static final Map<String, String> ATTRIBUTE_TO_MARKDOWN_PREFIX = Map.of(
            "jcr:title", "## ",
            "title", "## ",
            "subtitle", "### "
            // , "code", "```"
    );

    public static final List<String> TEXT_ATTRIBUTES = List.of(
            "jcr:title", "title", "subtitle", "linkTitle", "jcr:description", "text",
            /* "code", */ "copyright", // code component. code is handled separately to create the markdown code block
            "defaultValue", "exampleCode", "suffix", "exampleResult", "footer" // for servlet component
    );

    /**
     * This is debugging code we needed to gather information for the implementation; we keep it around for now.
     * out.println("Approximated markdown for " + path);
     * traverseTreeForStructureGathering(resource, out, null, null);
     * out.println("DONE");
     * out.println("HTML tags found:" + htmltags);
     */
    protected void traverseTreeAndPrintMarkdown(Resource resource, PrintWriter out) {
        if (resource.getName().equals("i18n")) {
            // The content of i18n nodes would be a duplication as it was already printed as "text" attribute in the parent node.
            return;
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
                    out.println(prefix + value);
                    if (htmltagpattern.matcher(value).find()) {
                        htmltags.add(attributename);
                    }
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
            resource.getChildren().forEach(child -> traverseTreeAndPrintMarkdown(child, out));
        }
    }

    protected boolean pageHandling(Resource resource, PrintWriter out) {
        boolean isPage = ResourceUtil.isResourceType(resource, "composum/pages/components/page");
        if (isPage) {
            String title = resource.getValueMap().get("jcr:title", String.class);
            String description = resource.getValueMap().get("jcr:description", String.class);
            List<String> categories = resource.getValueMap().get("category", List.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + title);
            }
            if (categories != null && !categories.isEmpty()) {
                out.println("Categories: " + categories.stream().collect(Collectors.joining(", ")));
            }
            if (StringUtils.isNotBlank(description)) {
                out.println(description);
            }
            out.println();
        }
        return isPage;
    }

    protected void handleCodeblock(Resource resource, PrintWriter out) {
        String code = resource.getValueMap().get("code", String.class);
        if (StringUtils.isNotBlank(code)) {
            out.println("```");
            out.println(code);
            out.println("```");
        }
    }

    protected boolean tableHandling(Resource resource, PrintWriter out) {
        boolean isTable = ResourceUtil.isResourceType(resource, "composum/pages/components/composed/table");
        if (isTable) {
            String title = resource.getValueMap().get("title", String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("#### " + title);
            }
            // for each child of type "row" we print a line with the values of the children of type "cell"
            StreamSupport.stream(resource.getChildren().spliterator(), true)
                    .filter(row -> ResourceUtil.isResourceType(row, "composum/pages/components/composed/table/row"))
                    .forEach(row -> {
                        out.print("| ");
                        StreamSupport.stream(row.getChildren().spliterator(), true)
                                .filter(cell -> ResourceUtil.isResourceType(cell, "composum/pages/components/composed/table/cell"))
                                .map(cell -> cell.getValueMap().get("text", String.class))
                                .forEach(text -> out.print(StringUtils.trimToEmpty(text) + " | "));
                        out.println(" |");
                    });
            out.println();
        }
        return isTable;
    }

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

    // debugging code; remove after it works.

    protected Set<String> htmltags = new HashSet<>();

    /**
     * Pattern that matches an opening html tag and captures the tag name.
     */
    protected Pattern htmltagpattern = Pattern.compile("<\\s*(\\w+)(\\s+[^>]*)?>");

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
        htmltagpattern.matcher(value).results()
                .map(matchResult -> matchResult.group(1))
                .forEach(htmltags::add);
        // -> found: [ext, a, sly, strong, code, em, language, type, p, br, div, path, u, ul, attributes, li, ol]
    }

}
