package com.composum.ai.aem.core.impl.autotranslate;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

public class TranslationRuleExtractorTest extends TestCase {

    @Mock
    private Resource xlsResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExtractRulesFromXSLX() throws IOException {
        TranslationRuleExtractor extractor = getTranslationRuleExtractor("translationtables/testtranslationtable.xlsx");
        Map<String, String> rules1 = extractor.extractRules(xlsResource, 1, 1, "A", "B");
        assertEquals("{crossroads=Kreuzung, traffic lights=Ampel}", String.valueOf(rules1));
        assertEquals(2, rules1.size());

        Map<String, String> rules2 = extractor.extractRules(xlsResource, 2, 3, "C", "B");
        assertEquals("{Apple=Apfel, Egg=Ei}", String.valueOf(rules2));
        assertEquals(2, rules2.size());
    }

    @Test
    public void testExtractRulesFromCsv() throws IOException {
        TranslationRuleExtractor extractor = getTranslationRuleExtractor("translationtables/testtranslationtable.csv");
        Map<String, String> rules = extractor.extractRules(xlsResource, 1, 3, "C", "B");
        assertEquals("{Apple=Apfel, Egg=Ei}", String.valueOf(rules));
        assertEquals(2, rules.size());
    }

    private @NotNull TranslationRuleExtractor getTranslationRuleExtractor(String filename) {
        when(xlsResource.getPath()).thenReturn(filename);
        TranslationRuleExtractor extractor = new TranslationRuleExtractor() {
            @Override
            protected InputStream getAssetInputStream(Resource xlsResource) throws NoSuchElementException {
                return getClass().getClassLoader().getResourceAsStream(filename);
            }
        };
        return extractor;
    }

}
