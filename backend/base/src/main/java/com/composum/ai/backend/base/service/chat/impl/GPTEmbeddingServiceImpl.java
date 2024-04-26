package com.composum.ai.backend.base.service.chat.impl;


import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTEmbeddingService;

@Component(service = GPTChatCompletionService.class)
public class GPTEmbeddingServiceImpl implements GPTEmbeddingService {

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    @Override
    public List<float[]> getEmbeddings(List<String> texts, GPTConfiguration configuration) throws GPTException {
        return chatCompletionService.getEmbeddings(texts, configuration);
    }

}
