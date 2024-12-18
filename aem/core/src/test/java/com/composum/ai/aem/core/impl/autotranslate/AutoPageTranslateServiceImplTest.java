package com.composum.ai.aem.core.impl.autotranslate;


import static com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateServiceImpl.compileContentPattern;
import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

import io.wcm.testing.mock.aem.junit5.AemContext;

public class AutoPageTranslateServiceImplTest {

    @Mock
    protected LiveRelationshipManager liveRelationshipManager;
    @Mock
    protected AutoTranslateService autoTranslateService;
    @Mock
    protected AIConfigurationService configurationService;
    @Mock
    protected ConfigurationBuilder configurationBuilder;
    @Mock
    protected AutoTranslateCaConfig config;
    @Mock
    protected GPTTranslationService translationService;
    @Spy
    protected AutoTranslateConfigService autoTranslateConfigService = new AutoTranslateConfigServiceImpl();

    @InjectMocks
    protected AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl();

    @Mock
    protected Resource resource;

    protected AutoCloseable mocks;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(autoTranslateConfigService.isTranslatableResource(any(Resource.class))).thenReturn(true);
    }

    @AfterEach
    public void close() throws Exception {
        mocks.close();
    }


    // problems on /content/wknd/language-masters/es/adventures/beervana-portland/jcr:content : displayPopupTitle true appears in list
    @Test
    public void testCollectPropertiesToTranslate() throws WCMException {
        AemContext context = new AemContext();
        Resource orig = context.create().resource("/content/en/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true");
        Resource origsub = context.create().resource("/content/en/jcr:content/foo", "jcr:title", "This is also to be translated");
        Resource copy = context.create().resource("/content/de/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true");
        Resource copysub = context.create().resource("/content/de/jcr:content/foo", "jcr:title", "This is also to be translated");


        when(liveRelationshipManager.getLiveRelationship(any(), anyBoolean())).then(invocation -> {
            Resource resource = invocation.getArgument(0);
            LiveRelationship liveRelationship = mock(LiveRelationship.class);
            when(liveRelationship.getSourcePath()).thenReturn(resource.getPath().replace("/de/", "/en/"));
            return liveRelationship;
        });

        List<AutoPageTranslateServiceImpl.PropertyToTranslate> props = new java.util.ArrayList<>();
        AutoPageTranslateServiceImpl.Stats stats = new AutoPageTranslateServiceImpl.Stats();
        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        service.collectPropertiesToTranslate(copy, props, stats, parms, false);
        assertEquals(2, props.size());
        assertEquals("something", props.get(0).propertyName);
        assertEquals("/content/de/jcr:content", props.get(0).targetResource.getPath());
        assertEquals("/content/en/jcr:content", props.get(0).sourceResource.getPath());
        assertEquals("jcr:title", props.get(1).propertyName);
        assertEquals("/content/de/jcr:content/foo", props.get(1).targetResource.getPath());
        assertEquals("/content/en/jcr:content/foo", props.get(1).sourceResource.getPath());
    }

    @Test
    public void testCompileContentPattern() {
        assertEquals("a|b", compileContentPattern("a|b").pattern());
        assertEquals("a(b)", compileContentPattern("a(b)").pattern());
        assertEquals("a[b]", compileContentPattern("a[b]").pattern());
        assertEquals("word", compileContentPattern("word").pattern());
        Pattern pattern = compileContentPattern("two   \nwords");
        assertEquals("two\\s+words", pattern.pattern());
        assertTrue(pattern.matcher("two words").matches());
        assertTrue(pattern.matcher("two  words").matches());
        assertTrue(pattern.matcher("two \n words").matches());
        assertTrue(pattern.matcher("tWo  WordS").matches());
    }

    @Test
    public void testTranslate() throws WCMException, PersistenceException {
        AemContext context = new AemContext();

        Resource orig = context.create().resource("/content/en/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true");
        Resource origsub = context.create().resource("/content/en/jcr:content/foo", "jcr:title", "This is also to be translated");
        Resource copy = spy(context.create().resource("/content/de/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true"));
        Resource copysub = context.create().resource("/content/de/jcr:content/foo", "jcr:title", "This is also to be translated");

        when(copy.adaptTo(ConfigurationBuilder.class)).thenReturn(configurationBuilder);
        when(configurationBuilder.as(AutoTranslateCaConfig.class)).thenReturn(config);

        LiveCopy liveCopy = mock(LiveCopy.class);
        when(liveCopy.getBlueprintPath()).thenReturn("/content/en");
        when(liveCopy.getPath()).thenReturn("/content/de");

        when(liveRelationshipManager.getLiveRelationship(any(), anyBoolean())).then(invocation -> {
            Resource resource = invocation.getArgument(0);
            LiveRelationship liveRelationship = mock(LiveRelationship.class);
            doReturn(resource.getPath().replace("/de/", "/en/")).when(liveRelationship).getSourcePath();
            doReturn(liveCopy).when(liveRelationship).getLiveCopy();
            return liveRelationship;
        });

        when(translationService.fragmentedTranslation(any(), any(), any(), any()))
                .then(invocation -> {
                    List<String> texts = invocation.getArgument(0);
                    return texts.stream().map(String::toUpperCase).collect(Collectors.toList());
                });

        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        service.translateLiveCopy(copy, parms);
        assertEquals("THIS IS TO BE TRANSLATED", copy.getValueMap().get("something"));
        assertEquals("THIS IS ALSO TO BE TRANSLATED", copysub.getValueMap().get("jcr:title"));
    }

    @Test
    public void testRemapPaths() {
        AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl();
        assertEquals("Some text containing a <a href=\"/content/livecopy/path/to/resource\">Link</a> here",
                service.remapPaths("Some text containing a <a href=\"/content/blueprint/path/to/resource\">Link</a> here", "/content/blueprint", "/content/livecopy"));
        assertEquals("<a href=\"/content/other/path/to/resource\">Link</a>",
                service.remapPaths("<a href=\"/content/other/path/to/resource\">Link</a>", "/content/blueprint", "/content/livecopy"));
        assertEquals("<a href=\"/content/livecopy/path1\">Link1</a><a href=\"/content/livecopy/path2\">Link2</a>",
                service.remapPaths("<a href=\"/content/blueprint/path1\">Link1</a><a href=\"/content/blueprint/path2\">Link2</a>", "/content/blueprint", "/content/livecopy"));
        assertEquals("<a>Link</a>",
                service.remapPaths("<a>Link</a>", "/content/blueprint", "/content/livecopy"));
        assertEquals("",
                service.remapPaths("", "/content/blueprint", "/content/livecopy"));
        assertEquals(null,
                service.remapPaths((String) null, "/content/blueprint", "/content/livecopy"));
    }

    @Test
    public void expandSelection_includesContextBeforeAndAfter() {
        boolean[] includeIndizes = {false, false, true, false, false};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{true, true, true, true, true}, includeIndizes);
    }

    @Test
    public void expandSelection_noInitialSelection() {
        boolean[] includeIndizes = {false, false, false, false, false};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{false, false, false, false, false}, includeIndizes);
    }

    @Test
    public void expandSelection_singleSelectionAtStart() {
        boolean[] includeIndizes = {true, false, false, false, false};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{true, true, true, false, false}, includeIndizes);
    }

    @Test
    public void expandSelection_singleSelectionAtEnd() {
        boolean[] includeIndizes = {false, false, false, false, true};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{false, false, true, true, true}, includeIndizes);
    }

    @Test
    public void expandSelection_multipleSelections() {
        boolean[] includeIndizes = {false, true, false, true, false};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{true, true, true, true, true}, includeIndizes);
    }

    @Test
    public void expandSelection_long() {
        boolean[] includeIndizes = {false, false, false, true, false, true, false, false, false};
        AutoPageTranslateServiceImpl.expandSelection(includeIndizes, 2);
        assertArrayEquals(new boolean[]{false, true, true, true, true, true, true, true, false}, includeIndizes);
    }

    @Test
    public void testConfigurationOrOverride() {
        AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl();

        // Test when override is null
        assertTrue(service.configurationOrOverride(true, null, null));
        assertFalse(service.configurationOrOverride(false, null, null));

        // Test when override is empty
        assertTrue(service.configurationOrOverride(true, "", null));
        assertFalse(service.configurationOrOverride(false, "", null));

        // Test when override has one element
        assertTrue(service.configurationOrOverride(false, "true", null));
        assertFalse(service.configurationOrOverride(true, "false", null));

        assertTrue(service.configurationOrOverride(false, " tRuE ", null));
        assertFalse(service.configurationOrOverride(true, " FalsE ", null));

        // We take the default on completely broken overrides
        assertTrue(service.configurationOrOverride(true, "what???", null));
        assertFalse(service.configurationOrOverride(false, "what???", null));

    }

    @Test
    public void testRulesTable() {
        AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl() {
            @Override
            protected Map<String, String> getRawRules(AutoTranslateTranslationTableConfig tableConfig, Resource tableResource) {
                Map<String, String> rules = new LinkedHashMap<>();
                rules.put("apple", "Apfel");
                rules.put("egg", "Ei");
                return rules;
            }
        };
        when(config.translationTableRuleText()).thenReturn("Translate '{0}' as '{1}'.");
        AutoTranslateTranslationTableConfig tableConfig = mock(AutoTranslateTranslationTableConfig.class);
        when(tableConfig.path()).thenReturn("/some/path.xlsx");
        when(config.translationTables()).thenReturn(new AutoTranslateTranslationTableConfig[]{tableConfig});
        List<AutoTranslateRuleConfig> rules = service.collectTranslationTables(config, resource);
        assertEquals(2, rules.size());
        assertEquals("TranslationRule{contentPattern=\"apple\", additionalInstructions=\"Translate 'apple' as 'Apfel'.\"}", rules.get(0).toString());
        assertEquals("TranslationRule{contentPattern=\"egg\", additionalInstructions=\"Translate 'egg' as 'Ei'.\"}", rules.get(1).toString());
    }

}
