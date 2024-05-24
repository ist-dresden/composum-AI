package com.composum.ai.composum.bundle.model;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;
import com.composum.pages.commons.model.AbstractModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class CreateDialogModel extends AbstractModel {

    private static final Logger LOG = LoggerFactory.getLogger(CreateDialogModel.class);

    protected transient ApproximateMarkdownService approximateMarkdownService;

    protected transient GPTChatCompletionService chatCompletionService;

    protected transient AIConfigurationService aiConfigurationService;

    public Map<String, String> getPredefinedPrompts() {
        GPTPromptLibrary paths = getAIConfigurationService().getGPTPromptLibraryPaths(getContext().getRequest(), getResource().getPath());
        if (paths != null) {
            String path = paths.contentCreationPromptsPath();
            Map<String, String> map = getAIConfigurationService().getGPTConfigurationMap(getContext().getRequest(), path, null);
            return map;
        }
        LOG.error("No paths for predefined prompts found for {}", getPath());
        return null;
    }

    public Map<String, String> getContentSelectors() {
        Map<String, String> results = new LinkedHashMap<>();
        results.putAll(readJsonFile("create/contentselectors.json"));
        List<ApproximateMarkdownService.Link> componentLinks = getApproximateMarkdownService().getComponentLinks(getResource());
        for (ApproximateMarkdownService.Link link : componentLinks) {
            if (!link.isNeedsVision() || getChatCompletionService().isVisionEnabled()) {
                results.put(link.getPath(), link.getTitle() + " (" + link.getPath() + ")");
            }
        }
        return results;
    }

    protected ApproximateMarkdownService getApproximateMarkdownService() {
        if (approximateMarkdownService == null) {
            approximateMarkdownService = requireNonNull(context.getService(ApproximateMarkdownService.class));
        }
        return approximateMarkdownService;
    }

    protected GPTChatCompletionService getChatCompletionService() {
        if (chatCompletionService == null) {
            chatCompletionService = requireNonNull(context.getService(GPTChatCompletionService.class));
        }
        return chatCompletionService;
    }

    protected AIConfigurationService getAIConfigurationService() {
        if (aiConfigurationService == null) {
            aiConfigurationService = requireNonNull(context.getService(AIConfigurationService.class));
        }
        return aiConfigurationService;
    }

    public Map<String, String> getTextLengths() {
        return readJsonFile("create/textlengths.json");
    }

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    static Map<String, String> readJsonFile(String filePath) {
        try {
            InputStream inputStream = CreateDialogModel.class.getClassLoader().getResourceAsStream(filePath);
            return gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), Map.class);
        } catch (JsonSyntaxException | JsonIOException e) {
            LOG.error("Cannot read {}", filePath, e);
            return null;
        }
    }

    /**
     * Whether it is field type rich or just text.
     */
    public Boolean getIsRichText() {
        return Boolean.valueOf(context.getRequest().getParameter("richtext"));
    }

    /**
     * For rendering as richtext widget - this is used as initial value for the text.
     */
    public String getText() {
        return "";
    }

    /**
     * For rendering as richttext widget - the height in the inline style. Possibly auto , max-content, fit-content or a specific height.
     * Doing fit-content just adapts to the content and you can scroll the whole dialog, anyway.
     */
    public String getHeight() {
        return null;
    }
}
