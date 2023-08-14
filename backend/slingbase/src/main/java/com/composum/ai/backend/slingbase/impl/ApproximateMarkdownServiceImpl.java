package com.composum.ai.backend.slingbase.impl;

import static com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin.PluginResult.NOT_HANDLED;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

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

    // List of ApproximateMarkdownServicePlugin dynamically injected by OSGI
    @Nonnull
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, service = ApproximateMarkdownServicePlugin.class)
    protected volatile List<ApproximateMarkdownServicePlugin> plugins;

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
            // FIXME(hps,26.05.23) this might lead to trouble if the user edits a non-default language first. Join with translations?
            // Also it's not quite clear what language we should take.
            return;
        }
        if (!ResourceUtil.normalize(resource.getPath()).startsWith("/content/")) {
            throw new IllegalArgumentException("For security reasons the resource must be in /content but is: " + resource.getPath());
        }
        ApproximateMarkdownServicePlugin.PluginResult pluginResult = executePlugins(resource, out);
        boolean printEmptyLine = false;
        if (pluginResult == NOT_HANDLED) {
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
        if (printEmptyLine) {
            out.println();
        }
        if (pluginResult == NOT_HANDLED || pluginResult == ApproximateMarkdownServicePlugin.PluginResult.HANDLED_ATTRIBUTES) {
            resource.getChildren().forEach(child -> approximateMarkdown(child, out));
        }
        logUnhandledAttributes(resource);
    }

    @Nonnull
    protected ApproximateMarkdownServicePlugin.PluginResult executePlugins(@Nonnull Resource resource, @Nonnull PrintWriter out) {
        for (ApproximateMarkdownServicePlugin plugin : plugins) {
            ApproximateMarkdownServicePlugin.PluginResult pluginResult = plugin.maybeHandle(resource, out, this);
            if (pluginResult != null && pluginResult != NOT_HANDLED) {
                return pluginResult;
            }
        }
        return NOT_HANDLED;
    }

    @Override
    @Nonnull
    public String getMarkdown(@Nullable String value) {
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

    protected void handleCodeblock(Resource resource, PrintWriter out) {
        String code = resource.getValueMap().get("code", String.class);
        if (StringUtils.isNotBlank(code)) {
            out.println("```\n");
            out.println(code.trim());
            out.println("\n```\n");
        }
    }

    // debugging code; remove after it works.

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
