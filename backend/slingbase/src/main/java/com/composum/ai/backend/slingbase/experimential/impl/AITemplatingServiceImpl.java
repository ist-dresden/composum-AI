package com.composum.ai.backend.slingbase.experimential.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.IterableUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.experimential.AITemplatingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


/**
 * This implementation uses the markers:
 * <ul>
 *   <li>A field that is a prompt begins with <code>PROMPTFIELD: </code></li>
 *   <li>A field that is referenced by other fields begins with <code>PROMPTFIELD#ID: </code> where ID is a unique identifier for the field</li>
 *   <li>A URL source is added as <code>SOURCEURL(https://example.com/)</code> after that.</li>
 *   <li>A prompt that applies to the whole page can be put into a multi line field; it begins on a line with <code>PAGEPROMPT: </code></li>
 * </ul>
 */
// for a structure like
// - Title: "PROMPTFIELD#NAME: name of the product"
//- Text: "PROMPTFIELD: single sentence invitation to check out the product"
//- Title: "Key Features"
//- Text: "PROMPTFIELD: markdown list of key features"
//- Title: "PROMPTFIELD: 'Why ' and the content of field #NAME'
//- Text: "PROMPTFIELD: call to action"
// we generate a JSON like {
//  "#NAME": "name of the product",
//  "#TEXT1": "single sentence invitation to check out the product",
//  "informationally": "Key Features",
//  "#TEXT2": "markdown list of key features",
//  "informationally": "Call to action",
//  "#TEXT3": "Why ' and the content of field #NAME",
//  "#TEXT4": "call to action"
//}
@Component
public class AITemplatingServiceImpl implements AITemplatingService {

    /**
     * Property prefix: if a property contains a prompt, it's copied into a property with this prefix so that we can redo and undo the process.
     */
    public static final String PROPERTY_PREFIX_PROMPT = "ai_prompt_";

    /**
     * A pattern for property names which we ignore. We only care about String properties that have whitespace, anyway.
     */
    protected static final Pattern IGNORED_PROPERTIES = Pattern.compile("^ai_prompt_.*");

    protected static final Pattern HAS_WHITESPACE = Pattern.compile("\\s");

    /**
     * Matches a text with PROMPTFIELD start and determines the id if there is one given. Either it's
     * <code>PROMPTFIELD: ...</code> or <code>PROMPTFIELD#ID: ...</code>.
     */
    protected static final Pattern PROMPTFIELD = Pattern.compile("^\\s*(<p>)?\\s*PROMPTFIELD(?<id>#\\w+)?:");

    /* A prefix for keys that says this is not a prompt but a text that can be used to analyze the flow of the page. */
    protected static final String PREFIX_INFORMATIONALLY = "informationally";

    /**
     * Pattern for a <code>SOURCEURL(https://example.com/)</code> to extract the URL - group "url".
     */
    protected static final Pattern SOURCEURL = Pattern.compile("SOURCEURL\\((?<url>[^)]+)\\)");

    /**
     * Pattern for a PAGEPROMPT: ... - all lines to the end of the field are in group "url".
     */
    protected static final Pattern PAGEPROMPT = Pattern.compile("^PAGEPROMPT:(?<prompt>.*)$", Pattern.MULTILINE);

    protected static final String SYSMSG = "You are a professional content writer / editor. Generate text according to the prompt, and then print it without any additional comments. Do not mention the prompt or the text or the act of text retrieval. Use the style and tone with which the input text is written, if not required otherwise. Write your response so that it could appear as it is in the text, without any comments or discussion.\n";

    /**
     * The prompt that is used for the generation. TODO: should probably later be in some resource / configurable.
     */
    protected static final String GENERATIONPROMPT_START = "" +
            "The following JSON contains prompts for parts of a complete text that should be generated with " +
            "the information of these retrieved URL(s), which might be referred to as the source." +
            "Output the JSON with the prompts replaced by the corresponding texts, " +
            "but leave the \"informationally\" items unchanged.\n" +
            "\n" +
            "```json\n";

    protected static final String GENERATIONPROMPT_END = "```";

    protected static final Type TYPE_MAP_STRING_STRING = new TypeToken<Map<String, String>>() {
    }.getType();

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Logger LOG = LoggerFactory.getLogger(AITemplatingServiceImpl.class);

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    @Reference
    protected ApproximateMarkdownService markdownService;

    @Reference
    protected AIConfigurationService configurationService;

    protected Resource normalize(Resource resource) {
        if (resource.getPath().contains("/" + JcrConstants.JCR_CONTENT)) {
            return resource;
        }
        if (resource.getChild(JcrConstants.JCR_CONTENT) != null) {
            return resource.getChild(JcrConstants.JCR_CONTENT);
        }
        throw new IllegalArgumentException("For safety reasons, we only work on one jcr:content resource or subresource.");
    }

