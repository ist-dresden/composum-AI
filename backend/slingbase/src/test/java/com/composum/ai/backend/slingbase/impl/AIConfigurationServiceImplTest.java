package com.composum.ai.backend.slingbase.impl;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Arrays;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionInfo;

/**
 * "Integration test" for the AI Configuration Service.
 */
public class AIConfigurationServiceImplTest {

    @Rule
    public SlingContext context =
            new SlingContextBuilder(ResourceResolverType.RESOURCERESOLVER_MOCK).plugin(CACONFIG).build();

    private OsgiAIConfigurationPluginImpl osgiAIConfigurationPlugin = new OsgiAIConfigurationPluginImpl();
    private SlingCaConfigPluginImpl slingCaConfigPlugin = new SlingCaConfigPluginImpl();
    private SlingCaConfigPluginImpl.Config caconfig = mock(SlingCaConfigPluginImpl.Config.class);
    private GPTPermissionConfiguration osgiCfg = mock(GPTPermissionConfiguration.class);
    private GPTChatCompletionService chatCompletionService = mock(GPTChatCompletionService.class);

    private Principal principal = mock(Principal.class);
    private UserManager userManager = mock(UserManager.class);

    private AIConfigurationServiceImpl service = new AIConfigurationServiceImpl() {{
        this.plugins = Arrays.asList(slingCaConfigPlugin, osgiAIConfigurationPlugin);
        this.chatCompletionService = AIConfigurationServiceImplTest.this.chatCompletionService;
    }};

    @Before
    public void setup() {
        context.registerAdapter(ResourceResolver.class, UserManager.class, userManager);
        when(principal.getName()).thenReturn("theuser");

        when(osgiCfg.services()).thenReturn(new String[]{"create"});
        when(osgiCfg.allowedPaths()).thenReturn(new String[]{"/content/allowed/.*"});
        when(osgiCfg.deniedPaths()).thenReturn(new String[]{"/content/allowed/denied/.*"});
        when(osgiCfg.allowedUsers()).thenReturn(new String[]{"theuser"});
        when(osgiCfg.allowedViews()).thenReturn(new String[]{".*"});
        when(osgiCfg.allowedComponents()).thenReturn(new String[]{".*"});
        when(osgiCfg.deniedComponents()).thenReturn(new String[0]);
        when(osgiCfg.allowedPageTemplates()).thenReturn(new String[]{".*"});
        when(osgiCfg.deniedPageTemplates()).thenReturn(new String[0]);
        osgiAIConfigurationPlugin.activate(osgiCfg);

        when(caconfig.enabled()).thenReturn(true);
        slingCaConfigPlugin.activate(caconfig);

        when(chatCompletionService.isEnabled(any())).thenReturn(true);
    }

    protected SlingHttpServletRequest getRequest() {
        SlingHttpServletRequest wrapped = Mockito.spy(context.request());
        doReturn(principal).when(wrapped).getUserPrincipal();
        assertThat(wrapped.getUserPrincipal(), is(principal));
        return wrapped;
    }

    @Test
    public void testAllow() {
        context.request().setResource(context.create().resource("/content/allowed/jcr:content/path"));
        GPTPermissionInfo allowed = service.allowedServices(getRequest(), "/content/allowed/jcr:content/path", "whatever");
        assertThat(allowed.getServicePermissions().size(), is((1)));
        assertThat(allowed.getServicePermissions().get(0).getServices(), CoreMatchers.hasItem("create"));
    }

    @Test
    public void testDeny() {
        context.request().setResource(context.create().resource("/content/allowed/denied/jcr:content/path"));
        GPTPermissionInfo allowed = service.allowedServices(getRequest(), "/content/allowed/denied/jcr:content/path", "whatever");
        assertThat(allowed, nullValue());

        context.request().setResource(context.create().resource("/content/other/jcr:content/path"));
        allowed = service.allowedServices(getRequest(), "/content/other/jcr:content/path", null);
        assertThat(allowed, nullValue());
    }

    @Test
    public void testGetConfiguration() {
        context.request().setResource(context.create().resource("/content/allowed/jcr:content/path"));
        String key = "thekey";
        // set up sling configuration for the path with key
        context.create().resource("/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig",
                "openAiApiKey", key);
        GPTConfiguration config = service.getGPTConfiguration(getRequest().getResourceResolver(), "/content/allowed/jcr:content/path");
        assertThat(config, notNullValue());
        assertThat(config.getApiKey(), is(key));
    }

}
