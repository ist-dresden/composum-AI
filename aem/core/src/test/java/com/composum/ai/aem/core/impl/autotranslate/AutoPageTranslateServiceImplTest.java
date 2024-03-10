package com.composum.ai.aem.core.impl.autotranslate;


import static com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateServiceImpl.isTranslatableProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

import io.wcm.testing.mock.aem.junit5.AemContext;

public class AutoPageTranslateServiceImplTest {

    @Mock
    protected LiveRelationshipManager liveRelationshipManager;
    @Mock
    protected AutoTranslateService autoTranslateService;

    @InjectMocks
    protected AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl();

    protected AutoCloseable mocks;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(autoTranslateService.isTranslatableResource(any(Resource.class))).thenReturn(true);
    }

    @AfterEach
    public void close() throws Exception {
        mocks.close();
    }

    @Test
    public void testIsTranslatableProperty() {
        // Test with certainly translatable properties
        for (String propertyName : AutoPageTranslateServiceImpl.CERTAINLY_TRANSLATABLE_PROPERTIES) {
            assertTrue(isTranslatableProperty(propertyName, "Some value"));
        }

        // Test with property name containing colon
        assertFalse(isTranslatableProperty("property:name", "Some value"));

        // Test with non-string value
        assertFalse(isTranslatableProperty("propertyName", new Object()));

        // Test with string value starting with /content/, /apps/, /libs/, /mnt/
        assertFalse(isTranslatableProperty("propertyName", "/content/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/apps/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/libs/someValue"));
        assertFalse(isTranslatableProperty("propertyName", "/mnt/someValue"));

        // Test with property name starting with AI_PREFIX or LC_PREFIX and ending with AI_TRANSLATED_SUFFIX or AI_ORIGINAL_SUFFIX
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.AI_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.AI_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.LC_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_TRANSLATED_SUFFIX, "Some value"));
        assertFalse(isTranslatableProperty(AITranslatePropertyWrapper.LC_PREFIX + "propertyName" + AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, "Some value"));

        // Test with string value without whitespace and 4 letter sequence
        assertFalse(isTranslatableProperty("propertyName", "abc"));

        // Test with string value with whitespace but without 4 letter sequence
        assertFalse(isTranslatableProperty("propertyName", "a b c"));

        // Test with string value with 4 letter sequence but without whitespace
        assertFalse(isTranslatableProperty("propertyName", "abcd"));

        // Test with string value with whitespace and 4 letter sequence
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh"));


        // Test with string value with multiple whitespace sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // Test with string value with multiple whitespace sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh"));

        // Test with string value with multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd efgh ijkl"));

        // Test with string value with multiple whitespace sequences and multiple 4 letter sequences
        assertTrue(isTranslatableProperty("propertyName", "abcd  efgh  ijkl"));

        // that's likely a boolean property
        assertFalse(isTranslatableProperty("displayPopupTitle", "true"));
        assertFalse(isTranslatableProperty("displayPopupTitle", "false"));

        // dates are not translateable
        assertFalse(isTranslatableProperty("propertyName", "2022-01-01"));
        assertFalse(isTranslatableProperty("propertyName", "2022-01-01T00:00:00.000Z"));
        assertFalse(isTranslatableProperty("propertyName", new Date().toString()));

        // rather do not translate anything that looks like a boolean, even if it's a whitelisted property
        assertFalse(isTranslatableProperty("shortDescription", "true"));
        assertFalse(isTranslatableProperty("jcr:title", "false"));
    }

    // problems on /content/wknd/language-masters/es/adventures/beervana-portland/jcr:content : displayPopupTitle true appears in list
    @Test
    public void testCollectPropertiesToTranslate() throws WCMException {
        AemContext context = new AemContext();
        Resource orig = context.create().resource("/content/en/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true");
        Resource origsub = context.create().resource("/content/en/jcr:content/foo", "jcr:title", "This is also to be translated");
        Resource copy = context.create().resource("/content/de/jcr:content", "something", "This is to be translated", "displayPopupTitle", "true");
        Resource copysub = context.create().resource("/content/de/jcr:content/foo", "jcr:title", "This is also to be translated");


        when(liveRelationshipManager.getLiveRelationship(Mockito.any(), anyBoolean())).then(invocation -> {
            Resource resource = invocation.getArgument(0);
            LiveRelationship liveRelationship = mock(LiveRelationship.class);
            when(liveRelationship.getSourcePath()).thenReturn(resource.getPath().replace("/de/", "/en/"));
            return liveRelationship;
        });

        List<AutoPageTranslateServiceImpl.PropertyToTranslate> props = new java.util.ArrayList<>();
        AutoPageTranslateServiceImpl.Stats stats = new AutoPageTranslateServiceImpl.Stats();
        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        service.collectPropertiesToTranslate(copy, props, stats, parms);
        assertEquals(2, props.size());
        assertEquals("something", props.get(0).propertyName);
        assertEquals("/content/de/jcr:content", props.get(0).targetResource.getPath());
        assertEquals("/content/en/jcr:content", props.get(0).sourceResource.getPath());
        assertEquals("jcr:title", props.get(1).propertyName);
        assertEquals("/content/de/jcr:content/foo", props.get(1).targetResource.getPath());
        assertEquals("/content/en/jcr:content/foo", props.get(1).sourceResource.getPath());
    }

}
