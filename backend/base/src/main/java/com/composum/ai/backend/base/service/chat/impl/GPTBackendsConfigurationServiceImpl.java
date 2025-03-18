package com.composum.ai.backend.base.service.chat.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import com.composum.ai.backend.base.service.chat.GPTBackendConfiguration;
import com.composum.ai.backend.base.service.chat.GPTBackendsConfigurationService;

/**
 * Provides the backend configurations from OSGI.
 */
@Component
@Designate(ocd = GPTBackendConfiguration.class, factory = true)
public class GPTBackendsConfigurationServiceImpl implements GPTBackendsConfigurationService {

    private GPTBackendConfiguration configuration;

    @Activate
    @Modified
    protected void activate(GPTBackendConfiguration configuration) {
        this.configuration = configuration;
    }

    @Deactivate
    protected void deactivate() {
        this.configuration = null;
    }

    @Nonnull
    @Override
    public List<GPTBackendConfiguration> getBackends() {
        return configuration == null ? Collections.emptyList() : Collections.singletonList(configuration);
    }

    @Nonnull
    @Override
    public List<String> getModelsForBackend(@Nonnull String backendId) {
        if (configuration != null && configuration.backendId().equals(backendId)) {
            return Arrays.asList(configuration.models().split(","));
        }
        return Collections.emptyList();
    }

}
