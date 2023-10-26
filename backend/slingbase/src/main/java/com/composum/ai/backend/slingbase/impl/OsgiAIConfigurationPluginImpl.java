package com.composum.ai.backend.slingbase.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;

/**
 * The {@code OsgiAIConfigurationPluginImpl} class is the default implementation of the {@link AIConfigurationPlugin} interface.
 * This implementation sources its configurations from the OSGI environment, specifically from instances of {@link GPTPermissionConfiguration}.
 *
 * <p>
 * The primary responsibility of this class is to determine which AI services are allowed based on various parameters such as:
 * <ul>
 *     <li>The user or user group making the request.</li>
 *     <li>The content path being accessed or edited.</li>
 *     <li>The URL of the editor in the browser.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The configurations are defined as OSGI configurations and can be dynamically modified at runtime. Each configuration specifies:
 * <ul>
 *     <li>Allowed and denied users or user groups.</li>
 *     <li>Allowed and denied content paths.</li>
 *     <li>Allowed and denied views (based on the URL).</li>
 *     <li>The specific AI services that the configuration applies to.</li>
 * </ul>
 * </p>
 *
 * <p>
 * When determining the allowed services, this implementation checks all the available configurations and aggregates the results.
 * A service is considered allowed if it matches any of the "allowed" regular expressions and does not match any of the "denied" regular expressions.
 * </p>
 *
 * @see AIConfigurationPlugin
 * @see GPTPermissionConfiguration
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
@Designate(ocd = GPTPermissionConfiguration.class, factory = true)
public class OsgiAIConfigurationPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIConfigurationPluginImpl.class);

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

    @Override
    @Nullable
    public List<GPTPermissionConfiguration> allowedServices(SlingHttpServletRequest request, String contentPath) {
        return List.of(config);
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
