package com.composum.ai.aem.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

/**
 * Abstract class for servlets that handle selectors.
 */
public class AbstractSelectorsServlet extends SlingSafeMethodsServlet {


    /**
     * The keys of the map are the displayed texts, the values the value for the selects.
     */
    protected static DataSource transformToDatasource(SlingHttpServletRequest request, Map<String, String> contentSelectors) {
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
        return dataSource;
    }
}
