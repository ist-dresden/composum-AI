package com.composum.chatgpt.bundle.model;

import java.util.Map;

import com.composum.pages.commons.model.AbstractModel;

public class ChatGPTCreateDialogModel extends AbstractModel {

    public Map<String, String> getPredefinedPrompts() {
        return Map.of();
    }

    public Map<String, String> getContentSelectors() {
        return Map.of();
    }

    public Map<String, String> getTextLengths() {
        return Map.of();
    }
}
