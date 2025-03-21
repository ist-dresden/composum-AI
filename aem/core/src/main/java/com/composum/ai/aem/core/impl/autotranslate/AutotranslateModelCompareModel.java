package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.granite.translation.api.TranslationResult;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;

/**
 * Model for the auto-translate comparison page.
 * This model handles the translation process and provides the results.
 */
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

    private List<TranslationResult> results;
    private String text;
    private String targetLanguage;
    private String[] selectedModels;

    @PostConstruct
    protected void init() {
        this.text = request.getParameter("text");
        this.targetLanguage = request.getParameter("targetLanguage");
        this.selectedModels = request.getParameterValues("selectedModels");
    }

    public List<String> getModels() {
        return gptBackendsService.getAllModels();
    }

    public String getError() {
        if (request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        StringBuilder error = new StringBuilder();
        if (text == null || text.trim().isEmpty()) {
            error.append("Please enter some text to translate.");
        }
        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            if (error.length() > 0) {
                error.append("\n");
            }
            error.append("Please select a target language.");
        }
        if (selectedModels == null || selectedModels.length == 0) {
            if (error.length() > 0) {
                error.append("\n");
            }
            error.append("Please select at least one model.");
        }
        return error.length() > 0 ? error.toString() : null;
    }

    /**
     * Determine the translation results. Trigger all requests in parallel to save time.
     */
    public List<TranslationResult> getResults() {
        if (results == null) {
            results = new ArrayList<>();

            if (text != null && !text.trim().isEmpty() &&
                    targetLanguage != null && !targetLanguage.trim().isEmpty() &&
                    selectedModels != null && selectedModels.length > 0 &&
                    gptTranslationService != null) {
                for (String modelName : selectedModels) {
                    GPTConfiguration configuration = GPTConfiguration.ofModel(modelName);
                    CompletableFuture<String> translationFuture = new CompletableFuture<>();
                    GPTCompletionCallback.GPTCompletionCollector collector = new GPTCompletionCallback.GPTCompletionCollector() {
                        @Override
                        public void onFinish(GPTFinishReason finishReason) {
                            if (finishReason == GPTFinishReason.STOP) {
                                translationFuture.complete(this.getResult());
                            } else {
                                translationFuture.complete(this.getResult() + "\n\nError: " + finishReason);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            translationFuture.complete(this.getResult() + "\n\nError: " + throwable);
                        }
                    };
                    gptTranslationService.streamingSingleTranslation(text, null, targetLanguage, configuration, collector);
                    results.add(new TranslationResult(modelName, translationFuture));
                }
            }
        }
        return results;
    }

    public static class TranslationResult {
        private final String model;
        private final CompletableFuture<String> translationFuture;

        public TranslationResult(String model, CompletableFuture<String> translationFuture) {
            this.model = model;
            this.translationFuture = translationFuture;
        }

        public String getModel() {
            return model;
        }

        public String getTranslation() {
            try {
                return translationFuture.get(1, TimeUnit.MINUTES);
            } catch (Exception e) {
                return e.toString();
            }
        }
    }

}
