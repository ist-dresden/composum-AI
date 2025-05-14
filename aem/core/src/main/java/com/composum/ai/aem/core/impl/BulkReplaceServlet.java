package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
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
    public static final Gson gson = new Gson();

    // LinkedHashMap to store the last 10 search job parameters (jobId -> parameters)
    private static final LinkedHashMap<String, Map<String, String>> jobMap = new LinkedHashMap<String, Map<String, String>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, String>> eldest) {
            return size() > 10;
        }
    };

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
        String operation = request.getParameter("operation");
        if ("search".equals(operation)) {
            // Operation: search (start job)
            // Required parameters: rootPath, term
            String rootPath = request.getParameter("rootPath");
            String term = request.getParameter("term");
            if (StringUtils.isBlank(rootPath) || StringUtils.isBlank(term)) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Required parameters: rootPath, term");
                return;
            }
            // Generate a new jobId and store the parameters in the jobMap (only last 10 jobs kept)
            String jobId = UUID.randomUUID().toString();
            Map<String, String> params = new HashMap<>();
            params.put("rootPath", rootPath);
            params.put("term", term);
            synchronized (jobMap) {
                jobMap.put(jobId, params);
            }
            response.setStatus(202);
            response.setContentType("application/json");
            response.getWriter().write("{\"jobId\":\"" + jobId + "\"}");
        } else if ("replace".equals(operation)) {
            handleReplace(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
        }
    }

    /**
     * Handles GET requests to stream search results.
     *
     * @param request  the SlingHttpServletRequest, not null
     * @param response the SlingHttpServletResponse, not null
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws IOException {
        String operation = request.getParameter("operation");
        if ("search".equals(operation)) {
            String jobId = request.getParameter("jobId");
            if (StringUtils.isBlank(jobId)) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing jobId");
                return;
            }
            Map<String, String> params;
            synchronized (jobMap) {
                params = jobMap.get(jobId);
            }
            if (params == null) {
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "JobId not found");
                return;
            }
            streamSearchResults(params, request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid GET operation");
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
    private void streamSearchResults(@Nonnull Map<String, String> params,
                                     @Nonnull SlingHttpServletRequest request,
                                     @Nonnull SlingHttpServletResponse response) throws IOException {
        String rootPath = params.get("rootPath");
        String term = params.get("term");

        ResourceResolver resolver = request.getResourceResolver();
        Resource rootResource = resolver.getResource(rootPath);
        if (rootResource == null) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Root path not found: " + rootPath);
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
            String xpath = "/jcr:root" + rootPath + "//element(*, cq:Page)[jcr:contains(., '*" + term + "*')]";
            Iterator<Resource> candidatePages = resolver.findResources(xpath, "xpath");

            while (candidatePages.hasNext()) {
                Resource candidate = candidatePages.next();
                Page candidatePage = pageManager.getPage(candidate.getPath());
                if (candidatePage != null) {
                    List<Match> matches = findMatches(candidatePage, term);
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
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Finds and returns matches for the search term within the given page.
     *
     * @param page the page to search, not null
     * @param term the search term, not null
     * @return a list of matches found in the page
     */
    private List<Match> findMatches(@Nonnull Page page, @Nonnull String term) {
        List<Match> matches = new ArrayList<>();
        Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            findMatchesInResource(contentResource, "", term, matches);
        }
        return matches;
    }

    /**
     * Recursively finds matches in the given resource and its children.
     *
     * @param resource   the resource to search, not null
     * @param parentPath the parent path as a string
     * @param term       the search term, not null
     * @param matches    the list to add found matches, not null
     */
    private void findMatchesInResource(@Nonnull Resource resource,
                                       String parentPath,
                                       @Nonnull String term,
                                       @Nonnull List<Match> matches) {
        ValueMap properties = resource.getValueMap();
        String componentPath = StringUtils.isNotEmpty(parentPath)
                ? parentPath + "/" + resource.getName() : resource.getName();
        if (componentPath.contains("jcr:content/")) {
            componentPath = componentPath.substring(componentPath.indexOf("jcr:content/") + "jcr:content/".length());
        }

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;
                String trimmed = stringValue.trim();
                // Skip values that seem like paths or URLs.
                if (trimmed.startsWith("/") || trimmed.startsWith("http:") || trimmed.startsWith("https:")) {
                    continue;
                }
                if (StringUtils.contains(stringValue, term)) {
                    Match m = new Match();
                    m.componentPath = componentPath;
                    m.property = propertyName;
                    m.excerpt = createExcerpt(stringValue, term);
                    matches.add(m);
                }
            }
        }

        for (Resource child : resource.getChildren()) {
            findMatchesInResource(child, componentPath, term, matches);
        }
    }

    /**
     * Creates an excerpt from the given text around the search term.
     *
     * @param text the full text, not null
     * @param term the search term, not null
     * @return an excerpt of the text surrounding the term
     */
    private String createExcerpt(@Nonnull String text, @Nonnull String term) {
        int index = text.indexOf(term);
        int start = Math.max(0, index - 20);
        int end = Math.min(text.length(), index + term.length() + 20);
        String excerpt = text.substring(start, end);
        if (start > 0) excerpt = "..." + excerpt;
        if (end < text.length()) excerpt = excerpt + "...";
        return excerpt;
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
            try (Reader reader = request.getReader()) {
                ReplaceRequest replaceRequest = gson.fromJson(reader, ReplaceRequest.class);
                if (replaceRequest.page == null || replaceRequest.term == null ||
                        replaceRequest.replacement == null || replaceRequest.targets == null ||
                        replaceRequest.targets.isEmpty()) {
                    response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST,
                            "Required fields: page, term, replacement, targets");
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
                List<Changed> changedList = new ArrayList<>();
                boolean pageModified = false;
                for (Target target : replaceRequest.targets) {
                    Resource componentResource = pageContentResource.getChild(target.componentPath);
                    if (componentResource == null) {
                        throw new IOException("Component not found: " + target.componentPath);
                    }
                    // Capture the original value
                    ValueMap props = componentResource.adaptTo(ValueMap.class);
                    String oldVal = props != null ? props.get(target.property, String.class) : null;
                    // Replace and capture new value.
                    String newVal = replaceInProperty(componentResource, target.property, replaceRequest.term, replaceRequest.replacement);
                    if (newVal != null) {
                        Changed ch = new Changed();
                        ch.componentPath = target.componentPath;
                        ch.property = target.property;
                        ch.excerpt = createExcerpt(newVal, replaceRequest.replacement);
                        ch.oldValue = oldVal;
                        ch.newValue = newVal;
                        changedList.add(ch);
                        pageModified = true;
                    } else {
                        throw new IOException("No changes made for property: " + target.property +
                                " in " + target.componentPath + " of " + replaceRequest.page);
                    }
                }
                if (pageModified) {
                    resolver.commit();
                    if (replaceRequest.autoPublish) {
                        LOG.info("Auto‑publishing page: {}", replaceRequest.page);
                        autoPublishPage(pageContentResource);
                    }
                }
                response.setStatus(SlingHttpServletResponse.SC_OK);
                response.setContentType("application/json");
                ReplacePageResponse pageResp = new ReplacePageResponse();
                pageResp.page = replaceRequest.page;
                pageResp.changed = changedList;
                response.getWriter().write(gson.toJson(pageResp));
            } catch (JsonSyntaxException e) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request");
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
     * @param term         the search term, not null
     * @param replacement  the replacement string, not null
     * @return the new property value if a change was made; null otherwise
     */
    private String replaceInProperty(@Nonnull Resource resource, @Nonnull String propertyName,
                                      @Nonnull String term, @Nonnull String replacement) {
        ValueMap properties = resource.adaptTo(ValueMap.class);
        if (properties == null || !properties.containsKey(propertyName)) {
            return null;
        }
        String value = properties.get(propertyName, String.class);
        if (value == null) {
            return null;
        }
        String newValue = value.replaceAll(Pattern.quote(term), replacement);
        if (!newValue.equals(value)) {
            ModifiableValueMap modifiableProperties = resource.adaptTo(ModifiableValueMap.class);
            if (modifiableProperties == null) {
                LOG.warn("Cannot get ModifiableValueMap for resource: {}", resource.getPath());
                return null;
            }
            modifiableProperties.put(propertyName, newValue);
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
     * @param pageResource the page resource to publish, not null
     */
    private void autoPublishPage(@Nonnull Resource pageResource) {
        try {
            // Get the page's content resource to check replication info
            Resource contentResource = pageResource.getChild("jcr:content");
            ValueMap vm = Objects.requireNonNull(contentResource).getValueMap();
            Calendar lastModified = vm.get("cq:lastModified", Calendar.class);
            Calendar lastReplicated = vm.get("cq:lastReplicated", Calendar.class);
            String lastAction = vm.get("cq:lastReplicationAction", String.class);
            // Only auto‑publish if the page qualifies: lastModified is not after lastReplicated and replication was Activate.
            if (lastModified != null && lastReplicated != null
                    && !lastModified.after(lastReplicated)
                    && "Activate".equals(lastAction)) {
                Replicator replicator = pageResource.getResourceResolver().adaptTo(Replicator.class);
                Session session = pageResource.getResourceResolver().adaptTo(Session.class);
                if (replicator != null && session != null) {
                    replicator.replicate(session, ReplicationActionType.ACTIVATE, pageResource.getPath());
                }
            } else {
                LOG.info("Page {} does not qualify for auto‑publication", pageResource.getPath());
            }
        } catch (Exception e) {
            LOG.error("Error auto‑publishing page: {}", pageResource.getPath(), e);
        }
    }

    // --- Static inner classes for response objects ---
    public static class SearchPageResponse {
        public String page;
        public List<Match> matches;
    }

    public static class SummaryResponse {
        public int pages;
        public int matches;
    }

    public static class ReplacePageResponse {
        public String page;
        public List<Changed> changed;
    }

    public static class Match {
        public String componentPath;
        public String property;
        public String excerpt;
    }

    public static class Changed {
        public String componentPath;
        public String property;
        public String excerpt;
        public String oldValue;
        public String newValue;
    }

    public static class ReplaceRequest {
        public String page;
        public String term;
        public String replacement;
        public List<Target> targets;
        public boolean createVersion;
        public boolean autoPublish;
    }

    public static class Target {
        public String componentPath;
        public String property;
    }


}

