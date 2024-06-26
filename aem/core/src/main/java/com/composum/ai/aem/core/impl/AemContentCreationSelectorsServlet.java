package com.composum.ai.aem.core.impl;

import static com.composum.ai.aem.core.impl.SelectorUtils.PARAMETER_PATH;
import static com.composum.ai.aem.core.impl.SelectorUtils.transformToDatasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.ui.components.ds.DataSource;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Servlet that reads the content selectors from a JSON file, adds links in the content and provides that to the dialog.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Content Creation Selectors Servlet",
                "sling.servlet.resourceTypes=composum-ai/servlets/contentcreationselectors",
        })
public class AemContentCreationSelectorsServlet extends SlingSafeMethodsServlet {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * JCR path to a JSON with the basic content selectors supported by the dialog.
     */
    public static final String PATH_CONTENTSELECTORS = "/conf/composum-ai/settings/dialogs/contentcreation/contentselectors.json";

    @Reference
    private ApproximateMarkdownService approximateMarkdownService;

    @Reference
    private GPTChatCompletionService chatCompletionService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        Map<String, String> contentSelectors = readPredefinedContentSelectors(request);
        String path = request.getParameter(PARAMETER_PATH);
        Resource resource = request.getResourceResolver().getResource(path);
        String property = request.getParameter("property");
        if (StringUtils.isNotBlank(property) && resource != null && resource.getChild(property) != null) {
            resource = resource.getChild(property);
        }
        if (resource != null) {
            addContentPaths(resource, contentSelectors);
        }
        DataSource dataSource = transformToDatasource(request, contentSelectors);
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    /**
     * We look for content paths in the component and it's parent. That seems more appropriate than the component itself
     * in AEM - often interesting links are contained one or more levels up, e.g. for text fields in teasers.
     */
    protected void addContentPaths(Resource resource, Map<String, String> contentSelectors) {
        List<ApproximateMarkdownService.Link> componentLinks = approximateMarkdownService.getComponentLinks(resource);
        for (ApproximateMarkdownService.Link link : componentLinks) {
            if (!link.isNeedsVision() || chatCompletionService.isVisionEnabled()) {
                contentSelectors.put(link.getPath(), link.getTitle() + " (" + link.getPath() + ")");
            }
        }
    }

    protected Map<String, String> readPredefinedContentSelectors(SlingHttpServletRequest request) throws IOException {
        Resource resource = request.getResourceResolver().getResource(PATH_CONTENTSELECTORS);
        Map<String, String> contentSelectors;
        try (InputStream in = resource.adaptTo(InputStream.class);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            contentSelectors = gson.fromJson(reader, Map.class);
        }
        return contentSelectors;
    }

}
