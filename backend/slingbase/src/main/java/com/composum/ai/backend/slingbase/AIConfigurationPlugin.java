package com.composum.ai.backend.slingbase;

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;

public interface AIConfigurationPlugin {

    /**
     * Determines the allowed services based on the provided request, content path, and editor URL.
     *
     * @param request     The SlingHttpServletRequest representing the current request.
     * @param contentPath The path of the content being edited.
     * @param editorUrl   The URL of the editor in the browser.
     * @return A set of allowed services.
     * @see AIConfigurationServlet#SERVICE_CATEGORIZE
     * @see AIConfigurationServlet#SERVICE_CREATE
     * @see AIConfigurationServlet#SERVICE_SIDEPANEL
     * @see AIConfigurationServlet#SERVICE_TRANSLATE
     */
    @Nonnull
    Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl);

}