    @Override
    public boolean replacePromptsInResource(Resource resource, String additionalPrompt, List<URI> additionalUrls)
            throws URISyntaxException, IOException {
        resource = normalize(resource);
        List<Replacement> replacements = descendantsStream(resource).flatMap(descendant ->
                descendant.getValueMap().entrySet().stream()
                        .filter(entry -> !IGNORED_PROPERTIES.matcher(entry.getKey()).matches())
                        .filter(entry -> entry.getValue() instanceof String)
                        .filter(entry -> HAS_WHITESPACE.matcher((String) entry.getValue()).find())
                        .map(entry -> new Replacement(descendant, entry.getKey(), (String) entry.getValue()))
        ).collect(Collectors.toList());
        if (replacements.isEmpty()) return false;

        Map<String, Replacement> ids = new HashMap<>();
        Map<String, String> texts = new LinkedHashMap<>(); // we want to keep the ordering of the texts!

        collectTexts(replacements, ids, texts);

        List<String> urls = extractSourceUrls(replacements);
        if (additionalUrls != null) {
            for (URI additionalUrl : additionalUrls) {
                urls.add(additionalUrl.toString());
            }
        }
        List<String> pagePrompts = extractPagePrompts(replacements);

        GPTChatRequest request = new GPTChatRequest();
        GPTConfiguration config = configurationService.getGPTConfiguration(resource.getResourceResolver(), resource.getPath());
        request.setConfiguration(GPTConfiguration.JSON.merge(config));
        request.addMessage(GPTMessageRole.SYSTEM, SYSMSG);

        for (String url : urls) {
            String markdown = markdownService.getMarkdown(new URI(url));
            request.addMessage(GPTMessageRole.USER,
                    "Please retrieve as background information the text content of URL `" + url + "`");
            request.addMessage(GPTMessageRole.ASSISTANT, markdown);
        }
        StringBuilder prompt = new StringBuilder();
        for (String pagePrompt : pagePrompts) {
            prompt.append(pagePrompt).append("\n\n");
        }
        prompt.append(GENERATIONPROMPT_START);
        prompt.append(gson.toJson(texts));
        prompt.append(GENERATIONPROMPT_END);
        request.addMessage(GPTMessageRole.USER, prompt.toString());

        String response = chatCompletionService.getSingleChatCompletion(request);
        Map<String, String> responses = gson.fromJson(response, TYPE_MAP_STRING_STRING);

        for (Map.Entry<String, String> entry : responses.entrySet()) {
            Replacement replacement = ids.get(entry.getKey());
            if (entry.getKey().startsWith(PREFIX_INFORMATIONALLY)) {
                continue;
            } else if (replacement == null) { // retry? For now, we give up.
                throw new IllegalStateException("The response contains a key that was not in the prompts: " + entry.getKey());
            }
            ModifiableValueMap properties = replacement.resource.adaptTo(ModifiableValueMap.class);
            properties.put(PROPERTY_PREFIX_PROMPT + replacement.property, properties.get(replacement.property));
            properties.put(replacement.property, entry.getValue());
        }
        return true;
    }

    private static void collectTexts(List<Replacement> replacements, Map<String, Replacement> ids, Map<String, String> texts) {
        AtomicInteger counter = new AtomicInteger();
        for (Replacement replacement : replacements) {
            Matcher idmatch = PROMPTFIELD.matcher(replacement.text);
            String id;
            if (idmatch.find()) {
                String name = idmatch.group("id");
                id = name != null ? name : "#TEXT" + String.valueOf(1000 + counter.incrementAndGet()).substring(1);
            } else {
                id = PREFIX_INFORMATIONALLY + String.valueOf(1000 + counter.incrementAndGet()).substring(1);
            }
            if (ids.containsKey(id)) {
                LOG.error("The resource contains a declaration for the key {} twice: one at {} and one at {}", id, ids.get(id), replacement);
                throw new IllegalArgumentException("The resource contains a declaration for the key " + id + " twice.");
            }
            ids.put(id, replacement);
            texts.put(id, replacement.text);
        }
    }

    protected static List<String> extractPagePrompts(List<Replacement> replacements) {
        List<String> pagePrompts = new ArrayList<>();
        for (Replacement replacement : replacements) {
            Matcher pageprompt = PAGEPROMPT.matcher(replacement.text);
            if (pageprompt.find()) {
                pagePrompts.add(pageprompt.group("prompt"));
                replacement.text = replacement.text.replaceFirst(PAGEPROMPT.pattern(), "");
            }
        }
        return pagePrompts;
    }

    protected static List<String> extractSourceUrls(List<Replacement> replacements) {
        List<String> urls = new ArrayList<>();
        for (Replacement replacement : replacements) {
            Matcher sourceurl = SOURCEURL.matcher(replacement.text);
            while (sourceurl.find()) {
                urls.add(sourceurl.group("url"));
                // remove the URL from the text
                replacement.text = replacement.text.replaceFirst(SOURCEURL.pattern(), "");
                sourceurl = SOURCEURL.matcher(replacement.text);
            }
        }
        return urls;
    }

    /**
     * Sets all properties from its backup copies starting with {@link #PROPERTY_PREFIX_PROMPT}
     * and removes the backups.
     *
     * @return
     */
    @Override
    public boolean resetToPrompts(Resource resource) throws PersistenceException {
        resource = normalize(resource);
        AtomicBoolean changed = new AtomicBoolean(false);
        descendantsStream(resource).forEach(descendant -> {
            ModifiableValueMap properties = descendant.adaptTo(ModifiableValueMap.class);
            List<String> backupKeys = properties.keySet().stream()
                    .filter(key -> key.startsWith(PROPERTY_PREFIX_PROMPT))
                    .collect(Collectors.toList());
            for (String key : backupKeys) {
                String originalKey = key.substring(PROPERTY_PREFIX_PROMPT.length());
                properties.put(originalKey, properties.get(key));
                properties.remove(key);
                changed.set(true);
            }
        });
        return changed.get();
    }

    protected Stream<Resource> descendantsStream(Resource resource) {
        List<Resource> children = IterableUtils.toList(resource.getChildren());
        return Stream.concat(
                Stream.of(resource),
                children.stream().flatMap(this::descendantsStream));
    }

    protected static class Replacement {
        public final Resource resource;
        public final String property;
        public String text;

        public Replacement(Resource resource, String property, String text) {
            this.resource = resource;
            this.property = property;
            this.text = text;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Replacement{");
            sb.append("resource=").append(resource.getPath());
            sb.append(", property='").append(property).append('\'');
            // too much information: sb.append(", prompt='").append(prompt).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

}
