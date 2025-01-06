package com.composum.ai.aem.core.impl;

import javax.servlet.Servlet;

import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet with functionality for the AI Translation Merge tool.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Translation Merge Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/config",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
public class AemAITranslationMergeServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AemAITranslationMergeServlet.class);

    // yet empty.

}
