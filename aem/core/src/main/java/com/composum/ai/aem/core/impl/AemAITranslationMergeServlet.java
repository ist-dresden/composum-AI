package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

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
 * The operations are distinguished by parameter 'operation'. There are:
 * <ul>
 *     <li>save: save a translation</li>
 *     <li>check: check if a resource has unmerged translations</li>
 *     <li>merge: merge translations</li>
 * </ul>
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

    /**
     * Handles POST requests to the servlet.
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     * @see AemAITranslationMergeServlet#handleSave(SlingHttpServletRequest, SlingHttpServletResponse)
     * @see AemAITranslationMergeServlet#handleCheck(SlingHttpServletRequest, SlingHttpServletResponse)
     * @see AemAITranslationMergeServlet#handleMerge(SlingHttpServletRequest, SlingHttpServletResponse)
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String operation = request.getParameter("operation");
        if ("save".equals(operation)) {
            handleSave(request, response);
        } else if ("check".equals(operation)) {
            handleCheck(request, response);
        } else if ("merge".equals(operation)) {
            handleMerge(request, response);
        } else if ("acceptTranslation".equals(operation)) {
            handleAcceptTranslation(request, response);
        } else if ("cancelInheritance".equals(operation)) {
            handleChangeInheritance(request, response, AutoTranslateMergeService.CancelOrReenable.CANCEL);
        } else if ("reenableInheritance".equals(operation)) {
            handleChangeInheritance(request, response, AutoTranslateMergeService.CancelOrReenable.REENABLE);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
        }
    }

    /**
     * Handles the 'check' operation to verify if a resource has unmerged translations.
     * <p>
     * Request parameters:
     * - `path`: the path of the resource to check for unmerged translations (required).
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    protected void handleCheck(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");
        if (StringUtils.isBlank(path)) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameter path");
            return;
        }
        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
            return;
        }
        List<AutoTranslateMergeService.AutoTranslateProperty> props = mergeService.getProperties(resource);
        boolean hasUnmerged = props != null && !props.isEmpty();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(gson.toJson(Collections.singletonMap("mergeable", hasUnmerged)));
    }

    /**
     * Handles the 'save' operation to save a translation.
     * <p>
     * Request parameters:
     * - `path`: the path of the resource to save the translation to (required).
     * - `propertyName`: the name of the property to save the translation to (required).
     * - `body`: the translation text to save (required).
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    protected void handleSave(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException, ServletException {
        SaveRequest saveRequest = gson.fromJson(request.getReader(), SaveRequest.class);

        if (StringUtils.isBlank(saveRequest.path) || StringUtils.isBlank(saveRequest.propertyName) || StringUtils.isBlank(saveRequest.body)) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(saveRequest.path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found: " + saveRequest.path);
            return;
        }

        try {
            Map<String, String> result = mergeService.saveTranslation(resource, saveRequest.propertyName, saveRequest.body, true);
            resolver.commit();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(gson.toJson(result));
        } catch (PersistenceException | WCMException | RuntimeException e) {
            LOG.error("Error saving property {} on {}", saveRequest.propertyName, saveRequest.path, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving property " + saveRequest.propertyName + " on resource " + saveRequest.path);
            throw new ServletException(e);
        }
    }

    /**
     * Handles the 'merge' operation to merge translations.
     * <p>
     * Request parameters:
     * - `path`: the path of the resource to merge the translation for (required).
     * - `propertyName`: the name of the property to merge the translation for (required).
     * - `originalSource`: the original source text (required).
     * - `newSource`: the new source text (required).
     * - `newTranslation`: the new translation text (required).
     * - `currentText`: the current text of the component (required).
     * - `language`: the language of the translation (optional).
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
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
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found: " + mergeRequest.path);
            return;
        }

        LOG.info("Merging text for path: {}, propertyName: {}, OS: {}, NS: {}, NT: {}, C: {}",
                mergeRequest.path, mergeRequest.propertyName, mergeRequest.originalSource, mergeRequest.newSource, mergeRequest.newTranslation, mergeRequest.currentText);

        String mergedText = mergeService.intelligentMerge(mergeRequest.language,
                resource, mergeRequest.originalSource, mergeRequest.newSource, mergeRequest.newTranslation, mergeRequest.currentText);

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(mergedText);
    }

    /**
     * Handles the "approve" operation of the servlet.
     * <p>
     * Request parameters:
     * - `path`: the path of the resource to approve the translation for (required).
     * - `propertyName`: the name of the property to approve the translation for (required).
     * </p>
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    protected void handleAcceptTranslation(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        CancellationRequest cancellationRequest = gson.fromJson(request.getReader(), CancellationRequest.class);
        if (cancellationRequest == null || cancellationRequest.path == null || cancellationRequest.propertyName == null) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(cancellationRequest.path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found: " + cancellationRequest.path);
            return;
        }

        try {
            mergeService.approveTranslation(resource, cancellationRequest.propertyName);
            resolver.commit();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(gson.toJson(Collections.singletonMap("accepted", true)));
        } catch (PersistenceException | WCMException | RuntimeException e) {
            LOG.error("Error approving property {} on {}", cancellationRequest.propertyName, cancellationRequest.path, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error approving property " + cancellationRequest.propertyName + " on resource " + cancellationRequest.path);
        }
    }

    /**
     * Handles the "cancelInheritance" operation of the servlet.
     * <p>
     * Request parameters:
     * - `path`: the path of the resource to cancel inheritance for (required).
     * - `propertyName`: the name of the property to cancel inheritance for - optional, if not provided, inheritance is cancelled for the whole component. That's the normal case - normally only page properties are cancelled individually.
     * </p>
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @param kind     whether to cancel or re-enable inheritance
     * @throws IOException if an I/O error occurs
     */
    protected void handleChangeInheritance(SlingHttpServletRequest request, SlingHttpServletResponse response, AutoTranslateMergeService.CancelOrReenable kind) throws IOException {
        CancellationRequest cancellationRequest = gson.fromJson(request.getReader(), CancellationRequest.class);
        if (cancellationRequest == null || cancellationRequest.path == null) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(cancellationRequest.path);
        if (resource == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND, "Resource not found: " + cancellationRequest.path);
            return;
        }

        try {
            mergeService.changeInheritance(resource, cancellationRequest.propertyName, kind);
            resolver.commit();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(gson.toJson(Collections.singletonMap("done", true)));
        } catch (PersistenceException | RuntimeException | WCMException e) {
            LOG.error("Error cancelling inheritance for property {} on {}", cancellationRequest.propertyName, cancellationRequest.path, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error cancelling inheritance for property " + cancellationRequest.propertyName + " on resource " + cancellationRequest.path);
        }
    }

    private static class SaveRequest {
        String path;
        String propertyName;
        String body;

        @Override
        public String toString() {
            return "SaveRequest{" +
                    "path='" + path + '\'' +
                    ", propertyName='" + propertyName + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    private static class MergeRequest {
        String path;
        String propertyName;
        String originalSource;
        String newSource;
        String newTranslation;
        String currentText;
        String language;


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
            if (language != null) {
                builder.append("targetLanguage", language);
            }
            return builder.toString();
        }
    }

    private static class CancellationRequest {
        String path;
        String propertyName;

        @Override
        public String toString() {
            return "CancellationRequest{" +
                    "path='" + path + '\'' +
                    ", propertyName='" + propertyName + '\'' +
                    '}';
        }
    }
}
