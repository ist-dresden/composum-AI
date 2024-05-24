package com.composum.ai.backend.slingbase.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

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

    protected final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

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

    /**
     * {@inheritDoc}
     * This method tries to parse the mapPath as JSON.
     */
    @Nullable
    @Override
    public Map<String, String> getGPTConfigurationMap(@NotNull SlingHttpServletRequest request, @Nullable String mapPath, @Nullable String languageKey) {
        if (mapPath == null || !mapPath.toLowerCase().contains(".json")) {
            return null;
        }
        Resource resource = request.getResourceResolver().getResource(mapPath);
        if (resource == null) {
            return null;
        }
        try (InputStream stream = resource.adaptTo(InputStream.class)) {
            if (stream == null) {
                return null;
            }
            return gson.fromJson(new InputStreamReader(stream), Map.class);
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            LOG.error("Error reading map from {}", mapPath, e);
            return null;
        }
    }
}
