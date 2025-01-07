package com.composum.ai.aem.core.impl;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateMergeService;
import com.day.cq.wcm.api.WCMException;
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

    @Reference
    private AutoTranslateMergeService mergeService;

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

        if (StringUtils.isBlank(path) || StringUtils.isBlank(propertyName) || StringUtils.isBlank(body)) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }

        try {
            mergeService.saveTranslation(resource, propertyName, body, true);
            response.setStatus(SlingHttpServletResponse.SC_NO_CONTENT);
            resolver.commit();
            response.setStatus(SlingHttpServletResponse.SC_NO_CONTENT);
        } catch (PersistenceException | WCMException | IllegalArgumentException e) {
            LOG.error("Error saving property", e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving property");
        }
    }

    protected void handleMerge(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        MergeRequest mergeRequest = gson.fromJson(request.getReader(), MergeRequest.class);
        // if mergeRequest or any of it's properties are null, complain
        if (mergeRequest == null || mergeRequest.path == null || mergeRequest.propertyName == null || mergeRequest.originalSource == null || mergeRequest.newSource == null || mergeRequest.newTranslation == null || mergeRequest.currentText == null) {
            LOG.error("Invalid merge request: {}", mergeRequest);
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(mergeRequest.path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }

        LOG.info("Merging text for path: {}, propertyName: {}, OS: {}, NS: {}, NT: {}, C: {}",
                mergeRequest.path, mergeRequest.propertyName, mergeRequest.originalSource, mergeRequest.newSource, mergeRequest.newTranslation, mergeRequest.currentText);

        String mergedText = mergeService.intelligentMerge(
                resource, mergeRequest.originalSource, mergeRequest.newSource, mergeRequest.newTranslation, mergeRequest.currentText);

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


        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            if (path != null) {
                builder.append("path", path);
            }
            if (propertyName != null) {
                builder.append("propertyName", propertyName);
            }
            if (originalSource != null) {
                builder.append("originalSource", originalSource);
            }
            if (newSource != null) {
                builder.append("newSource", newSource);
            }
            if (newTranslation != null) {
                builder.append("newTranslation", newTranslation);
            }
            if (currentText != null) {
                builder.append("currentText", currentText);
            }
            return builder.toString();
        }
    }

}
