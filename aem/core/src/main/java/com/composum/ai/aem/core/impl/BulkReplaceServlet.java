package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

/**
 * Servlet with functionality for the bulk replace tool.
 * The operations are distinguished by parameter 'operation'. There are:
 * <ul>
 *     <li>search: search for a string in a page tree</li>
 *     <li>replace: replaces a string in a page tree</li>
 * </ul>
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

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String operation = request.getParameter("operation");
        if ("search".equals(operation)) {
            handleSearch(request, response);
        } else if ("replace".equals(operation)) {
            handleReplace(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
        }
    }

    private void handleSearch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String rootPath = request.getParameter("rootPath");
        String term = request.getParameter("term");

        if (StringUtils.isBlank(rootPath) || StringUtils.isBlank(term)) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Required parameters: rootPath, term");
            return;
        }

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
        String componentPath = StringUtils.isNotEmpty(parentPath) ? parentPath + "/" + resource.getName() : resource.getName();

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

    private void handleReplace(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
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
