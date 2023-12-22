package com.composum.ai.aem.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.google.gson.Gson;

/**
 * Servlet that reads the content selectors from a JSON file, adds some things and returns that JSON.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Content Creation Selectors Servlet",
                "sling.servlet.resourceTypes=composum-ai/servlets/contentcreationselectors",
        })
public class ContentCreationSelectorsServlet extends SlingSafeMethodsServlet {

    private final Gson gson = new Gson();

    /**
     * JCR path to a JSON with the basic content selectors supported by the dialog.
     */
    public static final String PATH_CONTENTSELECTORS = "/conf/composum-ai/settings/dialogs/contentcreation/contentselectors.json";

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        Resource resource = request.getResourceResolver().getResource(PATH_CONTENTSELECTORS);
        Map<String, String> contentSelectors;
        try (InputStream in = resource.adaptTo(InputStream.class);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            contentSelectors = gson.fromJson(reader, Map.class);
        }
        List<Resource> resourceList = contentSelectors.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> values = new HashMap<>();
                    values.put("value", entry.getKey());
                    values.put("text", entry.getValue());
                    ValueMap valueMap = new ValueMapDecorator(values);
                    return new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured", valueMap);
                })
                .collect(Collectors.toList());
        DataSource dataSource = new SimpleDataSource(resourceList.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}
