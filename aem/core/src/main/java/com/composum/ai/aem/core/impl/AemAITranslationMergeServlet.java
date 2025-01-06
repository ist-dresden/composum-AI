package com.composum.ai.aem.core.impl;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Servlet with functionality for the AI Translation Merge tool.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Translation Merge Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/aitranslationmerge",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class AemAITranslationMergeServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AemAITranslationMergeServlet.class);

    public static final Gson gson = new Gson();

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String operation = request.getParameter("operation");
        if ("save".equals(operation)) {
            handleSave(request, response);
        } else if ("merge".equals(operation)) {
            handleMerge(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
        }
    }

    protected void handleSave(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");
        String propertyName = request.getParameter("propertyName");
        String body = request.getParameter("body");

        // TODO: Implement the save functionality
        LOG.info("Saving text for path: {}, propertyName: {}, body: {}", path, propertyName, body);

        response.setStatus(SlingHttpServletResponse.SC_NO_CONTENT);
    }

    protected void handleMerge(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        MergeRequest mergeRequest = gson.fromJson(request.getReader(), MergeRequest.class);

        // TODO: Implement the merge functionality
        LOG.info("Merging text for path: {}, propertyName: {}, OS: {}, NS: {}, NT: {}, C: {}",
                mergeRequest.path, mergeRequest.propertyName, mergeRequest.originalSource, mergeRequest.newSource, mergeRequest.newTranslation, mergeRequest.currentText);

        // For now, just return the current text as the merged result
        String mergedText = mergeRequest.currentText + " TODO MERGE";

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(mergedText);
    }

    private static class MergeRequest {
        String path;
        String propertyName;
        String originalSource;
        String newSource;
        String newTranslation;
        String currentText;
    }

}
