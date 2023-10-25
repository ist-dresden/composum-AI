package com.composum.ai.backend.slingbase.impl;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
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
    @javax.annotation.Nullable
    public Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl) {
        return null;
    }

    @Nullable
    @Override
    public GPTConfiguration getGPTConfiguration(@NotNull SlingHttpServletRequest request, @NotNull String contentPath) throws IllegalArgumentException {
        LOG.debug("getGPTConfiguration({}, {})", request.getResource().getPath(), contentPath);
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

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        OpenAIConfig config = confBuilder.as(OpenAIConfig.class);
        GPTConfiguration result = null;
        if (StringUtils.isNotBlank(config.openAiApiKey())) {
            result = new GPTConfiguration(config.openAiApiKey(), null);
        }
        LOG.debug("found key: {}", result);
        return result;
    }
}
