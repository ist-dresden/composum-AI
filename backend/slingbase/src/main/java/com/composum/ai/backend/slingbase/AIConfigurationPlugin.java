package com.composum.ai.backend.slingbase;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

public interface AIConfigurationPlugin {

    /**
     * Determines the allowed services based on the provided request, content path, and editor URL.
     *
     * @param request     The SlingHttpServletRequest representing the current request.
     * @param contentPath The path of the content being edited.
     * @param editorUrl   The URL of the editor in the browser.
     * @return A set of allowed services; null if this plugin doesn't implement this method.
     * @see AIConfigurationServlet#SERVICE_CATEGORIZE
     * @see AIConfigurationServlet#SERVICE_CREATE
     * @see AIConfigurationServlet#SERVICE_SIDEPANEL
     * @see AIConfigurationServlet#SERVICE_TRANSLATE
     */
    @Nullable
    Set<String> allowedServices(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath, @Nonnull String editorUrl);

    /**
     * Reads the GPTConfiguration from sling context aware configurations.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @return if the configuration could be determined we return it, otherwise null.
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    @Nullable
    GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException;

}
