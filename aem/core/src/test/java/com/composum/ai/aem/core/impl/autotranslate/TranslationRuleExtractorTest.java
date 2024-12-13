package com.composum.ai.aem.core.impl.autotranslate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;

import junit.framework.TestCase;

public class TranslationRuleExtractorTest extends TestCase {

    public void testExtractRules() throws IOException {
        TranslationRuleExtractor extractor = new TranslationRuleExtractor() {
            @Override
            protected InputStream getAssetInputStream(Resource xlsResource) throws NoSuchElementException {
                return getClass().getClassLoader().getResourceAsStream("hpsx/test1.xlsx");
            }
        };
        Map<String, String> rules = extractor.extractRules(null, 0, 1, 0, 1);
        assertEquals(2, rules.size());
    }
}
