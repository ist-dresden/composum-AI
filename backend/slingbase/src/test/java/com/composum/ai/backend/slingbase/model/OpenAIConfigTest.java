package com.composum.ai.backend.slingbase.model;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Rule;
import org.junit.Test;

public class OpenAIConfigTest {

    @Rule
    public SlingContext context =
            new SlingContextBuilder(ResourceResolverType.RESOURCERESOLVER_MOCK).plugin(CACONFIG).build();

    @Test
    public void testOpenAIConfig() {
        context.create().resource("/conf/test/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openAiApiKey", "test-api-key");
        context.create().resource("/content/test", "sling:configRef", "/conf/test");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        OpenAIConfig config = configurationBuilder.as(OpenAIConfig.class);

        assertEquals("test-api-key", config.openAiApiKey());
    }

    @Test
    public void testOpenAIConfigDirectDoesNotWork() {
        // not supported by Sling CA Config :-/
        context.create().resource("/content/test/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openAiApiKey", "test-api-key");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        OpenAIConfig config = configurationBuilder.as(OpenAIConfig.class);

        assertNull(config.openAiApiKey());
    }

    @Test
    public void testGlobalFallback() {
        context.create().resource("/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openAiApiKey", "test-api-key-global");
        // context.create().resource("/content/test", "sling:configRef", "/conf/test");
        context.create().resource("/content/test");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        OpenAIConfig config = configurationBuilder.as(OpenAIConfig.class);

        assertEquals("test-api-key-global", config.openAiApiKey());
    }

    /**
     * Just a demonstration how collections work; not actually used so far.
     */
    @Test
    public void testOpenAIConfigAsCollection() {
        context.create().resource("/conf/test/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig/test1",
                "openAiApiKey", "test-api-key-1");
        context.create().resource("/conf/test/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig/test2",
                "openAiApiKey", "test-api-key-2");
        context.create().resource("/content/test", "sling:configRef", "/conf/test");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        Collection<OpenAIConfig> configs = configurationBuilder.asCollection(OpenAIConfig.class);

        List<String> apiKeys = configs.stream().map(OpenAIConfig::openAiApiKey).collect(Collectors.toList());
        assertTrue(apiKeys.contains("test-api-key-1"));
        assertTrue(apiKeys.contains("test-api-key-2"));
    }

}
