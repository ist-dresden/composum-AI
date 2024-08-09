package com.composum.ai.backend.slingbase.impl;

import static com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin.PluginResult.HANDLED_ATTRIBUTES;
import static com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin.PluginResult.NOT_HANDLED;
import static com.composum.ai.backend.slingbase.impl.AllowDenyMatcherUtil.allowDenyCheck;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
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
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin.PluginResult;

/**
 * Implementation for {@link ApproximateMarkdownService}.
 */
@Component
@Designate(ocd = ApproximateMarkdownServiceImpl.Config.class)
public class ApproximateMarkdownServiceImpl implements ApproximateMarkdownService {

    public static final Map<String, String> ATTRIBUTE_TO_MARKDOWN_PREFIX = new HashMap<>();

    {
        {
            ATTRIBUTE_TO_MARKDOWN_PREFIX.put("jcr:title", "## ");
            ATTRIBUTE_TO_MARKDOWN_PREFIX.put("title", "## ");
            ATTRIBUTE_TO_MARKDOWN_PREFIX.put("subtitle", "### ");
            ATTRIBUTE_TO_MARKDOWN_PREFIX.put("cq:panelTitle", "#### ");
            // , "code", "```" handled in extra method
        }
    }

    /**
     * Ignored values for labelled output: "true"/ "false" / single number (int / float) attributes or array of numbers attributes, or shorter than 3 digits or path, or array or type date or boolean or {Date} or {Boolean} , inherit, blank, html tags, target .
     */
    protected final static Pattern IGNORED_VALUE_PATTERN = Pattern.compile("true|false|[0-9][0-9]?[0-9]?|/(conf|content|etc)/.*|\\{Boolean\\}(true|false)|inherit|blank|target|h[0-9]|div|p");

    /**
     * We ignore nodes named i18n or renditions and nodes starting with rep:, dam:, cq:
     */
    protected final static Pattern IGNORED_NODE_NAMES = Pattern.compile("i18n|renditions|rep:.*|dam:.*|cq:.*");

    protected final static Pattern IMAGE_PATTERN = Pattern.compile("\\.(png|jpg|jpeg|gif|svg)(/|$)", Pattern.CASE_INSENSITIVE);

    protected final static Pattern VIDEO_PATTERN = Pattern.compile("\\.(mp4|mov)(/|$)", Pattern.CASE_INSENSITIVE);

    /** We allow generating markdown for subpaths of /content, /public and /preview . */
    public static final Pattern ADMISSIBLE_PATH_PATTERN = Pattern.compile("/(content|preview|public)/.*/.*");

    /** If that occurs in a string it has several words. */
    public static final Pattern THREE_WHITESPACE_PATTERN = Pattern.compile("\\s\\S+\\s+\\S+\\s");

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

    /**
     * Whitelist for URLs we can connect to get the markdown. Required - the URL has to match one of the patterns.
     */
    protected List<Pattern> urlBlacklist;

    /**
     * Blacklist for URLs we can connect to get the markdown. The URL must not match one of the patterns.
     */
    protected List<Pattern> urlWhitelist;


