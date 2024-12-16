package com.composum.ai.aem.core.impl.autotranslate;

import static org.junit.jupiter.api.Assertions.assertThrows;
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


    @Test
    public void testParseColumnName() {
        TranslationRuleExtractor extractor = new TranslationRuleExtractor();
        // Test valid column names
        assertEquals(0, extractor.parseColumnName("A"));
        assertEquals(1, extractor.parseColumnName("B"));
        assertEquals(25, extractor.parseColumnName("Z"));
        assertEquals(26, extractor.parseColumnName("AA"));
        assertEquals(27, extractor.parseColumnName("AB"));
        assertEquals(701, extractor.parseColumnName("ZZ"));
        assertEquals(702, extractor.parseColumnName("AAA"));

        // Test valid numeric column names
        assertEquals(0, extractor.parseColumnName("1"));
        assertEquals(1, extractor.parseColumnName("2"));
        assertEquals(25, extractor.parseColumnName("26"));

        // Test invalid column names
        assertThrows(IllegalArgumentException.class, () -> extractor.parseColumnName(""));
        assertThrows(IllegalArgumentException.class, () -> extractor.parseColumnName(null));
        assertThrows(IllegalArgumentException.class, () -> extractor.parseColumnName("1A"));
        assertThrows(IllegalArgumentException.class, () -> extractor.parseColumnName("A1"));
        assertThrows(IllegalArgumentException.class, () -> extractor.parseColumnName("!"));
    }

}
