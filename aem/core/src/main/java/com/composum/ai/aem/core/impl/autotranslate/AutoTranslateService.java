package com.composum.ai.aem.core.impl.autotranslate;

import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

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
            @Nonnull TranslationParameters translationParameters)
            throws LoginException, PersistenceException;

    /**
     * Rolls the translation results at this resource back - mostly for debugging.
     */
    void rollback(Resource resource) throws WCMException, PersistenceException;

    boolean isEnabled();

    class TranslationParameters implements Cloneable {
        /**
         * Translate subpages as well.
         */
        public boolean recursive;

        /**
         * Maximum depth of the recursion.
         */
        public Integer maxDepth;

        /**
         * Optionally, additional instructions to add to the system prompt.
         */
        public String additionalInstructions;

        /**
         * If true the changes are saved ({@link ResourceResolver#commit()}) after each page.
         */
        public boolean autoSave = true;

        /**
         * Optionally, a number of rules that give additional instructions for translation if certain words or phrases
         * are present in the page.
         */
        public List<AutoTranslateRuleConfig> rules;

        /**
         * If set, this is used as user id that is saved to denote who translated the resource.
         */
        String userId = null;

        /**
         * Prefer High Intelligence Model : If set, the high intelligence model will be used for translation.
         */
        boolean preferHighIntelligenceModel = false;

        /**
         * Prefer Standard Model : If set, the standard model will be used for translation. Opposite of 'Prefer High Intelligence Model'
         */
        boolean preferStandardModel = false;

        @Override
        public String toString() {
            return "TranslationParameters{" +
                    "recursive=" + recursive +
                    ", maxDepth=" + maxDepth +
                    ", autoSave=" + autoSave +
                    ", userId='" + userId + '\'' +
                    ", preferHighIntelligenceModel=" + preferHighIntelligenceModel +
                    ", preferStandardModel=" + preferStandardModel +
                    ", additionalInstructions='" + additionalInstructions + '\'' +
                    '}';
        }

        @Override
        public TranslationParameters clone() {
            try {
                return (TranslationParameters) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    enum TranslationStatus {
        QUEUED, CANCELLING, RUNNING, CANCELLED, DONE_WITH_ERRORS, INTERRUPTED, ERROR, FINISHED;
    }

    abstract class TranslationRun {
        public String id;
        public TranslationStatus status;
        public String startTime;
        public String stopTime;
        public String user;
        public String rootPath;
        public StringBuilder messages = new StringBuilder();

        public abstract List<TranslationPage> getTranslatedPages();

        public abstract void cancel();

        public abstract void rollback(@Nonnull ResourceResolver resourceResolver) throws PersistenceException, WCMException;

        @Nonnull
        public String statusString() {
            return status != null ? status.name().toLowerCase() : "";
        }

        public boolean isInProgress() {
            return status == TranslationStatus.QUEUED || status == TranslationStatus.RUNNING || status == TranslationStatus.CANCELLING;
        }


        @Override
        public String toString() {
            return "TranslationRun: id='" + id + '\'' +
                    ", status='" + status + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", stopTime='" + stopTime + '\'' +
                    ", user='" + user + '\'' +
                    ", rootPath='" + rootPath + '\'' +
                    ", messages=" + messages;
        }

    }

    abstract class TranslationPage {
        private final static Pattern IMAGE_VIDEO_PATTERN =
                Pattern.compile("\\.(png|jpg|jpeg|gif|svg|mp3|mov|mp4)(/|$)", Pattern.CASE_INSENSITIVE);

        public String pagePath;
        public String translateCopyPagePath;

        public String status;
        public AutoPageTranslateService.Stats stats;

        public String editorUrl() {
            if (startsWith(pagePath, "/content/dam")) {
                if (IMAGE_VIDEO_PATTERN.matcher(pagePath).find()) {
                    return "/assetdetails.html" + pagePath;
                }
                return "/editor.html" + pagePath;
            } else {
                return "/editor.html" + pagePath + ".html";
            }
        }

        /** If a translate copy is present, this would open a diff view. */
        public String diffToCopyUrl() {
            if (startsWith(pagePath, "/content/dam") || translateCopyPagePath == null) {
                return null;
            } else {
                return "/mnt/overlay/wcm/core/content/sites/diffresources.html" + pagePath +
                        "?item=" + translateCopyPagePath + "&sideBySide";
            }
        }

    }

}
