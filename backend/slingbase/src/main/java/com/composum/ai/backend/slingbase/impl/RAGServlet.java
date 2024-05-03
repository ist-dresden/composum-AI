package com.composum.ai.backend.slingbase.impl;


import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.RAGService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Servlet providing various RAG supported services.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI RAG Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/rag",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })

public class RAGServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RAGServlet.class);

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The actual search query.
     */
    public static final String PARAM_QUERY = "query";

    /**
     * Maximum number of results.
     */
    public static final String PARAM_LIMIT = "limit";

    @Reference
    protected RAGService ragService;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        RequestPathInfo requestInfo = request.getRequestPathInfo();
        if (!"json".equals(requestInfo.getExtension())) {
            throw new ServletException("Only JSON is supported");
        }

        String query = request.getParameter(PARAM_QUERY);
        if (query == null || query.isEmpty()) {
            throw new ServletException("Missing query parameter");
        }

        Resource searchLocation = requestInfo.getSuffixResource();
        if (searchLocation == null) {
            throw new ServletException("Missing search location in suffix");
        }

        int limit = Integer.MAX_VALUE;
        if (request.getParameter(PARAM_LIMIT) != null) {
            limit = Integer.parseInt(request.getParameter(PARAM_LIMIT));
        }

        List<String> selectors = Arrays.asList(requestInfo.getSelectors());
        Object result;
        try {
            if (selectors.contains("related")) {
                result = related(request.getResourceResolver(), searchLocation, query, limit);
            } else {
                throw new ServletException("Unsupported selector: " + selectors);
            }
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
        String json = gson.toJson(result);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    /**
     * Does a plain search for the terms, without actual RAG.
     *  E.g. http://localhost:9090/bin/cpm/ai/rag.related.json/content/ist/composum/home?query=AI
     */
    protected List<?> related(@Nonnull ResourceResolver resourceResolver, @Nonnull Resource searchRoot, @Nonnull String query, int limit) throws RepositoryException {
        List<String> foundPaths = ragService.searchRelated(searchRoot, query, limit);
        // map resources to Map with keys path, title, description from the corresponding fields.
        return foundPaths.stream()
                .map(resourceResolver::getResource)
                .filter(Objects::nonNull)
                .map(r -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("path", r.getPath());
                    ValueMap vm = r.getValueMap();
                    entry.put("title", StringUtils.defaultString(
                            vm.get(JcrConstants.JCR_TITLE, String.class),
                            vm.get("title", String.class)
                    ));
                    entry.put("description", StringUtils.defaultString(
                            vm.get(JcrConstants.JCR_DESCRIPTION, String.class),
                            vm.get("description", String.class)
                    ));
                    return entry;
                })
                .collect(Collectors.toList());
    }
}
