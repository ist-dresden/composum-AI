package com.composum.ai.backend.slingbase.impl;

import javax.annotation.Nonnull;

import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;

/**
 * Collects {@link GPTPermissionConfiguration}s for {@link OsgiAIConfigurationPluginImpl}.
 *
 * @see GPTPermissionConfiguration
 */
public interface OsgiAIConfigurationPluginFactory {

    @Nonnull
    GPTPermissionConfiguration getConfig();

}
