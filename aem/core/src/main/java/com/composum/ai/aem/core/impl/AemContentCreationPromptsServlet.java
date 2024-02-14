package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;
import com.google.common.collect.ImmutableMap;

/**
 * Datasource for the prompts of the content creation dialog.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Content Creation Selectors Servlet",
                "sling.servlet.resourceTypes=composum-ai/servlets/contentcreationprompts",
        })
public class AemContentCreationPromptsServlet extends AbstractSelectorsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AemContentCreationPromptsServlet.class);

    @Reference
    private AIConfigurationService aiConfigurationService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        String pagePath = request.getRequestPathInfo().getSuffix();
        String language = null;
        Resource pageResource = request.getResourceResolver().getResource(pagePath);
        while (pageResource != null && language == null) {
            language = pageResource.getValueMap().get(JcrConstants.JCR_LANGUAGE, String.class);
            pageResource = pageResource.getParent();
        }

        Map<String, String> prompts = new HashMap<>();

        GPTPromptLibrary paths = aiConfigurationService.getGPTPromptLibraryPaths(request, pagePath);
        if (paths != null && paths.contentCreationPromptsPath() != null) {
            String path = paths.contentCreationPromptsPath();
            prompts = aiConfigurationService.getGPTConfigurationMap(request, path, language);
        }

        DataSource dataSource = transformToDatasource(request, prompts);
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
