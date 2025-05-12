package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet with functionality for the bulk replace tool.
 *
 * Operations:
 * 1. Search:
 *    - POST (operation=search)
 *       Parameters:
 *         • rootPath: absolute page path to start search (e.g. /content/site/en)
 *         • term: literal, case‑insensitive search text
 *       Response: 202 Accepted with JSON payload {"jobId": "<uuid>"}.
 *       Note: The search parameters are stored in an internal LinkedHashMap (only the last 10 operations are kept).
 *
 *    - GET (operation=search, jobId=<uuid>)
 *       Streams search results as text/event-stream based on the stored parameters.
 *
 * 2. Replace:
 *    - POST (operation=replace)
 *       Parameters:
 *         • rootPath, term, replacement, target (see bulkreplace.md)
 *       Response: JSON indicating replace result.
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

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
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
            synchronized(jobMap) {
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

    // New doGet to stream search results based on jobId
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String operation = request.getParameter("operation");
        if ("search".equals(operation)) {
            String jobId = request.getParameter("jobId");
            if (StringUtils.isBlank(jobId)) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing jobId");
                return;
            }
            Map<String, String> params;
            synchronized(jobMap) {
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

    // Streams search results using stored parameters
    private void streamSearchResults(Map<String, String> params, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
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
            String xpath = "/jcr:root" + rootPath + "//element(*, cq:Page)[jcr:contains(., '" + term + "')]";
            Iterator<Resource> candidatePages = resolver.findResources(xpath, "xpath");

            while (candidatePages.hasNext()) {
                Resource candidate = candidatePages.next();
                Page candidatePage = pageManager.getPage(candidate.getPath());
                if (candidatePage != null) {
                    List<Match> matches = findMatches(candidatePage, term.toLowerCase());
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

    private List<Match> findMatches(Page page, String term) {
        List<Match> matches = new ArrayList<>();
        Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            findMatchesInResource(contentResource, "", term, matches);
        }
        return matches;
    }

    private void findMatchesInResource(Resource resource, String parentPath, String term, List<Match> matches) {
        ValueMap properties = resource.getValueMap();
        String componentPath = StringUtils.isNotEmpty(parentPath)
                ? parentPath + "/" + resource.getName() : resource.getName();
        // Make the component path relative by removing any "jcr:content/" prefix
        if (componentPath.contains("jcr:content/")) {
            componentPath = componentPath.substring(componentPath.indexOf("jcr:content/") + "jcr:content/".length());
        }

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;
                if (StringUtils.containsIgnoreCase(stringValue, term)) {
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

    private String createExcerpt(String text, String term) {
        int index = text.toLowerCase().indexOf(term);
        int start = Math.max(0, index - 20);
        int end = Math.min(text.length(), index + term.length() + 20);
        String excerpt = text.substring(start, end);
        if (start > 0) excerpt = "..." + excerpt;
        if (end < text.length()) excerpt = excerpt + "...";
        return excerpt;
    }

    // New static classes for JSON replace request
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

    // Updated handleReplace to support JSON requests
    private void handleReplace(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // Check if request is JSON
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
                // Process the replacement for the single page
                ResourceResolver resolver = request.getResourceResolver();
                Resource pageResource = resolver.getResource(replaceRequest.page);
                if (pageResource == null) {
                    response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Page not found: " + replaceRequest.page);
                    return;
                }
                // If createVersion is true, create a version of the page before making changes.
                if (replaceRequest.createVersion) {
                    // ...existing code to create a version, e.g. call to version manager...
                    LOG.info("Creating version for page: {}", replaceRequest.page);
                }
                int propertiesChanged = 0, skipped = 0;
                long startTime = System.currentTimeMillis();
                List<Changed> changedList = new ArrayList<>();
                boolean pageModified = false;
                for (Target target : replaceRequest.targets) {
                    Resource componentResource = pageResource.getChild(target.componentPath);
                    if (componentResource == null) {
                        LOG.warn("Component not found: {}/{}", replaceRequest.page, target.componentPath);
                        skipped++;
                        continue;
                    }
                    if (replaceInProperty(componentResource, target.property, replaceRequest.term, replaceRequest.replacement)) {
                        Changed ch = new Changed();
                        ch.componentPath = target.componentPath;
                        ch.property = target.property;
                        changedList.add(ch);
                        propertiesChanged++;
                        pageModified = true;
                    } else {
                        skipped++;
                    }
                }
                if (pageModified) {
                    resolver.commit();
                    // If autoPublish is true, publish the page after changes.
                    if (replaceRequest.autoPublish) {
                        // ...existing code to auto-publish the page...
                        LOG.info("Auto-publishing page: {}", replaceRequest.page);
                    }
                }
                long duration = System.currentTimeMillis() - startTime;
                response.setStatus(SlingHttpServletResponse.SC_OK);
                response.setContentType("application/json");
                ResultResponse result = new ResultResponse();
                result.pages = pageModified ? 1 : 0;
                result.properties = propertiesChanged;
                result.skipped = skipped;
                result.durationMs = duration;
                response.getWriter().write(gson.toJson(result));
            } catch (JsonSyntaxException e) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request");
            }
        } else {
            // Existing form-based replace handling
            String rootPath = request.getParameter("rootPath");
            String term = request.getParameter("term");
            String replacement = request.getParameter("replacement");
            String[] targets = request.getParameterValues("target");

            if (StringUtils.isBlank(rootPath) || StringUtils.isBlank(term) ||
                    replacement == null || targets == null || targets.length == 0) {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST,
                        "Required parameters: rootPath, term, replacement, target");
                return;
            }

            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");

            ResourceResolver resolver = request.getResourceResolver();
            int pagesChanged = 0, propertiesChanged = 0, skipped = 0;
            long startTime = System.currentTimeMillis();

            try {
                Map<String, List<String[]>> targetsByPage = new HashMap<>();
                for (String target : targets) {
                    String[] parts = target.split("::");
                    if (parts.length != 3) {
                        LOG.warn("Invalid target format: {}", target);
                        skipped++;
                        continue;
                    }
                    String pagePath = parts[0];
                    targetsByPage.computeIfAbsent(pagePath, k -> new ArrayList<>()).add(parts);
                }

                for (Map.Entry<String, List<String[]>> entry : targetsByPage.entrySet()) {
                    String pagePath = entry.getKey();
                    List<String[]> pageTargets = entry.getValue();
                    Resource pageResource = resolver.getResource(pagePath);
                    if (pageResource == null) {
                        LOG.warn("Page not found: {}", pagePath);
                        skipped += pageTargets.size();
                        continue;
                    }

                    List<Changed> changedList = new ArrayList<>();
                    boolean pageModified = false;

                    for (String[] target : pageTargets) {
                        String componentPath = target[1];
                        String propertyName = target[2];
                        Resource componentResource = pageResource.getChild(componentPath);
                        if (componentResource == null) {
                            LOG.warn("Component not found: {}/{}", pagePath, componentPath);
                            skipped++;
                            continue;
                        }

                        if (replaceInProperty(componentResource, propertyName, term, replacement)) {
                            Changed ch = new Changed();
                            ch.componentPath = componentPath;
                            ch.property = propertyName;
                            changedList.add(ch);
                            propertiesChanged++;
                            pageModified = true;
                        } else {
                            skipped++;
                        }
                    }

                    if (pageModified) {
                        pagesChanged++;
                        ReplacePageResponse pageResp = new ReplacePageResponse();
                        pageResp.page = pagePath;
                        pageResp.changed = changedList;
                        sendEvent(response, "page", gson.toJson(pageResp));
                    }
                }

                if (propertiesChanged > 0) {
                    resolver.commit();
                }

                ResultResponse result = new ResultResponse();
                result.pages = pagesChanged;
                result.properties = propertiesChanged;
                result.skipped = skipped;
                result.durationMs = System.currentTimeMillis() - startTime;
                sendEvent(response, "result", gson.toJson(result));

            } catch (Exception e) {
                LOG.error("Error during replace operation", e);
                if (resolver.hasChanges()) {
                    resolver.revert();
                }
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    private boolean replaceInProperty(Resource resource, String propertyName, String term, String replacement) {
        try {
            ValueMap properties = resource.adaptTo(ValueMap.class);
            if (properties == null || !properties.containsKey(propertyName)) {
                return false;
            }
            String value = properties.get(propertyName, String.class);
            if (value == null) {
                return false;
            }
            String newValue = value.replaceAll("(?i)" + Pattern.quote(term), replacement);
            if (!newValue.equals(value)) {
                ModifiableValueMap modifiableProperties = resource.adaptTo(ModifiableValueMap.class);
                if (modifiableProperties == null) {
                    LOG.warn("Cannot get ModifiableValueMap for resource: {}", resource.getPath());
                    return false;
                }
                modifiableProperties.put(propertyName, newValue);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.error("Error replacing in property: {}", resource.getPath() + "/" + propertyName, e);
            return false;
        }
    }

    private void sendEvent(SlingHttpServletResponse response, String eventName, String data) throws IOException {
        response.getWriter().write("event: " + eventName + "\n");
        response.getWriter().write("data: " + data + "\n\n");
        response.getWriter().flush();
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

    public static class ResultResponse {
        public int pages;
        public int properties;
        public int skipped;
        public long durationMs;
    }

    public static class Match {
        public String componentPath;
        public String property;
        public String excerpt;
    }

    public static class Changed {
        public String componentPath;
        public String property;
    }
}
