package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;

/**
 * Serves the configurations for the automatic translation.
 */
public interface AutoTranslateConfigService {

    /**
     * Whether the debugging UI is enabled.
     */
    boolean isPocUiEnabled();

    boolean isEnabled();

    /**
     * If true, the translator will use the 'high-intelligence model' (see OpenAI config) for translation.
     */
    boolean isUseHighIntelligenceModel();

    boolean isTranslatableResource(@Nullable Resource resource);

    /**
     * Returns those attributes that should be translated. (Of the resource, not children.)
     */
    List<String> translateableAttributes(@Nullable Resource resource);

    /**
     * If true, we do not only provide changed texts to the AI during re-translating a page with some changes, but give the entire page to provide better context. That is a bit slower and a bit more expensive, but likely improves the result.
     */
    boolean includeFullPageInRetranslation();

    /**
     * If true, we when retranslating a page with some changes we provide the existing translations of that page to the AI as well as additional context with examples. That is a bit slower and a bit more expensive, but likely improves the result."
     */
    boolean includeExistingTranslationsInRetranslation();

}
