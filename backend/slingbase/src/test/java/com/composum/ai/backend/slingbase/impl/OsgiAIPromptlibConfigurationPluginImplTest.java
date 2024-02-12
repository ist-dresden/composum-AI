package com.composum.ai.backend.slingbase.impl;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Rule;
import org.junit.Test;

public class OsgiAIPromptlibConfigurationPluginImplTest {

    @Rule
    public SlingContext context =
            new SlingContextBuilder(ResourceResolverType.RESOURCERESOLVER_MOCK).plugin(CACONFIG).build();

    private OsgiAIPromptlibConfigurationPluginImpl service = new OsgiAIPromptlibConfigurationPluginImpl();

    @Test
    public void getGPTConfigurationMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        context.load(true).binaryFile(new ByteArrayInputStream(json.getBytes()), "/content/allowed/jcr:content/path.json");
        Map<String, String> result = service.getGPTConfigurationMap(context.request(), "/content/allowed/jcr:content/path.json", null);
        assertThat(result, not(nullValue()));
        assertThat(result.size(), is(2));
        assertThat(result.get("key1"), is("value1"));
        assertThat(result.get("key2"), is("value2"));
    }

}
