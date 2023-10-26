package com.composum.ai.backend.slingbase.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;
import com.composum.ai.backend.slingbase.model.OpenAIConfig;

/**
 * Reads configurations using Sling context aware configuration.
 * Higher precedence than {@link OsgiAIConfigurationPluginImpl}.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=1000"
)
public class SlingCaConfigPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(SlingCaConfigPluginImpl.class);

    @Override
    @Nullable
    public List<GPTPermissionConfiguration> allowedServices(SlingHttpServletRequest request, String contentPath) {
        LOG.debug("allowedServices({}, {})", request.getResource().getPath(), contentPath);
        Resource resource = determineResource(request, contentPath);

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        Collection<GPTPermissionConfiguration> configs = confBuilder.asCollection(GPTPermissionConfiguration.class);
        LOG.debug("found configs: {}", configs);
        return configs.isEmpty() ? null : List.copyOf(configs);
    }

    @Nullable
    @Override
    public GPTConfiguration getGPTConfiguration(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath) throws IllegalArgumentException {
        LOG.debug("getGPTConfiguration({}, {})", request.getResource().getPath(), contentPath);
        Resource resource = determineResource(request, contentPath);

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        OpenAIConfig config = confBuilder.as(OpenAIConfig.class);
        GPTConfiguration result = null;
        if (StringUtils.isNotBlank(config.openAiApiKey())) {
            result = new GPTConfiguration(config.openAiApiKey(), null);
        }
        LOG.debug("found key: {}", result);
        return result;
    }

    @Nonnull
    private static Resource determineResource(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath) {
        Resource resource = request.getResource();
        if (StringUtils.isNotBlank(contentPath)) {
            resource = request.getResourceResolver().getResource(contentPath);
        }
        if (resource == null) {
            throw new IllegalArgumentException("No resource found for path " + contentPath);
        }
        if (!resource.getPath().startsWith("/content/")) {
            throw new IllegalArgumentException("Path " + resource.getPath() + " is not a /content/ path");
        }
        return resource;
    }
}
