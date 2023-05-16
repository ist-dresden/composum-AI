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
}
