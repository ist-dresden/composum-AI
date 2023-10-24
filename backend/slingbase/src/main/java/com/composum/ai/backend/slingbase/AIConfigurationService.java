package com.composum.ai.backend.slingbase;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

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
     * @return a set of allowed services
     */
    @Nullable
    Set<String> allowedServices(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath, @Nonnull String editorUrl);

    /**
     * Reads the GPTConfiguration from sling context aware configurations.
     *
     * @param request     the request
     * @param contentPath if that's given we read the configuration for this path, otherwise we take the requests path, as long as it starts with /content/
     * @throws IllegalArgumentException if none of the paths is a /content/ path.
     */
    GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath) throws IllegalArgumentException;

}
