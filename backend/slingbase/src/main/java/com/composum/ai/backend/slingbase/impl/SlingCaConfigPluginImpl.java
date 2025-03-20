package com.composum.ai.backend.slingbase.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * Reads configurations using Sling context aware configuration.
 * Higher precedence than {@link OsgiAIConfigurationPluginImpl}.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=1000"
)
@Designate(ocd = SlingCaConfigPluginImpl.Config.class)
public class SlingCaConfigPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(SlingCaConfigPluginImpl.class);
    private boolean enabled;

    @Override
    @Nullable
    public List<GPTPermissionConfiguration> allowedServices(@NotNull SlingHttpServletRequest request, @NotNull String contentPath) {
        if (!enabled) {
            return null;
        }
        LOG.debug("allowedServices({}, {})", request.getResource().getPath(), contentPath);
        Resource resource = determineResource(request.getResourceResolver(), request.getResource(), contentPath);
        if (resource == null) {
            return null;
        }

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        Collection<GPTPermissionConfiguration> configs = confBuilder.asCollection(GPTPermissionConfiguration.class);
        LOG.debug("found configs: {}", configs);
        return configs.isEmpty() ? null : new ArrayList<>(configs);
    }

    @Nullable
    @Override
    public GPTConfiguration getGPTConfiguration(@Nonnull ResourceResolver resourceResolver, @Nonnull String contentPath) throws IllegalArgumentException {
        return null; // currently there is no context specific configuration, but that will likely change.
        /*
        if (!enabled) {
            return null;
        }
        LOG.debug("getGPTConfiguration({}, {})", contentPath);
        Resource resource = determineResource(resourceResolver, null, contentPath);
        if (resource == null) {
            return null;
        }

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        GPTConfiguration result = null;
        return result;
        */
    }

    @Nullable
    @Override
    public GPTPromptLibrary getGPTPromptLibraryPaths(@NotNull SlingHttpServletRequest request, @Nullable String contentPath) {
        if (!enabled || contentPath == null) {
            return null;
        }
        LOG.debug("getGPTPromptLibraryPaths({}, {})", request.getResource().getPath(), contentPath);
        Resource resource = determineResource(request.getResourceResolver(), request.getResource(), contentPath);
        if (resource == null) {
            return null;
        }

        ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
        GPTPromptLibrary config = confBuilder.as(GPTPromptLibrary.class);
        LOG.debug("found config: {}", config);
        return config;
    }

    /**
     * Determines the resource to use for the given request and content path.
     */
    @Nullable
    private static Resource determineResource(@Nonnull ResourceResolver resolver, @Nullable Resource requestResource, @Nonnull String contentPath) {
        Resource resource = requestResource;
        if (StringUtils.isNotBlank(contentPath)) {
            contentPath = contentPath.replace("_jcr_content", "jcr:content");
            resource = resolver.getResource(contentPath);
        }
        if (resource == null) {
            LOG.warn("No resource found for path {}", contentPath);
            return null;
        }
        if (!resource.getPath().startsWith("/content/")) {
            LOG.debug("Path {} is not a /content/ path", resource.getPath());
            return null;
        }
        return resource;
    }

    @Activate
    @Modified
    public void activate(Config config) {
        LOG.info("Activated with configuration {}", config);
        this.enabled = config.enabled();
    }

    @Deactivate
    public void deactivate() {
        LOG.info("Deactivated.");
        this.enabled = false;
    }

    @ObjectClassDefinition(name = "Composum AI SlingCaConfig Plugin", description = "Allows enabling / disabling the Sling Context Aware Configuration of the Composum AI.")
    public @interface Config {

        @AttributeDefinition(name = "Enabled", description = "Whether the Sling Context Aware Configuration of the Composum AI is enabled.")
        boolean enabled() default true;
    }

}
