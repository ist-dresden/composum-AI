package com.composum.ai.aem.core.impl.autotranslate;

import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.List;
import java.util.regex.Pattern;

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
    void rollback(Resource resource) throws WCMException, PersistenceException;

    boolean isEnabled();

    static class TranslationParameters implements Cloneable {
        /**
         * Translate subpages as well.
         */
        public boolean recursive;

        /**
         * Maximum depth of the recursion.
         */
        public Integer maxDepth;

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

        /**
         * Optionally, a number of rules that give additional instructions for translation if certain words or phrases
         * are present in the page.
         */
        public List<AutoTranslateRuleConfig> rules;

        /**
         * If set, this is used as user id that is saved to denote who translated the resource.
         */
        String userId = null;

        @Override
        public String toString() {
            return "TranslationParameters{" +
                    "recursive=" + recursive +
                    ", maxDepth=" + maxDepth +
                    ", translateWhenChanged=" + translateWhenChanged +
                    ", breakInheritance=" + breakInheritance +
                    ", autoSave=" + autoSave +
                    ", userId='" + userId + '\'' +
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

    static abstract class TranslationPage {
        private final static Pattern IMAGE_VIDEO_PATTERN =
                Pattern.compile("\\.(png|jpg|jpeg|gif|svg|mp3|mov|mp4)(/|$)", Pattern.CASE_INSENSITIVE);

        public String pagePath;
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
    }

}
