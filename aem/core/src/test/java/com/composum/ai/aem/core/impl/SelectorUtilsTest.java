package com.composum.ai.aem.core.impl;

import static com.composum.ai.aem.core.impl.SelectorUtils.getLanguageName;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;

public class SelectorUtilsTest extends TestCase {

    @Test
    public void testGetLanguageName() {
        assertEquals("English", getLanguageName("en"));
        assertEquals("Deutsch", getLanguageName("de"));
        assertEquals("Deutsch", getLanguageName("de_DE"));
        assertEquals("English", getLanguageName("en_CA"));
        assertEquals("français", getLanguageName("fr_CA"));
    }

}
