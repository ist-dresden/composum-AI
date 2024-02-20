package com.composum.ai.backend.slingbase;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionInfo;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * This is the primary service interface that provides methods to check which
 * AI services are allowed.
 */
public interface AIConfigurationService {

    /**
     * Method to check which services are allowed, based on the path
     * and editor URL provided.
     *
     * @param request     the SlingHttpServletRequest
     * @param contentPath the content path
     * @param editorUrl   the editor URL
     * @return information about allowed services
     */
    @Nullable
    GPTPermissionInfo allowedServices(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath, @Nonnull String editorUrl);

    /**
     * Reads the GPTConfiguration from sling context aware configurations.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    @Nullable
    GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException;

    /**
     * Reads the {@link GPTPromptLibrary} from sling context aware configurations, falling back to OSGI configurations, falling back to default values.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    @Nullable
    GPTPromptLibrary getGPTPromptLibraryPaths(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException;

    /**
     * Decodes the map from the given mapPath, as determined from the appropriate method of {@link #getGPTPromptLibraryPaths(SlingHttpServletRequest, String)}.
     */
    @Nullable
    Map<String, String> getGPTConfigurationMap(@Nonnull SlingHttpServletRequest request, @Nullable String mapPath, @Nullable String languageCode) throws IllegalArgumentException;

}
