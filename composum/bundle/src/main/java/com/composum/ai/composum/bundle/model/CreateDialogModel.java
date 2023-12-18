package com.composum.ai.composum.bundle.model;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.pages.commons.model.AbstractModel;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateDialogModel extends AbstractModel {

    private static final Logger LOG = LoggerFactory.getLogger(CreateDialogModel.class);

    protected transient ApproximateMarkdownService approximateMarkdownService;

    public Map<String, String> getPredefinedPrompts() {
        return readJsonFile("create/predefinedprompts.json");
    }

    public Map<String, String> getContentSelectors() {
        Map<String, String> results = new LinkedHashMap<>();
        results.putAll(readJsonFile("create/contentselectors.json"));
        List<ApproximateMarkdownService.Link> componentLinks = getApproximateMarkdownService().getComponentLinks(getResource());
        for (ApproximateMarkdownService.Link link : componentLinks) {
            results.put(link.getPath(), link.getTitle() + " (" + link.getPath() + ")");
        }
        return results;
    }

    protected ApproximateMarkdownService getApproximateMarkdownService() {
        if (approximateMarkdownService == null) {
            approximateMarkdownService = requireNonNull(context.getService(ApproximateMarkdownService.class));
        }
        return approximateMarkdownService;
    }

    public Map<String, String> getTextLengths() {
        return readJsonFile("create/textlengths.json");
    }

    static Map<String, String> readJsonFile(String filePath) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = CreateDialogModel.class.getClassLoader().getResourceAsStream(filePath);
            return mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
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
