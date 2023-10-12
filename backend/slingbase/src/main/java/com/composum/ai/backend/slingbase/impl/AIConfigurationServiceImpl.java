package com.composum.ai.backend.slingbase.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.AIConfigurationService;

/**
 * The default implementation of the AIConfigurationService.
 */
@Component(service = AIConfigurationService.class)
public class AIConfigurationServiceImpl implements AIConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(AIConfigurationServiceImpl.class);

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    private volatile List<AIConfigurationPlugin> plugins;

    /**
     * Union of the plugin's results.
     */
    @Override
    public Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl) {
        Set<String> allowedServices = new HashSet<>();
        for (AIConfigurationPlugin plugin : plugins) {
            try {
                Set<String> services = plugin.allowedServices(request, contentPath, editorUrl);
                allowedServices.addAll(services);
                if (!services.isEmpty()) {
                    LOG.info("Plugin {} allowed services {}", plugin.getClass(), services);
                }
            } catch (Exception e) {
                LOG.error("Error in AIConfigurationPlugin with {}", plugin.getClass(), e);
            }
        }
        return allowedServices;
    }
}
