package com.composum.ai.aem.core.impl.autotranslate;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

import io.wcm.testing.mock.aem.junit5.AemContext;

public class AutoPageTranslateServiceImplTest {

    @Mock
    protected LiveRelationshipManager liveRelationshipManager;
    @Mock
    protected AutoTranslateService autoTranslateService;
    @Spy
    protected AutoTranslateConfigService autoTranslateConfigService = new AutoTranslateConfigServiceImpl();

    @InjectMocks
    protected AutoPageTranslateServiceImpl service = new AutoPageTranslateServiceImpl();

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


        when(liveRelationshipManager.getLiveRelationship(Mockito.any(), anyBoolean())).then(invocation -> {
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

}
