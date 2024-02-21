package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
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
    TranslationRun startTranslation(ResourceResolver resourceResolver, String path, boolean recursive, GPTConfiguration configuration) throws LoginException, PersistenceException;

    public abstract class TranslationRun {
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

    public abstract class TranslationPage {
        public String pagePath;
        public String status;
    }

}
