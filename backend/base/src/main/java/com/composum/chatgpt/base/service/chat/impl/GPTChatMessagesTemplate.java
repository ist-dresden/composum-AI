package com.composum.chatgpt.base.service.chat.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.GPTException;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;

public class GPTChatMessagesTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(GPTChatMessagesTemplate.class);

    public static final String TEMPLATEDIR = "chattemplates/";
    public static final String TEMPLATESUFFIX = ".txt";

    /**
     * matches the lines like ---------- system ---------- that separate the individual messages in the template
     */
    public static final Pattern MESSAGE_SEPARATOR = Pattern.compile("^-{5,}\\s+(?<role>system|user|assistant)\\s+-{5,}$");

    /**
     * If that's in a string, it likely contains placeholders.
     */
    public static final String PLACEHOLDER_MARKER = "${";

    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private final List<GPTChatMessage> messages = new ArrayList<>();

    private final String templateName;

    public GPTChatMessagesTemplate(@Nullable ClassLoader classLoader, @Nonnull String name) {
        this.templateName = name;
        ClassLoader loader = classLoader != null ? classLoader : getClass().getClassLoader();
        String templatePath = TEMPLATEDIR + name + TEMPLATESUFFIX;
        try (InputStream stream = loader.getResourceAsStream(templatePath);
             InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader buf = new BufferedReader(in)) {
            Iterator<String> lineiterator = buf.lines()
                    .dropWhile((line) -> line.startsWith("#"))
                    .iterator();
            List<List<String>> blocks = getMessageLineBlocks(lineiterator);
            processBlocks(templatePath, blocks);
        } catch (IOException | NullPointerException e) {
            LOG.error("Error reading template {}", name, e);
            throw new GPTException("Internal error reading chat template");
        }
    }

    /**
     * split the lines into blocks of lines where the first line is matching MESSAGE_SEPARATOR
     */
    protected static List<List<String>> getMessageLineBlocks(Iterator<String> lineiterator) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> block = new ArrayList<>();
        while (lineiterator.hasNext()) {
            String line = lineiterator.next();
            if (MESSAGE_SEPARATOR.matcher(line).matches() && !block.isEmpty()) {
                blocks.add(block);
                block = new ArrayList<>();
            }
            block.add(line);
        }
        blocks.add(block);
        return blocks;
    }

    protected void processBlocks(String templatePath, List<List<String>> blocks) {
        for (List<String> b : blocks) {
            Matcher matcher = MESSAGE_SEPARATOR.matcher(b.get(0));
            if (!matcher.matches()) { // impossible
                LOG.error("Bug in template parsing of template {}", templatePath);
                throw new GPTException("Bug in template parsing of template.");
            }
            String role = matcher.group("role");
            String content = b.subList(1, b.size()).stream().collect(Collectors.joining("\n"));
            messages.add(new GPTChatMessage(GPTMessageRole.valueOf(role.toUpperCase()), content));
        }
    }

    /**
     * Returns a list of messages with the placeholders replaced. If a placeholder is missing, we throw an error, as this is a misusage of the template.
     */
    public List<GPTChatMessage> getMessages(Map<String, String> placeholderValues) {
        List<GPTChatMessage> result = new ArrayList<>();
        for (GPTChatMessage message : messages) {
            String content = message.getContent();
            if (content.contains(PLACEHOLDER_MARKER)) {
                content = PLACEHOLDER_PATTERN.matcher(content).replaceAll((match) -> {
                    String name = match.group(1);
                    String value = placeholderValues.get(name);
                    if (value == null) {
                        LOG.error("Problem with template usage: missing placeholder value for placeholder {} in template {}", name, templateName, new Exception("Stacktrace, not thrown"));
                        throw new GPTException("Missing placeholder value for " + name);
                    }
                    return value;
                });
            }
            result.add(new GPTChatMessage(message.getRole(), content));
        }
        return result;
    }

}
