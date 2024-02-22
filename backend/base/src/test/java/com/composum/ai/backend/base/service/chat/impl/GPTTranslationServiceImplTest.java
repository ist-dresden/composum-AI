package com.composum.ai.backend.base.service.chat.impl;

import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.LASTID;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_END;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_PATTERN;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_START;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.fakeTranslation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;

import junit.framework.TestCase;

public class GPTTranslationServiceImplTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(GPTTranslationServiceImpl.class);

    @Test
    public void testSeparatorConsistency() {
        assertTrue(MULTITRANSLATION_SEPARATOR_PATTERN.matcher(MULTITRANSLATION_SEPARATOR_START + LASTID + MULTITRANSLATION_SEPARATOR_END).matches());
    }

    @Test
    public void testJoinAndSeparate() {
        List<String> texts = Arrays.asList("text1", "text2", "text3");
        List<String> ids = new ArrayList<>();

        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        List<String> result = GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts);

        assertEquals(texts, result);
    }

    @Test
    public void testJoinAndSeparateWithWhitespace() {
        List<String> texts = Arrays.asList("", "text1", "", " \n \n ", "text3", "");
        List<String> ids = new ArrayList<>();

        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        List<String> result = GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts);

        assertEquals(texts.stream().map(String::trim).collect(Collectors.toList()), result);
    }

    @Test
    public void testChangedNumber() {
        List<String> texts = Arrays.asList("text1", "text2", "text3");
        List<String> ids = new ArrayList<>();
        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        ids.remove(2);
        ids.add("1234");
        Assert.assertThrows(GPTException.class, () -> GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts));
    }

    @Test
    public void testLostTexts() {
        List<String> texts = new ArrayList(Arrays.asList("text1", "text2", "text3"));
        List<String> ids = new ArrayList<>();
        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        texts.add("text4");
        ids.add("1234");
        Assert.assertThrows(GPTException.class, () -> GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts));
    }

    /**
     * Something from a real result.
     */
    @Test
    public void testRealTranslation() {
        String result = "\n" +
                "```\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 397360 %%%%%%%%%%%%%%%%\n" +
                "Hallo!\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 319439 %%%%%%%%%%%%%%%%\n" +
                "Guten Morgen\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 424242 %%%%%%%%%%%%%%%%\n" +
                "```";
        List<String> ids = Arrays.asList("397360", "319439");
        List<String> texts = Arrays.asList("Hi!", "Good morning");
        List<String> resultTexts = GPTTranslationServiceImpl.separateResultTexts(result, texts, ids, result);
        assertEquals(Arrays.asList("Hallo!", "Guten Morgen"), resultTexts);
    }

    @Test
    public void testFakeTranslation() {
        assertEquals(null, fakeTranslation(null));
        assertEquals("", fakeTranslation(""));
        assertEquals("THiS iS a tEsT <code>aNd sOmE COdE</code>", fakeTranslation("This is a test <code>and some Code</code>"));
    }

}
