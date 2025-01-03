package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;
import org.apache.sling.api.resource.Resource;

/**
 * Service for handling merge operations related to auto-translation.
 * Provides methods to retrieve properties for resources in the context of translations.
 */
public interface AutoTranslateMergeService {

    /**
     * Recursively finds all properties from the given resource and its children that have names starting with
     * {@link AITranslatePropertyWrapper#AI_NEW_TRANSLATED_SUFFIX} and creates the AI Translate Property Wrapper for each.
     *
     * @param resource the root resource to start property extraction from.
     * @return a list of AITranslatePropertyWrapper instances with translation details.
     */
    List<AITranslatePropertyWrapper> getProperties(Resource resource);
}
