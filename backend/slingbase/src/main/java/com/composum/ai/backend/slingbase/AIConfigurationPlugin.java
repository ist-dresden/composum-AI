package com.composum.ai.backend.slingbase;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Set;

public interface AIConfigurationPlugin {

    /**
     * Determines the allowed services based on the provided request, content path, and editor URL.
     *
     * @param request The SlingHttpServletRequest representing the current request.
     * @param contentPath The path of the content being edited.
     * @param editorUrl The URL of the editor in the browser.
     * @return A set of allowed services.
     */
    Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl);

}
