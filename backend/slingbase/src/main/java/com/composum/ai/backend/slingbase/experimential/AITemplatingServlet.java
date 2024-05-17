package com.composum.ai.backend.slingbase.experimential;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

/**
 * Servlet providing services related to templating.
 * Exports the methods of {@link AITemplatingService} via a servlet.
 * URL is /bin/cpm/ai/experimental/templating.{methodname}.json
 * (Apache Sling conform), POST only, returns JSON,
 * currently only a string message telling whether it was successful. All parameters of the methods are passed
 * HTML form style (application/x-www-form-urlencoded).
 *
 * @see "10PageTemplating.md"
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Templating Servlet (experimental)",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/experimental/templating",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class AITemplatingServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AITemplatingServlet.class);

    @Reference
    private AITemplatingService aiTemplatingService;

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    protected enum Method {
        replacePromptsInResource, resetToPrompts
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String method = request.getRequestPathInfo().getSelectorString();
        switch (Method.valueOf(method)) {
            case replacePromptsInResource:
                replacePromptsInResource(request, response);
                break;
            case resetToPrompts:
                resetToPrompts(request, response);
                break;
            default:
                response.setStatus(404);
        }
    }

    protected void replacePromptsInResource(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String resourcePath = request.getParameter("resourcePath");
        String additionalPrompt = request.getParameter("additionalPrompt");
        List<URI> additionalUrls = Stream.of(request.getParameterValues("additionalUrls"))
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(URI::create)
                .collect(Collectors.toList());
        try (JsonWriter writer = gson.newJsonWriter(response.getWriter())) {
            try {
                Resource resource = request.getResourceResolver().getResource(resourcePath);
                boolean changed = aiTemplatingService.replacePromptsInResource(resource, additionalPrompt, additionalUrls);
                writeToResponse(writer, response, true, changed, (changed ? "Successfully " : "No changes made: ") +
                        "replacing prompts in resource " + resourcePath);
            } catch (Exception e) {
                LOG.error("Error replacing prompts in resource " + resourcePath, e);
                writeToResponse(writer, response, false, false, "Error replacing prompts in resource " + resourcePath + ": " + e);
            }
        }
    }

    protected void writeToResponse(JsonWriter writer, SlingHttpServletResponse response, boolean success, boolean changes, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("changes").value(changes);
        writer.name("message").value(message);
        writer.endObject();
        writer.flush();
    }

    protected void resetToPrompts(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String resourcePath = request.getParameter("resourcePath");
        try (JsonWriter writer = gson.newJsonWriter(response.getWriter())) {
            try {
                Resource resource = request.getResourceResolver().getResource(resourcePath);
                boolean changed = aiTemplatingService.resetToPrompts(resource);
                writeToResponse(writer, response, true, changed, (changed ? "Successfully " : "No changes made: ") +
                        "reset resource " + resourcePath + " to prompts");
            } catch (Exception e) {
                LOG.error("Error resetting resource " + resourcePath + " to prompts", e);
                writeToResponse(writer, response, false, false, "Error resetting resource " + resourcePath + " to prompts: " + e);
            }
        }
    }
}
