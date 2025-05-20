package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigService;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet with functionality for the bulk replace tool.
 * <p>
 * Operations:
 * 1. Search:
 * - POST (operation=search)
 * Parameters:
 * • rootPath: absolute page path to start search (e.g. /content/site/en)
 * • term: literal search text
 * Response: 202 Accepted with JSON payload {"jobId": "<uuid>"}.
 * Note: The search parameters are stored in an internal LinkedHashMap (only the last 10 operations are kept).
 * <p>
 * - GET (operation=search, jobId=<uuid>)
 * Streams search results as text/event-stream based on the stored parameters.
 * <p>
 * 2. Replace:
 * - POST (operation=replace)
 * Parameters:
 * • rootPath, term, replacement, target (see bulkreplace.md)
 * Response: JSON indicating replace result.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Bulk Replace Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/bulkreplace",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class BulkReplaceServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(BulkReplaceServlet.class);
    private static final Gson gson = new Gson();
    /**
     * The number of characters surrounding the match that are put into an excerpt.
     */
    private static final int SURROUNDING_CHARS = 40;
    /**
     * Random marker for start of found string, will be replaced by span tag later.
     */
    private static final String STARTMARKER = "kASDjkSD";
    private static final String STARTMARKER_REPLACEMENT = "<span class=\"foundsearchstringmarker\">";
    /**
     * Random marker for end of found string, will be replaced by span tag later.
     */
    private static final String ENDMARKER = "cAkE8LkHJG";
    private static final String ENDMARKER_REPLACEMENT = "</span>";


    @Reference
    private AutoTranslateConfigService configService;

    @Reference
    private Replicator replicator;

    /**
     * HashMap to store the last 10 search job parameters (jobId -> parameters)
     */
    private static final Map<String, SearchParams> jobMap =
            Collections.synchronizedMap(
                    new LinkedHashMap<String, SearchParams>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, SearchParams> eldest) {
                            return size() > 10;
                        }
                    });

    /**
     * Handles POST requests for search and replace operations.
     *
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws IOException {
        try {
            String operation = request.getParameter("operation");
            if ("search".equals(operation)) {
                handleSearchPOST(request, response);
            } else if ("replace".equals(operation)) {
                handleReplace(request, response);
            } else {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
            }
        } catch (Exception e) {
            LOG.error("Error during POST operation", e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error during POST operation: " + e);
        }
    }

    /**
     * Remembers the search request at a random jobID and returns that, since streaming only supports GET, which is done later.
     *
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs
     */
    private void handleSearchPOST(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // Operation: search (start job)
        // Required parameters: rootPath, term
        SearchParams params = new SearchParams();
        params.rootPath = request.getParameter("rootPath");
        params.term = request.getParameter("term");

        if (!params.rootPath.startsWith("/content") || !params.rootPath.matches("/content/[^/]+/.*")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid rootPath - should be /content and at least level 3.");
            return;
        }
        if (StringUtils.isBlank(params.rootPath) || StringUtils.isBlank(params.term)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Required parameters: rootPath, term");
            return;
        }
        // Generate a new jobId and store the parameters in the jobMap (only last 10 jobs kept)
        String jobId = UUID.randomUUID().toString();
        jobMap.put(jobId, params);
        response.setStatus(202);
        response.setContentType("application/json");
        response.getWriter().write("{\"jobId\":\"" + jobId + "\"}");
    }

    /**
     * Handles GET requests to stream search results: the second step that does the actual search.
     *
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws IOException {
        try {
            String operation = request.getParameter("operation");
            if ("search".equals(operation)) {
                String jobId = request.getParameter("jobId");
                if (StringUtils.isBlank(jobId)) {
                    response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Missing jobId");
                    return;
                }
                SearchParams params = jobMap.get(jobId);
                if (params == null) {
                    response.sendError(SlingHttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().println("JobId not found.");
                    return;
                }
                streamSearchResults(params, request, response);
            } else {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Invalid operation.");
            }
        } catch (Exception e) {
            LOG.error("Error during GET operation", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error during GET operation: " + e);
        }
    }

    /**
     * Streams search results using stored parameters.
     *
     * @param params   the search parameters, not null
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs during streaming
     */
    private void streamSearchResults(@Nonnull SearchParams params,
                                     @Nonnull SlingHttpServletRequest request,
                                     @Nonnull SlingHttpServletResponse response) throws IOException {
        Pattern termsPattern = whitespaceLenientPattern(params.term);

        ResourceResolver resolver = request.getResourceResolver();
        Resource rootResource = resolver.getResource(params.rootPath);
        if (rootResource == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Root path not found.");
            return;
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        AtomicInteger totalPages = new AtomicInteger(0);
        AtomicInteger totalMatches = new AtomicInteger(0);

        try {
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                throw new ServletException("Could not get PageManager");
            }
            // Use findResources with an XPath query for candidate pages
            String xpath = "/jcr:root" + params.rootPath + "//element(*, cq:Page)[jcr:contains(., '*" + params.term + "*')]";
            Iterator<Resource> candidatePages = resolver.findResources(xpath, "xpath");

            while (candidatePages.hasNext()) {
                Resource candidate = candidatePages.next();
                Page candidatePage = pageManager.getPage(candidate.getPath());
                if (candidatePage != null) {
                    List<Match> matches = new ArrayList<>();
                    Resource contentResource = candidatePage.getContentResource();
                    if (contentResource != null) {
                        findMatchesInResource(contentResource, "", termsPattern, matches);
                    }
                    if (!matches.isEmpty()) {
                        totalPages.incrementAndGet();
                        totalMatches.addAndGet(matches.size());
                        SearchPageResponse pageEvent = new SearchPageResponse();
                        pageEvent.page = candidatePage.getPath();
                        pageEvent.matches = matches;
                        sendEvent(response, "page", gson.toJson(pageEvent));
                    }
                }
            }

            SummaryResponse summary = new SummaryResponse();
            summary.pages = totalPages.get();
            summary.matches = totalMatches.get();
            sendEvent(response, "summary", gson.toJson(summary));

        } catch (Exception e) {
            LOG.error("Error during search operation", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error during search operation: " + e.getMessage());
        }
    }

    /**
     * Recursively finds matches in the given resource and its children.
     *
     * @param resource    the resource to search, not null
     * @param parentPath  the parent path as a string
     * @param termPattern the pattern to replace, not null
     * @param matches     the list to add found matches, not null
     */
    private void findMatchesInResource(@Nonnull Resource resource,
                                       String parentPath,
                                       @Nonnull Pattern termPattern,
                                       @Nonnull List<Match> matches) {
        ValueMap properties = resource.getValueMap();
        String componentPath = StringUtils.isNotEmpty(parentPath)
                ? parentPath + "/" + resource.getName() : resource.getName();
        if (componentPath.startsWith("jcr:content/")) {
            componentPath = componentPath.substring(componentPath.indexOf("jcr:content/") + "jcr:content/".length());
        }

        // we re-use the recognition of translateable attributes from the AI translation since this is
        // very much what we want for search and replace, though that's not entirely without risk in case of future changes.
        for (String propertyName : configService.translateableAttributes(resource)) {
            String stringValue = (String) properties.get(propertyName); // if that's not a string there is something very off.
            if (termPattern.matcher(stringValue).find()) {
                Match m = new Match();
                m.componentPath = componentPath;
                m.property = propertyName;
                m.excerpt = createExcerpt(stringValue, termPattern, SURROUNDING_CHARS);
                matches.add(m);
            }
        }

        for (Resource child : resource.getChildren()) {
            findMatchesInResource(child, componentPath, termPattern, matches);
        }
    }

    /**
     * Creates an excerpt from the given text around the search term.
     *
     * @param text        the full text, not null
     * @param termPattern a pattern matching the search term, not null
     * @return an excerpt of the text surrounding the term
     */
    @Nonnull
    protected String createExcerpt(@Nonnull String text, @Nonnull Pattern termPattern, int surroundingChars) {
        String textWithMarkers = text.replaceAll(termPattern.pattern(), STARTMARKER + "$0" + ENDMARKER);
        textWithMarkers = toPlaintext(textWithMarkers);
        Pattern textWithMarkersPattern = Pattern.compile(
                Pattern.quote(STARTMARKER) + termPattern.pattern() + Pattern.quote(ENDMARKER));
        String result = abbreviateSurroundings(textWithMarkers, textWithMarkersPattern, surroundingChars);
        return result.replace(STARTMARKER, STARTMARKER_REPLACEMENT)
                .replace(ENDMARKER, ENDMARKER_REPLACEMENT);
    }

    /**
     * Creates an excerpt from the given text around the search term.
     *
     * @param text        the full text, not null
     * @param termPattern a pattern matching the search term, not null
     * @return an excerpt of the text surrounding the term
     */
    @Nonnull
    protected String abbreviateSurroundings(@Nonnull String text, @Nonnull Pattern termPattern, int surroundingChars) {
        Matcher m = termPattern.matcher(text);
        if (m.find()) {
            StringBuilder buf = new StringBuilder();
            int lastEnd = 0;
            do {
                int start = m.start();
                if (buf.length() == 0) {
                    buf.append(StringUtils.abbreviate(text.substring(0, start), start - 1, surroundingChars));
                } else {
                    buf.append(StringUtils.abbreviateMiddle(text.substring(lastEnd, start), "...\n...", 2 * surroundingChars));
                }
                buf.append(text, start, m.end());
                lastEnd = m.end();
            } while (m.find());
            buf.append(StringUtils.abbreviate(text.substring(lastEnd), surroundingChars));
            return buf.toString();
        } else {
            return "";
        }
    }

    /**
     * Opening or closing HTML tag inclusive attributes.
     */
    private static final String HTML_TAG_PATTERN = "</?[a-zA-Z][^<>]*>";

    /**
     * Use Jsoup to create plaintext from HTML. We assume it's HTML if it starts with < and ends with > , trimmed.
     */
    protected String toPlaintext(String text) {
        if (text.contains("<") && text.contains(">")) {
            return StringUtils.strip(text.replaceAll(HTML_TAG_PATTERN, ""));
        }
        return text;
    }

    /**
     * A pattern that matches the search String case sensitively but being lenient on whitespaces:
     * the pattern made from "a b c" should also match "a  b\nc".
     */
    protected Pattern whitespaceLenientPattern(@Nonnull String searchString) {
        String[] words = searchString.split("\\s+");
        StringBuilder patternBuilder = new StringBuilder();
        for (String word : words) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append("\\s+");
            }
            patternBuilder.append(Pattern.quote(word));
        }
        return Pattern.compile(patternBuilder.toString());
    }

    /**
     * Handles replacement operations, supporting both JSON and form-based requests.
     *
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs
     */
    private void handleReplace(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws IOException {
        if (request.getContentType() != null && request.getContentType().contains("application/json")) {
            ReplaceRequest replaceRequest = null;
            try (Reader reader = request.getReader()) {
                replaceRequest = gson.fromJson(reader, ReplaceRequest.class);
                if (replaceRequest.page == null || replaceRequest.term == null ||
                        replaceRequest.replacement == null || replaceRequest.targets == null ||
                        replaceRequest.targets.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Required fields: page, term, replacement, targets");
                    return;
                }

                ResourceResolver resolver = request.getResourceResolver();
                // Use PageManager to retrieve the page and work on its content resource
                PageManager pageManager = resolver.adaptTo(PageManager.class);
                if (pageManager == null) {
                    response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "PageManager not available");
                    return;
                }
                Page page = pageManager.getPage(replaceRequest.page);
                if (page == null) {
                    response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Page not found: " + replaceRequest.page);
                    return;
                }
                Resource pageContentResource = page.getContentResource();
                if (pageContentResource == null) {
                    response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Content resource not found for page: " + replaceRequest.page);
                    return;
                }

                if (replaceRequest.createVersion) {
                    LOG.info("Creating version for page: {}", replaceRequest.page);
                    createVersion(pageContentResource, replaceRequest.term, replaceRequest.replacement);
                }

                Pattern replacePattern = whitespaceLenientPattern(replaceRequest.term);

                List<Changed> changedList = new ArrayList<>();
                boolean pageModified = false;
                for (Target target : replaceRequest.targets) {
                    Resource componentResource = JcrConstants.JCR_CONTENT.equals(target.componentPath) ? pageContentResource :
                            pageContentResource.getChild(target.componentPath);
                    if (componentResource == null) {
                        throw new IOException("Component not found: " + target.componentPath);
                    }
                    // Capture the original value
                    ValueMap props = componentResource.adaptTo(ValueMap.class);
                    String oldVal = props != null ? props.get(target.property, String.class) : null;
                    // Replace and capture new value.
                    String newVal = replaceInProperty(componentResource, target.property, replacePattern, replaceRequest.replacement);
                    if (newVal != null) {
                        Changed ch = new Changed();
                        ch.componentPath = target.componentPath;
                        ch.property = target.property;
                        ch.excerpt = createExcerpt(newVal,
                                Pattern.compile(Pattern.quote(replaceRequest.replacement)),
                                SURROUNDING_CHARS);
                        ch.oldValue = oldVal;
                        ch.newValue = newVal;
                        changedList.add(ch);
                        pageModified = true;
                    } else {
                        throw new IOException("No changes made for property: " + target.property +
                                " in " + target.componentPath + " of " + replaceRequest.page);
                    }
                }

                Boolean published = null;
                if (pageModified) {
                    pageManager.touch(page.adaptTo(Node.class), true, Calendar.getInstance(), false);
                    resolver.commit();
                    if (replaceRequest.autoPublish) {
                        LOG.info("Auto‑publishing page: {}", replaceRequest.page);
                        published = autoPublishPage(pageContentResource);
                    }
                }

                response.setStatus(SlingHttpServletResponse.SC_OK);
                response.setContentType("application/json");
                ReplacePageResponse pageResp = new ReplacePageResponse();
                pageResp.page = replaceRequest.page;
                pageResp.changed = changedList;
                pageResp.published = published;
                response.getWriter().write(gson.toJson(pageResp));
            } catch (JsonSyntaxException e) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request");
            } catch (WCMException e) {
                LOG.error("{} on {}", e, replaceRequest, e);
                throw new IOException("Error during replace operation", e);
            }
        } else {
            throw new IOException("Unsupported content type: " + request.getContentType());
        }
    }

    /**
     * Replaces occurrences of the specified term with the replacement in the resource property.
     * Returns the new property value if changed; otherwise returns null.
     *
     * @param resource     the resource to modify, not null
     * @param propertyName the name of the property, not null
     * @param termPattern  a pattern for the search term, not null
     * @param replacement  the replacement string, not null
     * @return the new property value if a change was made; null otherwise
     */
    private String replaceInProperty(@Nonnull Resource resource, @Nonnull String propertyName,
                                     @Nonnull Pattern termPattern, @Nonnull String replacement) {
        ValueMap properties = resource.adaptTo(ValueMap.class);
        if (properties == null || !properties.containsKey(propertyName)) {
            return null;
        }
        String value = properties.get(propertyName, String.class);
        if (value == null) {
            return null;
        }
        String newValue = termPattern.matcher(value).replaceAll(replacement);
        if (!newValue.equals(value)) {
            ModifiableValueMap modifiableProperties = resource.adaptTo(ModifiableValueMap.class);
            if (modifiableProperties == null) {
                LOG.warn("Cannot get ModifiableValueMap for resource: {}", resource.getPath());
                return null;
            }
            modifiableProperties.put(propertyName, newValue);

            // Also replace pattern in target language saved values for the property to make it consistent.
            // This is not entirely without doubt, though, but probably the best way.
            AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(null, modifiableProperties, propertyName);
            if (wrapper.getTranslatedCopy() != null) {
                wrapper.setTranslatedCopy(termPattern.matcher(wrapper.getTranslatedCopy()).replaceAll(replacement));
            }
            if (wrapper.getNewTranslatedCopy() != null) {
                wrapper.setNewTranslatedCopy(termPattern.matcher(wrapper.getNewTranslatedCopy()).replaceAll(replacement));
            }
            if (wrapper.getAcceptedTranslation() != null) {
                wrapper.setAcceptedTranslation(termPattern.matcher(wrapper.getAcceptedTranslation()).replaceAll(replacement));
            }

            return newValue;
        }
        return null;
    }

    /**
     * Sends a Server-Sent Event to the client.
     *
     * @param response  the SlingHttpServletResponse, not null
     * @param eventName the event name, not null
     * @param data      the event data as a JSON string, not null
     * @throws IOException if an I/O error occurs while sending the event
     */
    private void sendEvent(@Nonnull SlingHttpServletResponse response, @Nonnull String eventName,
                           @Nonnull String data) throws IOException {
        response.getWriter().write("event: " + eventName + "\n");
        response.getWriter().write("data: " + data + "\n\n");
        response.getWriter().flush();
    }

    /**
     * Creates a revision of the page using the PageManager's createRevision method.
     *
     * @param pageResource the page resource to version, not null
     * @param term         the search term used, not null
     * @param replacement  the replacement text, not null
     * @throws IOException if version creation fails
     */
    private void createVersion(@Nonnull Resource pageResource, @Nonnull String term, @Nonnull String replacement)
            throws IOException {
        try {
            PageManager pageManager = Objects.requireNonNull(pageResource.getResourceResolver().adaptTo(PageManager.class));
            Page page = pageManager.getContainingPage(pageResource.getPath());
            pageManager.createRevision(page, null, "Before replacing '" + term + "' with '" + replacement + "'");
        } catch (Exception e) {
            LOG.error("Error creating version for page: {}", pageResource.getPath(), e);
            throw new IOException("Failed to create revision", e);
        }
    }

    /**
     * Automatically publishes the page if it qualifies based on modification and replication status.
     *
     * @param contentResource the page resource to publish, not null
     * @return true if the page was published, false otherwise
     */
    private boolean autoPublishPage(@Nonnull Resource contentResource) throws IOException {
        try {
            ValueMap vm = Objects.requireNonNull(contentResource).getValueMap();
            Calendar lastModified = vm.get("cq:lastModified", Calendar.class);
            Calendar lastReplicated = vm.get("cq:lastReplicated", Calendar.class);
            String lastAction = vm.get("cq:lastReplicationAction", String.class);
            // Only auto‑publish if the page qualifies: lastModified is not after lastReplicated and replication was Activate.
            if (lastModified != null && lastReplicated != null
                    && !lastModified.after(lastReplicated)
                    && "Activate".equals(lastAction)) {
                Session session = Objects.requireNonNull(contentResource.getResourceResolver().adaptTo(Session.class));
                replicator.replicate(session, ReplicationActionType.ACTIVATE, contentResource.getPath());
                return true;
            } else {
                LOG.info("Page {} does not qualify for auto‑publication", contentResource.getPath());
            }
            return false;
        } catch (Exception e) {
            LOG.error("Error auto‑publishing page: {}", contentResource.getPath(), e);
            throw new IOException("Failed to auto‑publish page", e);
        }
    }

    // --- Static inner classes for various objects ---

    protected static class SearchParams {
        public String rootPath;
        public String term;
    }

    protected static class SearchPageResponse {
        public String page;
        public List<Match> matches;
    }

    protected static class SummaryResponse {
        public int pages;
        public int matches;
    }

    protected static class ReplacePageResponse {
        public String page;
        public List<Changed> changed;
        public Boolean published;
    }

    protected static class Match {
        public String componentPath;
        public String property;
        public String excerpt;
    }

    protected static class Changed {
        public String componentPath;
        public String property;
        public String excerpt;
        public String oldValue;
        public String newValue;
    }

    protected static class ReplaceRequest {
        public String page;
        public String term;
        public String replacement;
        public List<Target> targets;
        public boolean createVersion;
        public boolean autoPublish;

        @Override
        public String toString() {
            return "ReplaceRequest{" +
                    "page='" + page + '\'' +
                    ", term='" + term + '\'' +
                    ", replacement='" + replacement + '\'' +
                    ", createVersion=" + createVersion +
                    ", autoPublish=" + autoPublish +
                    '}';
        }
    }

    protected static class Target {
        public String componentPath;
        public String property;
    }

}
