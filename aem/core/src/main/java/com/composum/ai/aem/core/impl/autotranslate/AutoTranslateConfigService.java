package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;

/**
 * Serves the configurations for the automatic translation.
 */
public interface AutoTranslateConfigService {

    /**
     * Whether the proof of concept UI is enabled.
     */
    boolean isPocUiEnabled();

    boolean isEnabled();

    /** If true, the translator will use the 'high-intelligence model' (see OpenAI config) for translation. */
    boolean isUseHighIntelligenceModel();

    boolean isTranslatableResource(@Nullable Resource resource);

    /**
     * Returns those attributes that should be translated. (Of the resource, not children.)
     */
    List<String> translateableAttributes(@Nullable Resource resource);

}
