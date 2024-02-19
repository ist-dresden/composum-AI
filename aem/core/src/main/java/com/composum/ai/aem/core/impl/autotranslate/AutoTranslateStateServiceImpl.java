package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/** Makes sure we have a global state of all translation runs. */
@Component(service = AutoTranslateStateService.class, scope = ServiceScope.SINGLETON)
public class AutoTranslateStateServiceImpl implements AutoTranslateStateService {

    private List<AutoTranslateServiceImpl.TranslationRunImpl> translationRuns = new ArrayList<>();

    @Override
    public List<AutoTranslateServiceImpl.TranslationRunImpl> getTranslationRuns() {
        return translationRuns;
    }

}
