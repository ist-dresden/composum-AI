package com.composum.ai.backend.slingbase;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionInfo;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

public interface AIConfigurationPlugin {

    /**
     * Determines the allowed services based on the provided request, content path, and editor URL.
     *
     * @param request     The SlingHttpServletRequest representing the current request.
     * @param contentPath The path of the content being edited.
     * @return A set of allowed services; null if this plugin doesn't implement this method.
     * @see GPTPermissionInfo#SERVICE_CATEGORIZE
     * @see GPTPermissionInfo#SERVICE_CREATE
     * @see GPTPermissionInfo#SERVICE_SIDEPANEL
     * @see GPTPermissionInfo#SERVICE_TRANSLATE
     */
    @Nullable
    default List<GPTPermissionConfiguration> allowedServices(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath) {
        return null;
    }

    /**
     * Reads the GPTConfiguration from sling context aware configurations.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @return if the configuration could be determined we return it, otherwise null.
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    @Nullable
    default GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException {
        return null;
    }

    /**
     * Reads the GPTPromptLibrary from sling context aware configurations or OSGI configurations, falling back to default values.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    @Nullable
    default GPTPromptLibrary getGPTPromptLibraryPaths(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) {
        return null;
    }

    /**
     * Returns the default paths for {@link #getGPTPromptLibraryPaths(SlingHttpServletRequest, String)}.
     */
    @Nullable
    default GPTPromptLibrary getGPTPromptLibraryPathsDefault() {
        return null;
    }

    @Nullable
    default Map<String, String> getGPTConfigurationMap(@Nonnull SlingHttpServletRequest request, @Nullable String mapPath) {
        return null;
    }

}
