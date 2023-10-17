package com.composum.ai.backend.slingbase;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.output.StringBuilderWriter;
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

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;

/**
 * Renders an approximate markdown representation of the text content of a page / resource.
 * The Sling way would be to create markdown.jsp for each component, but that would be quite an effort with all existing
 * Pages components, and since the markdown representation is only for retrieving text for suggesting keywords and
 * summarizing, keywording etc. we just go with a simple approach for now, which just might be good enough.
 * There can be plugins for the markdown conversion: see {@link ApproximateMarkdownServicePlugin}.
 * Can also be used to get the result as richtext by giving a .html suffix instead of .md.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Approximated Markdown Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/approximated",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
// curl -u admin:admin http://localhost:9090/bin/cpm/ai/approximated.md/content/ist/composum/home/platform/_jcr_content
// http://localhost:4502/bin/cpm/ai/approximated.md/content/wknd/us/en/magazine/_jcr_content
public class ApproximateMarkdownServlet extends SlingSafeMethodsServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(ApproximateMarkdownServlet.class);


    @Reference
    ApproximateMarkdownService approximateMarkdownService;

    @Reference
    protected GPTChatCompletionService chatService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        RequestPathInfo info = request.getRequestPathInfo();
        String path = info.getSuffix();
        boolean richtext = "html".equalsIgnoreCase(info.getExtension()) || "htm".equalsIgnoreCase(info.getExtension());
        Resource resource = request.getResourceResolver().getResource(path);
        if (richtext) {
            response.setContentType("text/html");
            StringBuilderWriter writer = new StringBuilderWriter();
            approximateMarkdownService.approximateMarkdown(resource, new PrintWriter(writer), request, response);
            // TODO: integrate that into the ApproximateMarkdownService as we convert to markdown and then back th HTML for richtext, but no time ATM.
            response.getWriter().write(chatService.markdownToHtml(writer.toString()));
        } else {
            response.setContentType("text/plain");
            approximateMarkdownService.approximateMarkdown(resource, response.getWriter(), request, response);
        }
    }

}
