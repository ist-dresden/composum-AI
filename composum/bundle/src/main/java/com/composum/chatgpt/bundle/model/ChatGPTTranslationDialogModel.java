package com.composum.chatgpt.bundle.model;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;

import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.taglib.PropertyEditHandle;
import com.composum.sling.core.BeanContext;

/**
 * Model for rendering the translation dialog. It gets instantiated on a property resource, e.g. page/cr:content/jcr:description .
 */
public class ChatGPTTranslationDialogModel extends AbstractModel {

    /**
     * The name of the property for which the translation dialog is opened.
     */
    protected String propertyName;

    /**
     * The property edit handle for the property.
     */
    protected PropertyEditHandle<String> propertyEditHandle;

    /**
     * The sources for translation.
     */
    protected List<Sources> sources;

    /**
     * Returns the list of sources.
     *
     * @return The sources.
     */
    public List<Sources> getSources() {
        return sources;
    }

    /**
     * Sets the sources.
     *
     * @param sources The sources.
     */
    public void setSources(List<Sources> sources) {
        this.sources = sources;
    }

    /**
     * Initializes the model.
     *
     * @param context  The bean context.
     * @param resource The resource.
     */
    @Override
    public void initialize(BeanContext context, Resource resource) {
        propertyName = resource.getName();
        super.initialize(context, resource);
        propertyEditHandle = new PropertyEditHandle(String.class);
        propertyEditHandle.setProperty(propertyName, propertyName, true);
        // FIXME(hps,04.05.23) how to determine that?
        propertyEditHandle.setMultiValue(false);
        propertyEditHandle.initialize(context, this.resource);
        // propertyEditHandle: resource e.g. /content/ist/software/home/test/jcr:content/main/text , propertyName = propertyPath = title
        // Abgeleitet e.g. TextField
        // locale=en_US, languages="en", "de", !multivalue
        // or: /content/ist/software/home/test/jcr:content , propertyName = propertyPath = jcr:title
    }

    /**
     * Determines the resource for the model.
     *
     * @param initialResource The initial resource.
     * @return The resource.
     * @throws IllegalArgumentException if the resource is null or not a property resource.
     */
    @Nullable
    @Override
    protected Resource determineResource(@Nullable Resource initialResource) {
        if (initialResource == null || !initialResource.getValueMap().isEmpty()) {
            throw new IllegalArgumentException("Resource must be a property resource: " + initialResource);
        }
        return initialResource.getParent();
    }

    /**
     * Returns the property edit handle.
     *
     * @return The property edit handle.
     */
    public PropertyEditHandle<String> getPropertyEditHandle() {
        return propertyEditHandle;
    }

    /**
     * Returns the property name.
     *
     * @return The property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Represents a source for translation.
     */
    public static class Sources {
        /**
         * The language key.
         */
        protected String languageKey;
        /**
         * The name of the language.
         */
        protected String languageName;
        /**
         * The text to be translated.
         */
        protected String text;

        /**
         * Returns the language key.
         *
         * @return The language key.
         */
        public String getLanguageKey() {
            return languageKey;
        }

        /**
         * Sets the language key.
         *
         * @param languageKey The language key.
         */
        public void setLanguageKey(String languageKey) {
            this.languageKey = languageKey;
        }

        /**
         * Returns the language name.
         *
         * @return The language name.
         */
        public String getLanguageName() {
            return languageName;
        }

        /**
         * Sets the language name.
         *
         * @param languageName The language name.
         */
        public void setLanguageName(String languageName) {
            this.languageName = languageName;
        }

        /**
         * Returns the text.
         *
         * @return The text.
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the text.
         *
         * @param text The text.
         */
        public void setText(String text) {
            this.text = text;
        }
    }
}
