package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.api.resource.Resource;

/**
 * Service for handling merge operations related to auto-translation.
 * Provides methods to retrieve properties for resources in the context of translations.
 */
public interface AutoTranslateMergeService {

    /**
     * Represents a translated property associated with a resource.
     */
    class AutoTranslateProperty {
        private final String path;
        private final AITranslatePropertyWrapper wrapper;

        public AutoTranslateProperty(String path, AITranslatePropertyWrapper wrapper) {
            this.path = path;
            this.wrapper = wrapper;
        }

        public String getPath() {
            return path;
        }

        public AITranslatePropertyWrapper getWrapper() {
            return wrapper;
        }

        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            if (getPath() != null) {
                builder.append("path", getPath());
            }
            if (getWrapper() != null) {
                builder.append("property", getWrapper().getPropertyName());
            }
            return builder.toString();
        }
    }

    /**
     * Recursively finds all properties from the given resource and its children that have names starting with
     * {@link AITranslatePropertyWrapper#AI_NEW_TRANSLATED_SUFFIX} and creates the AI Translate Property Wrapper for each.
     *
     * @param resource the root resource to start property extraction from.
     * @return a list of AutoTranslateProperty instances with translation details.
     */
    List<AutoTranslateProperty> getProperties(Resource resource);
}
