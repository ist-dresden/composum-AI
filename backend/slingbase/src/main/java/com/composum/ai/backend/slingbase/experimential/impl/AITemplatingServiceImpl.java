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

import javax.annotation.Nonnull;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
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
    protected static final Pattern PROMPTFIELD = Pattern.compile("^\\s*(?<prefix>(<\\w+>\\s*)*)PROMPTFIELD(?<id>#\\w+)?:\\s*");

    /* A prefix for keys that says this is not a prompt but a text that can be used to analyze the flow of the page. */
    protected static final String PREFIX_INFORMATIONALLY = "informationally#";

    /**
     * Pattern for a <code>SOURCEURL(https://example.com/)</code> to extract the URL - group "url".
     */
    protected static final Pattern SOURCEURL = Pattern.compile("SOURCEURL\\((?<url>[^)]+)\\)");

    /**
     * Pattern for a PAGEPROMPT: ... - all lines to the end of the field are in group "url".
     */
    protected static final Pattern PAGEPROMPT = Pattern.compile("PAGEPROMPT:(?<prompt>.*)$", Pattern.DOTALL);

    /**
     * Heuristics to identify richtext properties: start and end with a HTML tag.
     */
    protected static final Pattern RICHTEXT_PATTERN = Pattern.compile("\\s*<\\s*\\w+\\s*>.*</\\s*\\w+\\s*>\\s*", Pattern.DOTALL);

    protected static final String SYSMSG = "You are a professional content writer / editor.\n" +
            "Generate text according to the prompt, and then print it without any additional comments.\n" +
            "Do not mention the prompt or the text or the act of text retrieval.\n" +
            "Do NEVER EVER repeat the prompt!\n" +
            "Write your responses so that they could appear as they are in a text, without any comments or discussion.";

    protected static final String PREFIX_PROMPT = "Create a text for a web page that is based on the retrieved information according to the following instructions which are separated into several parts. The separators like \"%%%%%%%% ID %%%%%%%%\" should be printed as they are, to structure both the output and the instructions.\n\n";

    protected static final Pattern SEPARATOR_PATTERN = Pattern.compile("(?m)^\\s*%{6,10}\\s*(?<id>\\S+)\\s*%{6,10}$");

    protected static final String THE_END_COMMAND = "Print as plain text: 'end of page' in parentheses";

    /**
     * Matches a properly executed {@link #THE_END_COMMAND}.
     */
    protected static final Pattern THE_END_PATTERN = Pattern.compile("\\(\\s*end of page\\s*\\)");


    protected static final Type TYPE_MAP_STRING_STRING = new TypeToken<Map<String, String>>() {
    }.getType();

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    protected static final Logger LOG = LoggerFactory.getLogger(AITemplatingServiceImpl.class);

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
        List<Replacement> replacements = collectPossibleReplacements(resource);
        if (replacements.isEmpty()) return false;

        Map<String, Replacement> ids = new HashMap<>();
        Map<String, String> texts = new LinkedHashMap<>(); // we want to keep the ordering of the texts!

        List<String> urls = extractSourceUrls(replacements);
        if (additionalUrls != null) {
            additionalUrls.stream().map(URI::toString).forEach(urls::add);
        }
        List<String> pagePrompts = extractPagePrompts(replacements); // before collectPrompts since it removes the page prompts

        collectPrompts(replacements, ids, texts);
        texts.put("END", THE_END_COMMAND); // check whether the page is complete

        GPTChatRequest request = makeRequest(resource, urls, pagePrompts, texts);

        Map<String, String> responses = null;
        boolean finished = false;
        for (int i = 0; i < 3; ++i) {
            String response = chatCompletionService.getSingleChatCompletion(request);
            try {
                responses = extractParts(response);
            } catch (AITemplatingRetryableException e) {
                LOG.info("Retrying since it seems the page was not properly completed. " + e);
                continue;
            }
            String endmarker = responses.get("END");
            finished = THE_END_PATTERN.matcher(StringUtils.defaultString(endmarker)).find();
            if (finished) break;
            LOG.info("Retrying since it seems the page was not properly completed.");
        }
        LOG.warn("Giving up after 3 tries - template was not properly executed. We still replace it so that it's easier to fix.");

        executeReplacements(responses, ids);
        return true;
    }


    protected static @NotNull String joinText(Map<String, String> prompts) {
        StringBuilder prompt = new StringBuilder(PREFIX_PROMPT);
        for (Map.Entry<String, String> entry : prompts.entrySet()) {
            prompt.append("%%%%%%%% ").append(entry.getKey()).append(" %%%%%%%%\n");
            prompt.append(entry.getValue().trim()).append("\n");
        }
        return prompt.toString();
    }

    /**
     * Splits the response at the %%%%%%%% ID %%%%%%%% separators and puts the items into a map.
     * Inverse of {@link #joinText(Map)}.
     */
    @Nonnull
    protected static Map<String, String> extractParts(String response) {
        final Map<String, String> parts = new LinkedHashMap<>();
        Matcher matcher = SEPARATOR_PATTERN.matcher(response);
        int lastEnd = 0;
        String id = null;
        while (matcher.find()) {
            int end = matcher.start();
            if (id != null) parts.put(id, response.substring(lastEnd, end).trim());
            lastEnd = matcher.end();
            id = matcher.group("id");
        }
        if (id != null) parts.put(id, response.substring(lastEnd).trim());
        return parts;
    }

    protected static void executeReplacements(Map<String, String> responses, Map<String, Replacement> ids) {
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            Replacement replacement = ids.get(entry.getKey());
            if (entry.getKey().startsWith(PREFIX_INFORMATIONALLY) || entry.getKey().equals("END")) {
                continue;
            } else if (replacement == null) { // retry? For now, we give up.
                throw new AITemplatingRetryableException("The response contains a key that was not in the prompts: " + entry.getKey());
            }
            ModifiableValueMap properties = replacement.resource.adaptTo(ModifiableValueMap.class);
            properties.put(PROPERTY_PREFIX_PROMPT + replacement.property, properties.get(replacement.property));
            properties.put(replacement.property, entry.getValue());
        }
    }

    protected @NotNull GPTChatRequest makeRequest(Resource resource, List<String> urls, List<String> pagePrompts, Map<String, String> prompts) throws IOException, URISyntaxException {
        GPTChatRequest request = new GPTChatRequest();
        GPTConfiguration config = configurationService.getGPTConfiguration(resource.getResourceResolver(), resource.getPath());
        config = GPTConfiguration.HIGH_INTELLIGENCE.merge(config);
        request.setConfiguration(config);
        StringBuilder sysprompt = new StringBuilder();
        sysprompt.append(SYSMSG);
        for (String pagePrompt : pagePrompts) {
            sysprompt.append("\n\n").append(pagePrompt);
        }
        request.addMessage(GPTMessageRole.SYSTEM, sysprompt.toString());

        for (String url : urls) {
            String markdown = markdownService.getMarkdown(new URI(url));
            request.addMessage(GPTMessageRole.USER,
                    "Please retrieve as source / background information for later prompts the text content of URL `" + url + "`");
            request.addMessage(GPTMessageRole.ASSISTANT, markdown);
        }
        request.addMessage(GPTMessageRole.USER, joinText(prompts));
        return request;
    }

    protected @NotNull List<Replacement> collectPossibleReplacements(Resource resource) {
        List<Replacement> replacements = descendantsStream(resource).flatMap(descendant ->
                descendant.getValueMap().entrySet().stream()
                        .filter(entry -> !IGNORED_PROPERTIES.matcher(entry.getKey()).matches())
                        .filter(entry -> entry.getValue() instanceof String)
                        .filter(entry -> HAS_WHITESPACE.matcher((String) entry.getValue()).find())
                        .map(entry -> new Replacement(descendant, entry.getKey(), (String) entry.getValue()))
        ).collect(Collectors.toList());
        return replacements;
    }

    protected static void collectPrompts(List<Replacement> replacements, Map<String, Replacement> ids, Map<String, String> prompts) {
        AtomicInteger counter = new AtomicInteger();
        for (Replacement replacement : replacements) {
            boolean isRichtext = RICHTEXT_PATTERN.matcher(replacement.text).matches();
            String prompt;
            Matcher idmatch = PROMPTFIELD.matcher(replacement.text);
            String id;
            if (idmatch.find()) {
                String name = idmatch.group("id");
                id = name != null ? "PROMPT" + name : "PROMPT#" + String.valueOf(1000 + counter.incrementAndGet()).substring(1);
                prompt = StringUtils.defaultString(idmatch.group("prefix")) + replacement.text.substring(idmatch.end());
                prompt = (isRichtext ? "Print as rich text HTML: " : "Print as plain text: ") + prompt;
                if (ids.containsKey(id)) {
                    LOG.error("The resource contains a declaration for the key {} twice: one at {} and one at {}", id, ids.get(id), replacement);
                    throw new IllegalArgumentException("The resource contains a declaration for the key " + id + " twice.");
                }
                ids.put(id, replacement);
            } else {
                id = PREFIX_INFORMATIONALLY + String.valueOf(1000 + counter.incrementAndGet()).substring(1);
                prompt = "Print unchanged the quoted text without the quotes: ```" + replacement.text + "```";
            }
            prompts.put(id, prompt);
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
            if (resource != null) sb.append("resource=").append(resource.getPath());
            sb.append(", property='").append(property).append('\'');
            // too much information: sb.append(", prompt='").append(prompt).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    /** An exception that says something is wrong with the response, but that might be temporary and can be retried. */
    protected static class AITemplatingRetryableException extends RuntimeException {
        public AITemplatingRetryableException(String message) {
            super(message);
        }
    }

}
