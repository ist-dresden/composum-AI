package com.composum.ai.backend.slingbase;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Set;

/**
 * This is the primary service interface that provides methods to check which
 * AI services are allowed.
 */
public interface AIConfigurationService {

    /**
     * Method to check which services are allowed, based on the path
     * and editor URL provided.
     *
     * @param request    the SlingHttpServletRequest
     * @param contentPath the content path
     * @param editorUrl   the editor URL
     * @return a set of allowed services
     */
    Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl);

}