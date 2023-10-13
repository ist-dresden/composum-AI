package com.composum.ai.backend.slingbase.impl;

import static com.composum.ai.backend.slingbase.AllowDenyMatcherUtil.allowDenyCheck;
import static com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin.PluginResult.NOT_HANDLED;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.AllowDenyMatcherUtil;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

/**
 * Implementation for {@link ApproximateMarkdownService}.
 */
@Component
@Designate(ocd = ApproximateMarkdownServiceImpl.Config.class)
public class ApproximateMarkdownServiceImpl implements ApproximateMarkdownService {

    public static final Map<String, String> ATTRIBUTE_TO_MARKDOWN_PREFIX = Map.of(
            "jcr:title", "## ",
            "title", "## ",
            "subtitle", "### "
            // , "code", "```" handled in extra method
    );

    /**
     * A list of attributes that are output (in that ordering) without any label, each on a line for itself.
     */
    @Nonnull
    protected List<String> textAttributes;

    /**
     * A list of labelled attributes that come first if they are present, in the given order.
     */
    protected List<String> labelledAttributeOrder;

    /**
     * A pattern which attributes have to be output with a label: the attribute name, a colon and a space and then the
     * trimmed attribute value followed by newline.
     */
    @Nullable
    protected Pattern labeledAttributePatternAllow;

    /**
     * A pattern matching exceptions for {@link #labeledAttributePatternAllow}.
     */
    @Nullable
    protected Pattern labeledAttributePatternDeny;


    private static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServiceImpl.class);
    /**
     * Pattern that matches an opening html tag and captures the tag name.
     */
    protected Pattern PATTERN_HTML_TAG = Pattern.compile("<\\s*(ext|a|sly|strong|code|em|language|type|p|br|div|path|u|ul|attributes|li|ol)(\\s+[^>]*)?>", Pattern.CASE_INSENSITIVE);

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    protected final Set<String> htmltags = new HashSet<>();

    // List of ApproximateMarkdownServicePlugin dynamically injected by OSGI
    @Nonnull
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, service = ApproximateMarkdownServicePlugin.class)
    protected volatile List<ApproximateMarkdownServicePlugin> plugins;

    protected void logUnhandledAttributes(Resource resource) {
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (!textAttributes.contains(entry.getKey()) && value.matches(".*\\s+.*\\s+.*\\s+.*") &&
                        !allowDenyCheck(entry.getKey(), labeledAttributePatternAllow, labeledAttributePatternDeny)) {
                    // check whether we forgot something
                    LOG.info("Ignoring text attribute {} in {}", entry.getKey(), resource.getPath());
                }
            }
        }
    }

    @Nonnull
    @Override
    public String approximateMarkdown(@Nullable Resource resource) {
        try (StringWriter s = new StringWriter()) {
            try (PrintWriter p = new PrintWriter(s)) {
                approximateMarkdown(resource, p);
            }
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
            // TODO(hps,26.05.23) this might lead to trouble if the user edits a non-default language first. Join with translations?
            // Also it'd be not quite clear what language we should take.
            return;
        }
        if (!ResourceUtil.normalize(resource.getPath()).startsWith("/content/")) {
            throw new IllegalArgumentException("For security reasons the resource must be in /content but is: " + resource.getPath());
        }
        if (!resource.getPath().contains("/jcr:content") && resource.getChild("jcr:content") != null) {
            resource = resource.getChild("jcr:content");
        }
        ApproximateMarkdownServicePlugin.PluginResult pluginResult = executePlugins(resource, out);
        boolean printEmptyLine = false;
        if (pluginResult == NOT_HANDLED) {
            for (String attributename : textAttributes) {
                String value = resource.getValueMap().get(attributename, String.class);
                if (isNotBlank(value)) {
                    String prefix = ATTRIBUTE_TO_MARKDOWN_PREFIX.getOrDefault(attributename, "");
                    String markdown;
                    if ("text".equals(attributename) && resource.getValueMap().get("textIsRich") != null) {
                        String textIsRich = resource.getValueMap().get("textIsRich", String.class);
                        markdown = "true".equalsIgnoreCase(textIsRich) ?
                                chatCompletionService.htmlToMarkdown(value).trim() : value;
                    } else {
                        markdown = getMarkdown(value);
                    }
                    out.println(prefix + markdown);
                    printEmptyLine = true;
                }
            }
            handleCodeblock(resource, out);
            handleLabeledAttributes(resource, out);
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
        if (isNotBlank(code)) {
            out.println("```\n");
            out.println(code.trim());
            out.println("\n```\n");
        }
    }

    protected void handleLabeledAttributes(Resource resource, PrintWriter out) {
        if (labeledAttributePatternAllow == null) {
            return;
        }
        for (String attributename : labelledAttributeOrder) {
            String value = resource.getValueMap().get(attributename, String.class);
            if (isNotBlank(value)) {
                out.println(attributename + ": " + getMarkdown(value));
            }
        }
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (labelledAttributeOrder.contains(entry.getKey()) || textAttributes.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (isNotBlank(value) &&
                        allowDenyCheck(entry.getKey(), labeledAttributePatternAllow, labeledAttributePatternDeny)) {
                    out.println(entry.getKey() + ": " + getMarkdown(value));
                }
            }
        }
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        LOG.info("Activated with configuration {}", config);
        textAttributes = List.of(config.textAttributes());
        labeledAttributePatternAllow = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.labelledAttributePatternAllow());
        labeledAttributePatternDeny = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.labelledAttributePatternDeny());
        labelledAttributeOrder = List.of(config.labelledAttributeOrder());
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("Deactivated.");
    }

    /**
     * Configuration class Config that allows us to configure TEXT_ATTRIBUTES.
     */
    @ObjectClassDefinition(name = "Composum AI Approximate Markdown Service Configuration", description = "Configuration for the Approximate Markdown Service used to get a text representation of a page or component for use with the AI.")
    public @interface Config {

        @AttributeDefinition(name = "Text Attributes",
                description = "List of attributes that are treated as text and converted to markdown. If not present, no attributes are treated as text.", defaultValue = {
                "jcr:title", "title", "subtitle", "linkTitle", "jcr:description", "text",
                /* "code", */ "copyright", // code component; code is handled in extra method
                "defaultValue", "exampleCode", "suffix", "exampleResult", "footer" // for servlet component
        })
        String[] textAttributes() default {};

        // these will be joined with | and then compiled as a pattern
        @AttributeDefinition(name = "Labeled Attribute Pattern Allow",
                description = "Regular expressions for attributes that are output with a label. If not present, none will be output except the text attributes.")
        String[] labelledAttributePatternAllow() default {".*"};

        @AttributeDefinition(name = "Labeled Attribute Pattern Deny",
                description = "Regular expressions for attributes that are not output with a label. Takes precedence over the corresponding allow regexp list.")
        String[] labelledAttributePatternDeny() default {".*:.*"};

        @AttributeDefinition(name = "Labelled Attribute Order",
                description = "List of labelled attributes that come first if they are present, in the given order.")
        String[] labelledAttributeOrder() default {};

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
