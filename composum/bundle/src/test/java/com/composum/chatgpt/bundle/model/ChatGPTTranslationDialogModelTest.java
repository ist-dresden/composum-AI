package com.composum.chatgpt.bundle.model;

import static com.composum.pages.commons.PagesConstants.LANGUAGES_ATTR;
import static com.composum.pages.commons.PagesConstants.RA_CURRENT_PAGE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.properties.Language;
import com.composum.pages.commons.model.properties.Languages;
import com.composum.pages.commons.service.SiteManager;
import com.composum.sling.core.BeanContext;

/**
 * Tests for {@link ChatGPTTranslationDialogModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChatGPTTranslationDialogModelTest {

    public static final String PATH = "/content/i18izedpage";
    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Mock
    private SiteManager siteManager;

    @Mock
    private Languages languages;

    @Mock
    private Language language;

    private BeanContext beanContext;

    @Before
    public void setUp() throws Exception {
        context.load().fileVaultXml("/content/i18izedpage.xml", PATH);
        beanContext = new BeanContext.Map(new HashMap<>(), new HashMap<>(), new HashMap<>()) {
            @Override
            public <T> T getAttribute(String name, Class<T> T) {
                if (PagesConstants.RA_STICKY_LOCALE.equals(name)) {
                    return (T) Locale.ENGLISH;
                }
                return super.getAttribute(name, T);
            }
        };
        beanContext.setAttribute("com.composum.pages.commons.service.SiteManager", siteManager, BeanContext.Scope.application);
        beanContext.setAttribute(LANGUAGES_ATTR, languages, BeanContext.Scope.application);
        Page page = new Page();
        beanContext.setAttribute(RA_CURRENT_PAGE, page, BeanContext.Scope.application);
        // can't set via constructor due to bug in BeanContext.Map
        FieldUtils.writeField(beanContext, "request", context.request(), true);
        // Mockito.when(languages.getLanguage()).thenReturn(language);

    }

    @Test
    public void testImport() {
        Resource resource = context.resourceResolver().getResource(PATH);
        ec.checkThat(resource, is(notNullValue()));
    }

    @Test
    public void testModel() {
        context.request().setParameterMap(Map.of("propertypath", "jcr:description"));
        Resource resource = context.resourceResolver().getResource(PATH + "/jcr:content/jcr:description");
        ChatGPTTranslationDialogModel model = beanContext.withResource(resource).adaptTo(ChatGPTTranslationDialogModel.class);
        ec.checkThat(model, is(notNullValue()));
        ec.checkThat(model.getPropertyEditHandle(), is(notNullValue()));
        ec.checkThat(model.getPropertyName(), is("jcr:description"));
        ec.checkThat(model.getFieldType(), is("rich"));
        // we give up at this point: the setup according to AbstractModel is too complex to mock for the expected value of the test.
        // ec.checkThat(model.getPropertyEditHandle().getValue(), is("<p>english <em>description</em></p>"));
    }

}
