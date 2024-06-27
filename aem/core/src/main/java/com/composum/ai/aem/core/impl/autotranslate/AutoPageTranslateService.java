package com.composum.ai.aem.core.impl.autotranslate;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.WCMException;

/**
 * Actual logic for translating a livecopy.
 */
public interface AutoPageTranslateService {

    /**
     * Implements the actual translation for one page or asset.
     */
    Stats translateLiveCopy(
            @Nonnull Resource resource,
            @Nonnull AutoTranslateService.TranslationParameters translationParameters)
            throws WCMException, PersistenceException;

    /**
     * Rolls everything back in the resource - mostly for testing purposes.
     */
    void rollback(Resource resource) throws WCMException, PersistenceException;

    class Stats {
        public int translateableProperties;

        public int translatedProperties;

        /**
         * Were already translated, but translation source had changed.
         */
        public int retranslatedProperties;

        /**
         * Properties that have manually been modified after translation, but were now changed to automatic translation
         * because of a change in the source. -> manual care is needed.
         * Also count as {@link #retranslatedProperties}.
         */
        public int modifiedButRetranslatedProperties;

        public int paths;

        public int relocatedPaths;

        public String collectedAdditionalInstructions;

        public boolean hasChanges() {
            return translatedProperties + retranslatedProperties + modifiedButRetranslatedProperties + relocatedPaths > 0;
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "translateableProperties=" + translateableProperties +
                    ", translatedProperties=" + translatedProperties +
                    ", retranslatedProperties=" + retranslatedProperties +
                    ", modifiedButRetranslatedProperties=" + modifiedButRetranslatedProperties +
                    ", paths=" + paths +
                    ", relocatedPaths=" + relocatedPaths +
                    '}';
        }
    }

}
