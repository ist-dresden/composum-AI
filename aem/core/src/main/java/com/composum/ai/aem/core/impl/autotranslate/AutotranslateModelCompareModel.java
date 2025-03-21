package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class AutotranslateModelCompareModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    protected com.composum.ai.backend.base.service.chat.GPTBackendsService gptBackendsService;

    @OSGiService
    protected com.composum.ai.backend.base.service.chat.GPTTranslationService gptTranslationService;

    private List<String> models;
    private List<TranslationResult> results;

    @PostConstruct
    protected void init() {
        // Retrieve available models
        if (gptBackendsService != null) {
            models = gptBackendsService.getAllModels();
        }

        // Process translation if the required parameters are provided
        String text = request.getParameter("text");
        String targetLanguage = request.getParameter("targetLanguage");
        String[] selectedModels = request.getParameterValues("selectedModels");

        if (text != null && !text.trim().isEmpty() &&
                targetLanguage != null && !targetLanguage.trim().isEmpty() &&
                selectedModels != null && selectedModels.length > 0 &&
                gptTranslationService != null) {
            results = new ArrayList<>();
            for (String modelName : selectedModels) {
                try {
                    GPTConfiguration configuration = GPTConfiguration.ofModel(modelName);
                    String translation = gptTranslationService.singleTranslation(text, null, targetLanguage, configuration);
                    results.add(new TranslationResult(modelName, translation));
                } catch (Exception e) {
                    results.add(new TranslationResult(modelName, "Error: " + e.getMessage()));
                }
            }
        }
    }

    public List<String> getModels() {
        return models;
    }

    public List<TranslationResult> getResults() {
        return results;
    }

    public static class TranslationResult {
        private String model;
        private String translation;

        public TranslationResult(String model, String translation) {
            this.model = model;
            this.translation = translation;
        }

        public String getModel() {
            return model;
        }

        public String getTranslation() {
            return translation;
        }
    }
}
