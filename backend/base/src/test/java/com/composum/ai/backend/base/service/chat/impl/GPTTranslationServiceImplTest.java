package com.composum.ai.backend.base.service.chat.impl;

import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.LASTID;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_END;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_PATTERN;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_START;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.fakeTranslation;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;

import junit.framework.TestCase;

public class GPTTranslationServiceImplTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(GPTTranslationServiceImpl.class);

    @Test
    public void testSeparatorConsistency() {
        assertTrue(MULTITRANSLATION_SEPARATOR_PATTERN.matcher(MULTITRANSLATION_SEPARATOR_START + LASTID + MULTITRANSLATION_SEPARATOR_END).find());
    }

    @Test
    public void testJoinAndSeparate() {
        List<String> texts = asList("text1", "text2", "text3");
        List<String> ids = new ArrayList<>();

        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        List<String> result = GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts);

        assertEquals(texts, result);
    }

    @Test
    public void testJoinAndSeparateWithWhitespace() {
        List<String> texts = asList("", "text1", "", " \n \n ", "text3", "");
        List<String> ids = new ArrayList<>();

        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        List<String> result = GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts);

        assertEquals(texts.stream().map(String::trim).collect(Collectors.toList()), result);
    }

    @Test
    public void testChangedNumber() {
        List<String> texts = asList("text1", "text2", "text3");
        List<String> ids = new ArrayList<>();
        String joinedTexts = GPTTranslationServiceImpl.joinTexts(texts, ids);
        ids.remove(2);
        ids.add("1234");
        Assert.assertThrows(GPTException.class, () -> GPTTranslationServiceImpl.separateResultTexts(joinedTexts, texts, ids, joinedTexts));
    }

    @Test
    public void testLostTexts() {
        List<String> texts = new ArrayList(asList("text1", "text2", "text3"));
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
    public void testSimulateRealTranslation() {
        String result = "\n" +
                "```%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 397360 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "Hallo!\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 319439 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "Guten Morgen\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 424242 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%```";
        List<String> ids = asList("397360", "319439");
        List<String> texts = asList("Hi!", "Good morning");
        List<String> resultTexts = GPTTranslationServiceImpl.separateResultTexts(result, texts, ids, result);
        assertEquals(asList("Hallo!", "Guten Morgen"), resultTexts);
    }

    @Test
    public void testSimulateError() {
        String result = "Äh, %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 357056 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "Äh, Über Uns\n" +
                "Äh, %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 566470 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
        List<String> ids = asList("357056", "566470");
        List<String> texts = asList("About us");
        List<String> resultTexts = GPTTranslationServiceImpl.separateResultTexts(result, texts, ids, result);
        assertEquals(asList("Äh, Über Uns\nÄh,"), resultTexts);
    }

    @Test
    public void testSimulateDifference() {
        // wrong number of % by local model
        String result = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 357056 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "Über uns\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 566470 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "WKND ist eine Gruppe von Outdoor-, Musik-, Handwerk-, Abenteuersport- und Reisebegeisterten, die es sich zum Ziel gesetzt, unsere Erfahrungen, Verbindungen und Expertisen der Welt zu teilen.\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 424242 %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%";
        List<String> ids = asList("357056", "566470");
        List<String> texts = asList("About us", "...");
        List<String> resultTexts = GPTTranslationServiceImpl.separateResultTexts(result, texts, ids, result);
        assertEquals(2, resultTexts.size());
    }

    @Test
    public void testFakeTranslation() {
        assertEquals(null, fakeTranslation(null));
        assertEquals("", fakeTranslation(""));
        String faked = fakeTranslation("This is a test <code>and some Code</code>");
        assertEquals("this is a test <code>and some code</code>", faked.toLowerCase());
        System.out.println(faked); // random case; that's a bit hard to test
    }

    @Test
    public void testFakedFragmentedTranslation() {
        GPTTranslationServiceImpl service = new GPTTranslationServiceImpl();
        GPTTranslationServiceImpl.Config config = mock(GPTTranslationServiceImpl.Config.class);
        when(config.fakeTranslation()).thenReturn(true);
        service.activate(config);
        assertTrue(service.fragmentedTranslation(null, null, null).isEmpty());
        assertTrue(service.fragmentedTranslation(asList(), null, null).isEmpty());
        assertEquals(Arrays.asList("", "\n", "holla", "").toString(), service.fragmentedTranslation(asList("", "\n", "holla", ""), null, null).toString().toLowerCase());
        assertEquals(Arrays.asList("holla", "\nmiau ho ho ").toString(), service.fragmentedTranslation(asList("holla", "\nmiau ho Ho "), null, null).toString().toLowerCase());

        assertEquals(null, service.singleTranslation(null, null, "de",null));
        assertEquals("", service.singleTranslation("", null, "de",null));
        assertEquals("hallo", service.singleTranslation("hallo", null, "de",null).toLowerCase());
        assertEquals("\nhu ho ", service.singleTranslation("\nHu ho ", null, "de",null).toLowerCase());
    }

    @Test
    public void testFragmentedTranslation() {
        GPTTranslationServiceImpl service = new GPTTranslationServiceImpl() {
            @Nullable
            @Override
            public String singleTranslation(@Nullable String rawText, @Nullable String sourceLanguage, @Nullable String targetLanguage, @Nullable GPTConfiguration configuration) {
                assertFalse(rawText.contains("17"));
                return rawText.toUpperCase(Locale.ROOT);
            }
        };
        GPTTranslationServiceImpl.Config config = mock(GPTTranslationServiceImpl.Config.class);
        service.activate(config);
        assertTrue(service.fragmentedTranslation(null, null, null).isEmpty());
        assertTrue(service.fragmentedTranslation(asList(), null, null).isEmpty());
        assertEquals(Arrays.asList("", "\n", "HOLLA", ""), service.fragmentedTranslation(asList("", "\n", "holla", ""), null, null));
        assertEquals(Arrays.asList("HOLLA", "\nMIAU HO HO "), service.fragmentedTranslation(asList("holla", "\nmiau ho Ho "), null, null));
        assertEquals(Arrays.asList("17", "TRUE", "\nMIAU HO HO ", "TRUE", "17"), service.fragmentedTranslation(asList("17", "true", "\nmiau ho Ho ", "true", "17"), null, null));
    }

}
