package com.composum.ai.backend.slingbase.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;

/**
 * This implementation sources its configurations from the OSGI environment, specifically from instances of {@link GPTPermissionConfiguration}.
 * We use a factory to allow multiple instances of the configuration; if there are no configuration entries in the factory we use
 * the (single) configuration of this service as fallback - which might just be the defaults.
 *
 * @see AIConfigurationPlugin
 * @see GPTPermissionConfiguration
 */
@Component(
        configurationPid = "com.composum.ai.backend.slingbase.impl.OsgiAIConfigurationFallback",
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
@Designate(ocd = GPTPermissionConfiguration.class, factory = false)
public class OsgiAIConfigurationPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIConfigurationPluginImpl.class);

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected volatile List<OsgiAIConfigurationPluginFactory> factoryList;

    private GPTPermissionConfiguration fallbackConfig;

    @Activate
    @Modified
    protected void activate(GPTPermissionConfiguration configuration) {
        this.fallbackConfig = configuration;
        LOG.info("Activated with fallback configuration {}", configuration);
    }

    @Deactivate
    protected void deactivate() {
        this.fallbackConfig = null;
        LOG.info("Deactivated.");
    }

    @Override
    @Nullable
    public List<GPTPermissionConfiguration> allowedServices(SlingHttpServletRequest request, String contentPath) {
        if (factoryList != null && !factoryList.isEmpty()) {
            return factoryList.stream()
                    .map(OsgiAIConfigurationPluginFactory::getConfig)
                    .collect(Collectors.toList());
        }
        return List.of(fallbackConfig);
    }

    /**
     * Not implemented here.
     */
    @Nullable
    @Override
    public GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException {
        return null;
    }

}
