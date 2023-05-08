package com.composum.chatgpt.bundle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;

import com.composum.pages.commons.PagesConstants;
import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.model.properties.Language;
import com.composum.pages.commons.taglib.PropertyEditHandle;
import com.composum.sling.core.BeanContext;

/**
 * Model for rendering the translation dialog. It gets instantiated on a property resource, e.g. page/cr:content/jcr:description .
 */
public class ChatGPTTranslationDialogModel extends AbstractModel {

    /**
     * The name of the property for which the translation dialog is opened, e.g. "jcr:title", "title", "text", ...
     */
    protected String propertyName;


    /**
     * The path where the property is actually saved according to our i18n method, e.g. "i18n/de/text".
     */
    protected String propertyI18nPath;

    /**
     * The property edit handle for the property.
     */
    protected PropertyEditHandle<String> handle;

    /**
     * The sources for translation.
     */
    protected List<Source> sources;

    public void setPropertyI18nPath(String propertyI18nPath) {
        this.propertyI18nPath = propertyI18nPath;
    }

    /**
     * Returns the list of sources.
     *
     * @return The sources.
     */
    public List<Source> getSources() {
        if (sources == null) {
            List<Source> newSources = new ArrayList<>();
            String valueInDefaultLanguage = getValueForLanguage(getLanguages().getDefaultLanguage());
            for (Language language : getLanguages()) {
                if (!language.equals(getLanguage())) {
                    String value = getValueForLanguage(language);
                    boolean isDefaultLanguage = language.equals(getLanguages().getDefaultLanguage());
                    // check whether there actually is a text for that language and that it's not just taken from the default language.
                    if (StringUtils.isNotBlank(value) && (isDefaultLanguage || !value.equals(valueInDefaultLanguage))) {
                        Source source = new Source(language.getLanguageKey(), language.getName(), value);
                        newSources.add(source);
                    }
                }
            }
            sources = newSources;
        }
        return sources;
    }

    protected String getValueForLanguage(Language language) {
        BeanContext othercontext = new BeanContext.Wrapper(context.withLocale(language.getLocale())) {
            @Override
            public <T> T getAttribute(String name, Class<T> T) {
                if (PagesConstants.RA_STICKY_LOCALE.equals(name)) {
                    return (T) language.getLocale();
                }
                return super.getAttribute(name, T);
            }
        };
        PropertyEditHandle<String> handle = makePropertyEditHandle(othercontext, propertyName);
        String value = handle.getValue();
        return value;
    }

    @javax.annotation.Nullable
    public String getValueForLanguage(@Nonnull String languageKey) {
        for (Language language : getLanguages()) {
            if (language.getLanguageKey().equals(languageKey)) {
                return getValueForLanguage(language);
            }
        }
        return null;
    }

    /**
     * Returns "singlesource" if there is only one source language, so that we can hide the checkbox.
     */
    public String getSingleSourceClass() {
        return getSources().size() > 1 ? "" : "singlesource";
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
    }

    protected PropertyEditHandle<String> makePropertyEditHandle(BeanContext context, String propertyI18nPath) {
        Objects.requireNonNull(getPropertyI18nPath());
        PropertyEditHandle<String> handle = new PropertyEditHandle(String.class);
        handle.setProperty(propertyName, propertyI18nPath, true);
        // FIXME(hps,04.05.23) how to determine that?
        handle.setMultiValue(false);
        handle.initialize(context, this.resource);
        // propertyEditHandle: resource e.g. /content/ist/software/home/test/jcr:content/main/text , propertyName = propertyPath = title
        // Abgeleitet e.g. TextField
        // locale=en_US, languages="en", "de", !multivalue
        // or: /content/ist/software/home/test/jcr:content , propertyName = propertyPath = jcr:title
        return handle;
    }

    protected String getPropertyI18nPath() {
        if (propertyI18nPath == null) {
            propertyI18nPath = context.getRequest().getParameter("propertypath");
        }
        return propertyI18nPath;
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
        if (handle == null) {
            handle = makePropertyEditHandle(context, getPropertyI18nPath());
        }
        return handle;
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
     * Whether it is field type rich or just text.
     *
     * @see com.composum.sling.cpnl.TextTag.Type
     */
    public String getFieldType() {
        // FIXME(hps,05.05.23) somehow determine this.
        return "rich";
    }

    public boolean isTranslationPossible() {
        return !getSources().isEmpty();
    }

    /**
     * Represents a source for translation.
     */
    public static class Source {
        protected final String languageKey;
        protected final String languageName;
        protected final String text;

        public Source(String languageKey, String languageName, String text) {
            this.languageKey = languageKey;
            this.languageName = languageName;
            this.text = text;
        }

        /**
         * Returns the language key.
         *
         * @return The language key.
         */
        public String getLanguageKey() {
            return languageKey;
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
         * Returns the text.
         *
         * @return The text.
         */
        public String getText() {
            return text;
        }
    }
}
