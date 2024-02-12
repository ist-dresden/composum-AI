package com.composum.ai.backend.slingbase.impl;

import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * This implementation sources the global GPTPromptLibrary configuration from the OSGI environment.
 *
 * @see AIConfigurationPlugin
 * @see GPTPromptLibrary
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
@Designate(ocd = GPTPromptLibrary.class)
public class OsgiAIPromptlibConfigurationPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIPromptlibConfigurationPluginImpl.class);

    private GPTPromptLibrary config;

    @Activate
    @Modified
    protected void activate(GPTPromptLibrary configuration) {
        this.config = configuration;
        LOG.info("Activated with configuration {}", configuration);
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
        LOG.info("Deactivated.");
    }

    @Nullable
    @Override
    public GPTPromptLibrary getGPTPromptLibraryPaths(@NotNull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException {
        return config;
    }

}
