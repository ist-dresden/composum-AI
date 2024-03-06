package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.day.cq.wcm.api.WCMException;

public interface AutoTranslateService {

    /**
     * Retrieves all currently running translation processes.
     */
    List<TranslationRun> getTranslationRuns();

    /**
     * Starts a new translation run.
     */
    TranslationRun startTranslation(
            @Nonnull ResourceResolver resourceResolver, @Nonnull String path,
            @Nonnull TranslationParameters translationParameters, @Nullable GPTConfiguration configuration)
            throws LoginException, PersistenceException;

    /**
     * Rolls the translation results at this resource back - mostly for debugging.
     */
    void rollback(Resource resource) throws WCMException;

    boolean isEnabled();

    static class TranslationParameters {
        /**
         * Translate subpages as well.
         */
        public boolean recursive;

        /**
         * Also re-translate properties where the original was changed.
         */
        public boolean translateWhenChanged;
        /**
         * Optionally, additional instructions to add to the system prompt.
         */
        public String additionalInstructions;

        /**
         * If true, we break the inheritance of the component / the property on translation.
         */
        public boolean breakInheritance;

        /**
         * If true the changes are saved ({@link ResourceResolver#commit()}) after each page.
         */
        public boolean autoSave = true;
    }

    static abstract class TranslationRun {
        public String id;
        public String status;
        public String startTime;
        public String stopTime;
        public String user;
        public String rootPath;
        public StringBuilder messages = new StringBuilder();

        public abstract List<TranslationPage> getTranslatedPages();

        public abstract void cancel();

        public abstract void rollback(@Nonnull ResourceResolver resourceResolver) throws PersistenceException, WCMException;
    }

    static abstract class TranslationPage {
        public String pagePath;
        public String status;
        public AutoPageTranslateService.Stats stats;
    }

}
