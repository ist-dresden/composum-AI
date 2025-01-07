package com.composum.ai.aem.core.impl.autotranslate;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.WCMException;

/**
 * Service for handling merge operations related to auto-translation.
 * Provides methods to retrieve properties for resources in the context of translations.
 */
public interface AutoTranslateMergeService {

    /**
     * Recursively finds all properties from the given resource and its children that have names starting with
     * {@link AITranslatePropertyWrapper#AI_NEW_TRANSLATED_SUFFIX} and creates the AI Translate Property Wrapper for each.
     *
     * @param resource the root resource to start property extraction from.
     * @return a list of AutoTranslateProperty instances with translation details.
     */
    List<AutoTranslateProperty> getProperties(Resource resource);

    /**
     * Saves the new content to a property.
     *
     * @param resource     the resource to save the translation to.
     * @param propertyName the name of the property to save the translation to.
     * @param content      the new content to save.
     * @param markAsMerged whether to mark the property as merged
     */
    void saveTranslation(@Nonnull Resource resource, @Nonnull String propertyName,
                         @Nonnull String content, @Nonnull boolean markAsMerged) throws WCMException;

    /*
     * Try an intelligent merge: combine the original source, the new source, the new translation and the current text into a new text trying to keep the spirit of the translation.
     * @param language the language of the translation.
     * @param resource the resource to merge the text for.
     * @param originalSource the original source text.
     * @param newSource the new source text.
     * @param newTranslation the new translation text.
     * @param currentText the current text.
     */
    String intelligentMerge(String language, @Nonnull Resource resource, @Nonnull String originalSource,
                            @Nonnull String newSource, @Nonnull String newTranslation,
                            @Nonnull String currentText);

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

        public String getOriginalCopyDiffsHTML() {
            String original = wrapper.getOriginalCopy();
            String newOriginal = wrapper.getNewOriginalCopy();
            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(original, newOriginal);
            dmp.diff_cleanupSemanticLossless(diffs);

            StringBuilder htmlBuf = new StringBuilder();
            for (DiffMatchPatch.Diff aDiff : diffs) {
                String text = aDiff.text;
                switch (aDiff.operation) {
                    case INSERT: // strangely the html context in HTL seems to swallow ins, so we use span.ins instead
                        htmlBuf.append("<span class=\"ins\">").append(text).append("</span>");
                        break;
                    case DELETE:
                        htmlBuf.append("<del>").append(text).append("</del>");
                        break;
                    case EQUAL:
                        htmlBuf.append(text);
                        break;
                }
            }
            String html = htmlBuf.toString();
            return html;
        }

        public String getOriginalCopyInsertionsMarked() {
            String src = wrapper.getOriginalCopy();
            String dst = wrapper.getNewOriginalCopy();
            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(src, dst);
            dmp.diff_cleanupSemanticLossless(diffs);

            StringBuilder htmlBuf = new StringBuilder();
            for (DiffMatchPatch.Diff aDiff : diffs) {
                String text = aDiff.text;
                switch (aDiff.operation) {
                    case INSERT: // strangely the html context in HTL seems to swallow ins, so we use span.ins instead
                        htmlBuf.append("<span class=\"ins\">").append(text).append("</span>");
                        break;
                    case EQUAL:
                        htmlBuf.append(text);
                        break;
                }
            }
            String html = htmlBuf.toString();
            return html;
        }

        public String getNewOriginalCopyInsertionsMarked() {
            String src = wrapper.getNewOriginalCopy();
            String dst = wrapper.getOriginalCopy();
            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(src, dst);
            dmp.diff_cleanupSemanticLossless(diffs);

            StringBuilder htmlBuf = new StringBuilder();
            for (DiffMatchPatch.Diff aDiff : diffs) {
                String text = aDiff.text;
                switch (aDiff.operation) {
                    case INSERT:
                        htmlBuf.append("<span class=\"ins\">").append(text).append("</span>");
                        break;
                    case EQUAL:
                        htmlBuf.append(text);
                        break;
                }
            }
            String html = htmlBuf.toString();
            return html;
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

}
