package com.composum.ai.aem.core.impl.autotranslate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.day.cq.wcm.api.WCMException;

/**
 * Actual logic for translating a livecopy.
 */
public interface AutoPageTranslateService {

    /**
     * Implements the actual translation for one page or asset.
     */
    Stats translateLiveCopy(
            @Nonnull Resource resource, @Nullable GPTConfiguration configuration,
            @Nonnull AutoTranslateService.TranslationParameters translationParameters)
            throws WCMException, PersistenceException;

    /**
     * Rolls everything back in the resource - mostly for testing purposes.
     */
    void rollback(Resource resource) throws WCMException;

    public static class Stats {
        public int translateableProperties;
        public int translatedProperties;
        public int paths;
        public int relocatedPaths;

        @Override
        public String toString() {
            return "Stats{" +
                    "translateableProperties=" + translateableProperties +
                    ", translatedProperties=" + translatedProperties +
                    ", paths=" + paths +
                    ", relocatedPaths=" + relocatedPaths +
                    '}';
        }
    }

}
