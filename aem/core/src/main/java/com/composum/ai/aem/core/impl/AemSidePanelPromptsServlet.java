package com.composum.ai.aem.core.impl;

import static com.composum.ai.aem.core.impl.SelectorUtils.PARAMETER_PATH;
import static com.composum.ai.aem.core.impl.SelectorUtils.findLanguage;
import static com.composum.ai.aem.core.impl.SelectorUtils.replaceLanguagePlaceholder;
import static com.composum.ai.aem.core.impl.SelectorUtils.transformToDatasource;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.DataSource;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * Datasource for the prompts of the sidebar AI dialog.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Content Creation Selectors Servlet",
                "sling.servlet.resourceTypes=composum-ai/servlets/sidepanelaiprompts",
        })
public class AemSidePanelPromptsServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AemSidePanelPromptsServlet.class);

    @Reference
    private AIConfigurationService aiConfigurationService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        String pagePath = request.getParameter(PARAMETER_PATH);
        Resource pageResource = request.getResourceResolver().getResource(pagePath);
        String language = findLanguage(pageResource);

        Map<String, String> prompts = Collections.emptyMap();

        GPTPromptLibrary paths = aiConfigurationService.getGPTPromptLibraryPaths(request, pagePath);
        if (paths != null && paths.sidePanelPromptsPath() != null) {
            String path = paths.sidePanelPromptsPath();
            prompts = aiConfigurationService.getGPTConfigurationMap(request, path, language);
        } else {
            LOG.warn("No content creation prompts path found for page " + pagePath);
        }
        prompts = replaceLanguagePlaceholder(prompts, language);

        DataSource dataSource = transformToDatasource(request, prompts);
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

}
