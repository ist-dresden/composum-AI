package com.composum.ai.backend.slingbase.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;

/**
 * Collects {@link GPTPermissionConfiguration}s for {@link OsgiAIConfigurationPluginImpl}.
 *
 * @see GPTPermissionConfiguration
 */
@Component
@Designate(ocd = GPTPermissionConfiguration.class, factory = true)
public class OsgiAIConfigurationPluginFactoryImpl implements OsgiAIConfigurationPluginFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIConfigurationPluginFactoryImpl.class);

    private GPTPermissionConfiguration config;

    @Activate
    @Modified
    protected void activate(GPTPermissionConfiguration configuration) {
        this.config = configuration;
        LOG.info("Activated with configuration {}", configuration);
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
        LOG.info("Deactivated.");
    }

    public GPTPermissionConfiguration getConfig() {
        return config;
    }

}
