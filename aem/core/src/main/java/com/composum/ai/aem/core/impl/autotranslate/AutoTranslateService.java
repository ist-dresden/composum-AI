package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

public interface AutoTranslateService {

    /**
     * Retrieves all currently running translation processes.
     */
    List<TranslationRun> getTranslationRuns();

    /**
     * Starts a new translation run.
     */
    TranslationRun startTranslation(ResourceResolver resourceResolver, String path, boolean recursive) throws LoginException, PersistenceException;

    public abstract class TranslationRun {
        public String id;
        public String status;
        public String startTime;
        public String stopTime;
        public String user;
        public String rootPath;

        public abstract List<TranslationPage> getTranslatedPages();
        public abstract void cancel();
    }

    public abstract class TranslationPage {
        public String pagePath;
        public String status;
    }

}
