package com.composum.ai.aem.core.impl;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

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

    /**
     * Handles POST requests to the servlet.
     *
     * @param request  the Sling HTTP servlet request
     * @param response the Sling HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String operation = request.getParameter("operation");
        if ("save".equals(operation)) {
            handleSearch(request, response);
        } else if ("check".equals(operation)) {
            handleReplace(request, response);
        } else {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
        }
    }

    private void handleSearch(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    }

    private void handleReplace(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    }

}
