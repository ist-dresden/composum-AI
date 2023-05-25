package com.composum.chatgpt.bundle.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.composum.pages.commons.model.AbstractModel;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatGPTCreateDialogModel extends AbstractModel {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> getPredefinedPrompts() {
        return readJsonFile("create/predefinedprompts.json");
    }

    public Map<String, String> getContentSelectors() {
        return readJsonFile("create/contentselectors.json");
    }

    public Map<String, String> getTextLengths() {
        return readJsonFile("create/textlengths.json");
    }

    private Map<String, String> readJsonFile(String filePath) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
            return mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
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
        return "fit-content";
    }
}
