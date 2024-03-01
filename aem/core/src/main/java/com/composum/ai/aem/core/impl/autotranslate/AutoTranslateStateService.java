package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

public interface AutoTranslateStateService {
    List<AutoTranslateServiceImpl.TranslationRunImpl> getTranslationRuns();
}
