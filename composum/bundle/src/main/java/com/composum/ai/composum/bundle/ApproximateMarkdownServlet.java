package com.composum.ai.composum.bundle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an approximate markdown representation of the text content of a page / resource.
 * The Sling way would be to create markdown.jsp for each component, but that would be quite an effort with all existing
 * Pages components, and since the markdown representation is only for retrieving text for suggesting keywords and
 * summarizing, keywording etc. we just go with a simple approach for now, which just might be good enough.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Approximated Markdown Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/platform/ai/approximated.markdown",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
// curl -u admin:admin http://localhost:9090/bin/cpm/platform/ai/approximated.markdown.md/content/ist/composum/home/platform/_jcr_content
public class ApproximateMarkdownServlet extends SlingSafeMethodsServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServlet.class);

    @Reference
    ApproximateMarkdownService approximateMarkdownService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        RequestPathInfo info = request.getRequestPathInfo();
        String path = info.getSuffix();
        Resource resource = request.getResourceResolver().getResource(path);
        response.setContentType("text/plain");
        try (Writer w = response.getWriter();
             PrintWriter out = new PrintWriter(w)) {
            approximateMarkdownService.approximateMarkdown(resource, out);
        }
    }

}
