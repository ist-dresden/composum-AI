package com.composum.ai.backend.slingbase.experimential.impl;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.ai.backend.slingbase.experimential.impl.AITemplatingServiceImpl.Replacement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AITemplatingServiceImplTest {

    private AITemplatingServiceImpl aiTemplatingService;

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Before
    public void setUp() {
        aiTemplatingService = new AITemplatingServiceImpl();
    }

    @Test
    public void testCollectPrompts_withValidReplacements() {
        Replacement r1 = new Replacement(null, "property1", "PROMPTFIELD: single sentence invitation to check out the product");
        Replacement r2 = new Replacement(null, "property2", "PROMPTFIELD#ID1: name of the product");
        Replacement r3 = new Replacement(null, "property3", "<p><strong>PROMPTFIELD: markdown list of key features</strong></p>");
        Replacement r4 = new Replacement(null, "property4", "Key Features");
        List<Replacement> replacements = Arrays.asList(r1, r2, r3, r4);

        Map<String, Replacement> ids = new HashMap<>();
        Map<String, String> texts = new LinkedHashMap<>();

        AITemplatingServiceImpl.collectPrompts(replacements, ids, texts);

        ec.checkThat(texts.size(), is(4));
        ec.checkThat(texts.keySet(), allOf(hasItem("PROMPT#001"), hasItem("PROMPT#ID1"), hasItem("PROMPT#002"), hasItem("informationally#003")));

        ec.checkThat(texts.get("PROMPT#001"), is("As plain text: single sentence invitation to check out the product"));
        ec.checkThat(texts.get("PROMPT#ID1"), is("As plain text: name of the product"));
        // we remove html tags at the start of the prompt as that seems ChatGPT to prevent recognizing these as prompts.
        ec.checkThat(texts.get("PROMPT#002"), is("As rich text HTML: markdown list of key features</strong></p>"));
        ec.checkThat(texts.get("informationally#003"), is("Print unchanged the quoted text without the triple backtick quoting: ```Key Features```"));

        ec.checkThat(ids.size(), is(3));
        ec.checkThat(ids.get("PROMPT#001"), is(r1));
        ec.checkThat(ids.get("PROMPT#ID1"), is(r2));
        ec.checkThat(ids.get("PROMPT#002"), is(r3));
    }

    @Test
    public void testCollectPrompts_withDuplicateIds() {
        List<Replacement> replacements = Arrays.asList(
                new Replacement(null, "property1", "PROMPTFIELD#ID1: name of the product"),
                new Replacement(null, "property2", "PROMPTFIELD#ID1: another name of the product")
        );

        Map<String, Replacement> ids = new HashMap<>();
        Map<String, String> texts = new LinkedHashMap<>();

        ec.checkThrows(IllegalArgumentException.class, () -> AITemplatingServiceImpl.collectPrompts(replacements, ids, texts));
    }

    @Test
    public void testCollectSourceUrls() {
        Replacement r1 = new Replacement(null, "property1", "PROMPTFIELD#ID1: bla SOURCEURL(http://www.example.net)");
        Replacement r2 = new Replacement(null, "property2", "PROMPTFIELD: SOURCEURL(http://www.example.com) another name of the product");
        List<Replacement> replacements = Arrays.asList(r1, r2);

        List<String> sourceUrls = AITemplatingServiceImpl.extractSourceUrls(replacements);

        ec.checkThat(sourceUrls.size(), is(2));
        ec.checkThat(sourceUrls, allOf(hasItem("http://www.example.net"), hasItem("http://www.example.com")));

        ec.checkThat(r1.text, is("PROMPTFIELD#ID1: bla "));
        ec.checkThat(r2.text, is("PROMPTFIELD:  another name of the product"));
    }

    @Test
    public void htmlEscapingInJson() {
        Gson gson = new GsonBuilder().create(); // deliberately no disablehtmlescaping
        String json = gson.toJson("<p>a</p>");
        ec.checkThat(json, is("\"\\u003cp\\u003ea\\u003c/p\\u003e\""));
        gson = new GsonBuilder().disableHtmlEscaping().create();
        json = gson.toJson("<p>a</p>");
        ec.checkThat(json, is("\"<p>a</p>\""));
    }

    @Test
    public void joinAndExtract() {
        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("PROMPT#001", "something");
        prompts.put("informationally#002", "As rich text HTML: <p><strong>markdown list of key features</strong></p>");
        String joined = AITemplatingServiceImpl.joinText(prompts);
        Map<String, String> result = AITemplatingServiceImpl.extractParts(joined);
        ec.checkThat(result, is(prompts));
    }

}
