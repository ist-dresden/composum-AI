package com.composum.ai.backend.slingbase.model;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

public class OpenAIConfigTest {

    @Rule
    public SlingContext context =
            new SlingContextBuilder(ResourceResolverType.RESOURCERESOLVER_MOCK).plugin(CACONFIG).build();

    @Test
    public void testOpenAIConfig() {
        context.create().resource("/conf/test/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openaikey", "test-api-key");
        context.create().resource("/content/test", "sling:configRef", "/conf/test");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        OpenAIConfig config = configurationBuilder.as(OpenAIConfig.class);

        assertEquals("test-api-key", config.openaikey());
    }

    @Test
    public void testGlobalFallback() {
        context.create().resource("/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openaikey", "test-api-key-global");
        context.create().resource("/content/test", "sling:configRef", "/conf/test");

        Resource resource = context.resourceResolver().getResource("/content/test");
        ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
        OpenAIConfig config = configurationBuilder.as(OpenAIConfig.class);

        assertEquals("test-api-key-global", config.openaikey());
    }

}