    private static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServiceImpl.class);

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    // List of ApproximateMarkdownServicePlugin dynamically injected by OSGI
    @Nonnull
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, service = ApproximateMarkdownServicePlugin.class)
    protected volatile List<ApproximateMarkdownServicePlugin> plugins;

    protected void logUnhandledAttributes(Resource resource) {
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (!textAttributes.contains(entry.getKey()) && value.contains(" ") && THREE_WHITESPACE_PATTERN.matcher(value).find() &&
                        !allowDenyCheck(entry.getKey(), labeledAttributePatternAllow, labeledAttributePatternDeny)) {
                    // check whether we forgot something
                    LOG.info("Ignoring text attribute {} in {}", entry.getKey(), resource.getPath());
                }
            }
        }
    }

    @Nonnull
    @Override
    public String approximateMarkdown(@Nullable Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response) {
        LOG.debug(">>> approximateMarkdown for {}", resource != null ? resource.getPath() : null);
        try (StringWriter s = new StringWriter()) {
            try (PrintWriter out = new PrintWriter(s)) {
                approximateMarkdown(resource, out, request, response);
            }
            return s.toString();
        } catch (IOException e) {
            // pretty much impossible for a StringWriter , no sensible handling.
            throw new IllegalStateException(e);
        } finally {
            LOG.debug("<<< approximateMarkdown for {}", resource != null ? resource.getPath() : null);
        }
    }

    @Override
    public void approximateMarkdown(
            @Nullable Resource resource, @Nonnull PrintWriter realOutput,
            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        if (resource == null || IGNORED_NODE_NAMES.matcher(resource.getName()).matches()) {
            // The content of i18n nodes would be a duplication as it was already printed as "text" attribute in the parent node.
            // TODO(hps,26.05.23) this might lead to trouble if the user edits a non-default language first. Join with translations?
            // Also, it'd be not quite clear what language we should take. That's Composum only, though.
            return;
        }
        if (!ADMISSIBLE_PATH_PATTERN.matcher(ResourceUtil.normalize(resource.getPath())).matches()) {
            throw new IllegalArgumentException("For security reasons the resource must be in /content,/preview,/public but is: " + resource.getPath());
        }
        if (!resource.getPath().contains("/jcr:content") && resource.getChild("jcr:content") != null) {
            resource = resource.getChild("jcr:content");
        }
        StringWriter buf = new StringWriter();
        PrintWriter out = new PrintWriter(buf, false);
        PluginResult pluginResult = executePlugins(resource, out, request, response);
        boolean printEmptyLine = false;
        if (pluginResult == NOT_HANDLED) {
            if (resource.getValueMap().isEmpty() && !resource.hasChildren()) { // attribute resource
                String attributeName = resource.getName();
                String markdown = attributeToMarkdown(resource.getParent(), attributeName,
                        resource.getParent().getValueMap().get(attributeName, String.class));
                out.println(markdown);
                // no need for empty line since there are no children.
            } else {
                printEmptyLine = handleResource(resource, out, printEmptyLine);
            }
        }
        if (printEmptyLine) {
            out.println();
        }
        if (pluginResult == NOT_HANDLED || pluginResult == HANDLED_ATTRIBUTES) {
            resource.getChildren().forEach(child -> approximateMarkdown(child, out, request, response));
        }
        logUnhandledAttributes(resource);

        out.close();
        String markdown = buf.toString();
        if (!markdown.isEmpty()) {
            realOutput.print(markdown);
            for (ApproximateMarkdownServicePlugin plugin : plugins) {
                plugin.cacheMarkdown(resource, markdown);
            }
        }
    }

    protected boolean handleResource(@NotNull Resource resource, @NotNull PrintWriter out, boolean printEmptyLine) {
        for (String attributename : textAttributes) {
            String value = resource.getValueMap().get(attributename, String.class);
            if (isNotBlank(value)) {
                String prefix = ATTRIBUTE_TO_MARKDOWN_PREFIX.getOrDefault(attributename, "");
                String markdown = attributeToMarkdown(resource, attributename, value);
                out.println(prefix + markdown);
                printEmptyLine = true;
            }
        }
        printEmptyLine = handleCodeblock(resource, out, printEmptyLine);
        printEmptyLine = handleLabeledAttributes(resource, out, printEmptyLine);
        return printEmptyLine;
    }

    protected String attributeToMarkdown(@NotNull Resource resource, String attributename, String value) {
        String markdown;
        if ("text".equals(attributename) && resource.getValueMap().get("textIsRich") != null) {
            String textIsRich = resource.getValueMap().get("textIsRich", String.class);
            markdown = "true".equalsIgnoreCase(textIsRich) ?
                    chatCompletionService.htmlToMarkdown(value).trim() : value;
        } else {
            markdown = getMarkdown(value);
        }
        return markdown;
    }

    @Nonnull
    protected PluginResult executePlugins(
            @Nonnull Resource resource, @Nonnull PrintWriter out,
            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        for (ApproximateMarkdownServicePlugin plugin : plugins) {
            PluginResult pluginResult =
                    plugin.maybeHandle(resource, out, this, request, response);
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

    @NotNull
    @Override
    public String getMarkdown(@Nonnull URI uri) throws MalformedURLException, IOException, IllegalArgumentException {
        URLConnection conn = checkUrlAdmissible(uri).toURL().openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        try (InputStream in = conn.getInputStream()) {
            String contentType = conn.getContentType() == null ? "" : conn.getContentType();
            if (contentType.contains("text/html") || contentType.contains("application/xhtml+xml")) {
                Charset charset = conn.getContentEncoding() != null ?
                        Charset.forName(conn.getContentEncoding()) : Charset.defaultCharset();
                String result = IOUtils.toString(in, charset);
                return chatCompletionService.htmlToMarkdown(result);
            } else if (contentType.contains("text/plain")) {
                Charset charset = conn.getContentEncoding() != null ?
                        Charset.forName(conn.getContentEncoding()) : Charset.defaultCharset();
                // not quite markdown, but there is no sensible conversion and that's likely OK, anyway.
                return IOUtils.toString(in, charset);
            } else {
                throw new IllegalArgumentException("Unsupported content type " + contentType + " for " + uri);
            }
        }
    }

    protected URI checkUrlAdmissible(URI uri) {
        if (urlBlacklist.stream().anyMatch(pattern -> pattern.matcher(uri.toString()).find())) {
            throw new IllegalArgumentException("URL " + uri + " is blacklisted " +
                    "in OSGI configuration 'Composum AI Approximate Markdown Service Configuration'");
        }
        if (urlWhitelist.isEmpty() || urlWhitelist.stream().noneMatch(pattern -> pattern.matcher(uri.toString()).find())) {
            throw new IllegalArgumentException("URL " + uri + " is not whitelisted " +
                    "in OSGI configuration 'Composum AI Approximate Markdown Service Configuration'");
        }
        return uri;
    }

    protected boolean handleCodeblock(Resource resource, PrintWriter out, boolean printEmptyLine) {
        String code = resource.getValueMap().get("code", String.class);
        if (isNotBlank(code)) {
            out.println("```\n");
            out.println(code.trim());
            out.println("\n```\n");
            return true;
        }
        return printEmptyLine;
    }

    protected boolean handleLabeledAttributes(Resource resource, PrintWriter out, boolean printEmptyLine) {
        if (labeledAttributePatternAllow == null) {
            return false;
        }
        boolean firstline = true;
        for (String attributename : labelledAttributeOrder) {
            String value = resource.getValueMap().get(attributename, String.class);
            if (isNotBlank(value)) {
                if (printEmptyLine && firstline) {
                    out.println();
                    firstline = false;
                }
                out.println(attributename + ": " + getMarkdown(value) + " <br>");
                printEmptyLine = true;
            }
        }
        for (Map.Entry<String, Object> entry : resource.getValueMap().entrySet()) {
            if (labelledAttributeOrder.contains(entry.getKey()) || textAttributes.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (isNotBlank(value) && admissibleValue(value) &&
                        allowDenyCheck(entry.getKey(), labeledAttributePatternAllow, labeledAttributePatternDeny)) {
                    if (printEmptyLine && firstline) {
                        out.println();
                        firstline = false;
                    }
                    out.println(entry.getKey() + ": " + getMarkdown(value) + " <br>");
                    printEmptyLine = true;
                }
            }
        }
        return printEmptyLine;
    }

    /**
     * We do not print pure numbers, booleans and some special strings since those are likely attributes determining the component layout, not actual text that is printed.
     * all "true"/ "false" / single number (int / float) attributes or array of numbers attributes, or shorter than 3 digits or path, or array or type date or boolean or {Date} or {Boolean} , inherit, blank, html tags, target .
     */
    protected boolean admissibleValue(Object object) {
        if (object instanceof String) {
            String value = (String) object;
            return !IGNORED_VALUE_PATTERN.matcher(value).matches();
        }
        return false;
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        LOG.info("Activated with configuration {}", config);
        textAttributes = Stream.of(config.textAttributes())
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());
        labeledAttributePatternAllow = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.labelledAttributePatternAllow());
        labeledAttributePatternDeny = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.labelledAttributePatternDeny());
        labelledAttributeOrder = Stream.of(config.labelledAttributeOrder())
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());
        urlBlacklist = Stream.of(config.urlSourceBlacklist() != null ? config.urlSourceBlacklist() : new String[0])
                .filter(StringUtils::isNotBlank).map(Pattern::compile).collect(Collectors.toList());
        urlWhitelist = Stream.of(config.urlSourceWhitelist() != null ? config.urlSourceWhitelist() : new String[0])
                .filter(StringUtils::isNotBlank).map(Pattern::compile).collect(Collectors.toList());
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

        @AttributeDefinition(name = "URL Source Whitelist Regex",
                description = "Only if using URLs as external source: Whitelist for URLs that can be read and turned into markdown. If not set, reading URLs is turned off." +
                        "For security reasons you might want to prevent local addresses to be contacted." +
                        "To allow everything you might use https?://.* , but make sure you have a good blacklist in that case.")
        String[] urlSourceWhitelist();

        @AttributeDefinition(name = "URL Source Blacklist Regex",
                description = "Only if using URLs as external source: Blacklist for URLs that can be read and turned into markdown. Has precendence over whitelist.")
        String[] urlSourceBlacklist() default {
                ".*localhost.*",
                "^(?!https?://).*",  // URLs that are not http / https are not allowed
                // URLs where the host name is only digits / periods (numeric IPs)
                ".*://[0-9.]*/.*",
                // IPv6 hostnames in URLs are not allowed:
                ".*://\\[[0-9a-fA-F:]*\\].*",
        };

        @AttributeDefinition(name = "Text Attributes",
                description = "List of attributes that are treated as text and converted to markdown. If not present, no attributes are treated as text.")
        String[] textAttributes() default {
                "jcr:title", "title", "subtitle", "linkTitle", "jcr:description", "text", "cq:panelTitle",
                /* "code", */ "copyright", // code component; code is handled in extra method
                "defaultValue", "exampleCode", "suffix", "exampleResult", "footer" // for servlet component
        };

        // these will be joined with | and then compiled as a pattern
        @AttributeDefinition(name = "Labeled Attribute Pattern Allow",
                description = "Regular expressions for attributes that are output with a label. If not present, none will be output except the text attributes.")
        String[] labelledAttributePatternAllow() default {".*"};

        @AttributeDefinition(name = "Labeled Attribute Pattern Deny",
                description = "Regular expressions for attributes that are not output with a label. Takes precedence over the corresponding allow regexp list.")
        String[] labelledAttributePatternDeny() default {".*:.*", "layout", "backgroundColor", "color", "textColor", "template",
                "theme", "variation", "buttonSymbol", "columns", "icon", "elementType", "textAlignment", "alignment",
                "linkTarget", "interval", "fileReference", "height", "width", "textIsRich", "style",
                "padding.*", ".*[cC]ss[cC]lass.*"};

        @AttributeDefinition(name = "Labelled Attribute Order",
                description = "List of labelled attributes that come first if they are present, in the given order.")
        String[] labelledAttributeOrder() default {};

    }

    /**
     * {@inheritDoc}
     * We traverse the attributes of resource and all children and collect everything that starts with /content.
     * If there are less than 5 links, we continue with the parent resource until jcr:content is reached.
     * The link title will be the jcr:title or title attribute.
     */
    @NotNull
    @Override
    public List<Link> getComponentLinks(@NotNull Resource resource) {
        List<Link> resourceLinks = new ArrayList<>();
        if (resource == null) {
            return resourceLinks;
        }

        plugins.stream().forEach(plugin -> resourceLinks.addAll(plugin.getMasterLinks(resource)));

        Resource searchResource = resource;
        if (resource.getValueMap().isEmpty()) { // attribute resource, use parent
            searchResource = resource.getParent();
        }
        while (searchResource != null && resourceLinks.size() < 5 && searchResource.getPath().contains("/jcr:content/")) {
            List<Link> resourceLinkCandidates = new ArrayList<>();
            collectLinks(searchResource, resourceLinkCandidates);
            Iterator<Link> iterator = resourceLinkCandidates.iterator();
            while (resourceLinks.size() < 5 && iterator.hasNext()) {
                Link link = iterator.next();
                if (!resourceLinks.contains(link)) {
                    resourceLinks.add(link);
                }
            }
            searchResource = searchResource.getParent();
        }
        return resourceLinks;
    }

    /**
     * Collects links from a resource and its children. The link title will be the jcr:title or title attribute.
     *
     * @param resource      the resource to collect links from
     * @param resourceLinks the list to store the collected links
     */
    protected void collectLinks(@NotNull Resource resource, List<Link> resourceLinks) {
        resource.getValueMap().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .filter(entry -> ((String) entry.getValue()).startsWith("/content/"))
                .forEach(entry -> {
                    String path = (String) entry.getValue();
                    Resource targetResource = resource.getResourceResolver().getResource(path);
                    if (targetResource != null) {
                        if (targetResource.getChild(JcrConstants.JCR_CONTENT) != null) {
                            targetResource = targetResource.getChild(JcrConstants.JCR_CONTENT);
                        }
                        String title = targetResource.getValueMap().get("jcr:title", String.class);
                        if (title == null) {
                            title = targetResource.getValueMap().get("title", String.class);
                        }
                        if (title == null) {
                            title = targetResource.getName();
                            if (JcrConstants.JCR_CONTENT.equals(title)) {
                                title = targetResource.getParent().getName();
                            }
                        }
                        boolean needsVision = isNeedsVision(targetResource);
                        if (!VIDEO_PATTERN.matcher(targetResource.getPath()).find()) {
                            Link link = new Link(path, title, needsVision);
                            if (!resourceLinks.contains(link)) {
                                resourceLinks.add(link);
                            }
                        }
                    }
                });
        resource.getChildren().forEach(child -> {
            collectLinks(child, resourceLinks);
        });
    }

    private static boolean isNeedsVision(Resource targetResource) {
        if (IMAGE_PATTERN.matcher(targetResource.getPath()).find()) {
            return true;
        }
        if (targetResource.getValueMap().get("jcr:content/jcr:mimeType", String.class) != null) {
            return true;
        }
        return false;
    }

    @Override
    public String getImageUrl(Resource imageResource) {
        if (imageResource == null) {
            return null;
        }
        for (ApproximateMarkdownServicePlugin plugin : plugins) {
            String imageUrl = plugin.getImageUrl(imageResource);
            if (imageUrl != null) {
                return imageUrl;
            }
        }
        return null;
    }

    // debugging code; remove after it works.

    protected Pattern PATTERN_HTML_TAG = Pattern.compile("<\\s*(ext|a|sly|strong|code|em|language|type|p|br|div|path|u|ul|attributes|li|ol|h[1-6]|b|i)(\\s+[^>]*)?>", Pattern.CASE_INSENSITIVE);

    protected final Set<String> htmltags = new HashSet<>();

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
                    // out.println(resource.getPath() + " [" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey() + ": " + value);
                    // out.println("[" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey() + ": " + value);
                    out.println("[" + resourceTypeForChildren + "] " + subpathForAttributes + entry.getKey());
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
        Matcher m = PATTERN_HTML_TAG.matcher(value);
        while (m.find()) {
            htmltags.add(m.group(1));
        }
        // -> found: [ext, a, sly, strong, code, em, language, type, p, br, div, path, u, ul, attributes, li, ol]
    }

}
