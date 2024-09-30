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
     * If a page is re-translated with only a few modified texts:
     * If true we include the texts that do not have to be translated, too, to guide the translation, and in the
     * target language; otherwise
     * we only include the texts that have to be translated.
     */
    boolean includeAlreadyTranslatedValues();

}
