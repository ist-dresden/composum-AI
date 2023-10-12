package com.composum.ai.backend.slingbase;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * AIConfigurationServlet provides access to AI configurations.
 *
 * <p>This servlet is responsible for determining which AI services are allowed for a given content path and editor URL.
 * It serves as an entry point for clients to understand the restrictions and permissions associated with AI services
 * in the Composum platform.</p>
 *
 * <h2>Endpoint</h2>
 * <p>GET /bin/cpm/ai/config</p>
 *
 * <h2>Parameters</h2>
 * <ul>
 *     <li><b>contentPath</b> (suffix): The path of the content being edited or viewed.</li>
 *     <li><b>editorUrl</b> (query parameter): The URL of the editor in the browser.</li>
 * </ul>
 *
 * <h2>Response</h2>
 * <p>Returns a JSON object with a key "allowedServices" that contains a list of AI services that are allowed
 * for the given content path and editor URL. For example:</p>
 * <pre>
 * {
 *     "allowedServices": {"sidepanel": true, "create": true}
 * }
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>This servlet can be used by frontend components to dynamically adjust the availability of AI features
 * based on the configurations set in the backend. For instance, if a certain content path is restricted from
 * using the AI side panel, the frontend can make a call to this servlet to check the allowed services and
 * hide the side panel accordingly.</p>
 *
 * @see com.composum.ai.backend.slingbase.AIConfigurationService
 */
// http://localhost:4502/bin/cpm/ai/config.json/content/wknd/us/en/magazine/_jcr_content?editorurl=/editor.html/content/wknd/us/en/magazine.html
@Component(service = {Servlet.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Configuration Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/config",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
public class AIConfigurationServlet extends SlingSafeMethodsServlet {

    /**
     * Parameter that gives the editor URL to check the permissions for.
     */
    public static final String PARAM_EDITORURL = "editorUrl";

    /**
     * Content Creation Dialog
     */
    public static final String SERVICE_CREATE = "create";

    /**
     * Side Panel AI
     */
    public static final String SERVICE_SIDEPANEL = "sidepanel";

    /**
     * Only for Composum: translation.
     */
    public static final String SERVICE_TRANSLATE = "translate";

    /**
     * Only for composum: categorization.
     */
    public static final String SERVICE_CATEGORIZE = "categorize";

    @Reference
    private AIConfigurationService aiConfigurationService;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String contentPath = request.getRequestPathInfo().getSuffix();
        String editorUrl = request.getParameter(PARAM_EDITORURL);
        Set<String> allowedServices = aiConfigurationService.allowedServices(request, contentPath, editorUrl);
        Map<String, Boolean> allowedServicesMap = allowedServices.stream()
                .collect(Collectors.toMap(service -> service, service -> true));
        response.setContentType("application/json");
        Map<String, Map<String, Boolean>> jsonResponse = Map.of("allowedServices", allowedServicesMap);
        response.getWriter().write(gson.toJson(jsonResponse));
    }

}
