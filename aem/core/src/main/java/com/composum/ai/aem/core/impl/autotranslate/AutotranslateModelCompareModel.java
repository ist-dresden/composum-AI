package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.composum.ai.aem.core.impl.SelectorUtils;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

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
    private String targetLanguageError;
    private String[] selectedModels;
    private String instructions;

    @PostConstruct
    protected void init() {
        this.text = request.getParameter("text");
        this.targetLanguage = request.getParameter("targetLanguage");
        this.selectedModels = request.getParameterValues("selectedModels");
        this.instructions = request.getParameter("instructions");
    }

    public List<String> getModels() {
        return gptBackendsService.getAllModels();
    }

    public String getBackendList() {
        return gptBackendsService.getActiveBackends().stream().collect(Collectors.joining(", "));
    }

    /**
     * The actual textual representation of the target language, used with the model.
     */
    public String getParsedTargetLanguage() {
        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            return null;
        }
        targetLanguage = targetLanguage.trim();
        String parsedLanguage;
        if (targetLanguage.startsWith("/content/")) {
            PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
            Page page = pageManager.getPage(targetLanguage);
            if (page == null) {
                targetLanguageError = "Page not found: " + targetLanguage;
                return null;
            }
            parsedLanguage = SelectorUtils.findLanguage(page.getContentResource());
            parsedLanguage = SelectorUtils.getLanguageName(parsedLanguage, Locale.ENGLISH);
        } else if (SelectorUtils.isLocaleName(targetLanguage)) {
            parsedLanguage = SelectorUtils.getLanguageName(targetLanguage, Locale.ENGLISH);
        } else if (targetLanguage.matches("^[a-zA-Z]{2,3}([-_][a-zA-Z]{2,3})?$")) {
            targetLanguageError = "Target language looks like a locale name, but is not: " + targetLanguage;
            parsedLanguage = null;
        } else {
            parsedLanguage = targetLanguage;
        }
        return parsedLanguage;
    }

    public String getError() {
        getParsedTargetLanguage(); // might generate error
        if (request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        StringBuilder error = new StringBuilder();
        if (targetLanguageError != null) {
            error.append(targetLanguageError).append("\n\n");
        }
        if (text == null || text.trim().isEmpty()) {
            error.append("Please enter some text to translate.");
        }
        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            if (error.length() > 0) {
                error.append("\n");
            }
            error.append("Please enter a target language.");
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
                    getParsedTargetLanguage() != null &&
                    selectedModels != null && selectedModels.length > 0 &&
                    gptTranslationService != null) {
                for (String modelName : selectedModels) {
                    GPTConfiguration configuration = GPTConfiguration.ofModel(modelName);
                    if (instructions != null && !instructions.trim().isEmpty()) {
                        configuration = GPTConfiguration.ofAdditionalInstructions(instructions).merge(configuration);
                    }
                    CompletableFuture<String> translationFuture = new CompletableFuture<>();
                    long startTime = System.currentTimeMillis();

                    TranslationResult tr = new TranslationResult(modelName, translationFuture);
                    GPTCompletionCallback.GPTCompletionCollector collector = new GPTCompletionCallback.GPTCompletionCollector() {
                        @Override
                        public void onFinish(GPTFinishReason finishReason) {
                            if (finishReason == GPTFinishReason.STOP) {
                                translationFuture.complete(this.getResult());
                            } else {
                                translationFuture.complete(this.getResult() + "\n\nError: " + finishReason);
                            }
                            tr.seconds = (int) (System.currentTimeMillis() - startTime);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            translationFuture.complete(this.getResult() + "\n\nError: " + throwable);
                            tr.seconds = (int) (System.currentTimeMillis() - startTime);
                        }
                    };
                    try {
                        gptTranslationService.streamingSingleTranslation(text, null, getParsedTargetLanguage(), configuration, collector);
                    } catch (Exception e) {
                        translationFuture.complete("Error: " + e);
                    }
                    results.add(tr);
                }
            }
        }
        return results;
    }

    public static class TranslationResult {
        private final String model;
        private final CompletableFuture<String> translationFuture;
        private int seconds;

        public TranslationResult(String model, CompletableFuture<String> translationFuture) {
            this.model = model;
            this.translationFuture = translationFuture;
        }

        public String getModel() {
            return model;
        }

        public int getSeconds() {
            getTranslation(); // make sure it's already done and this is actually set.
            return seconds;
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
