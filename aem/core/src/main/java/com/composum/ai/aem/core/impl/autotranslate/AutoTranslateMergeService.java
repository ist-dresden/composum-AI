package com.composum.ai.aem.core.impl.autotranslate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;

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
    @Nonnull
    List<AutoTranslateProperty> getProperties(Resource resource);

    /**
     * Saves the new content to a property.
     *
     * @param resource     the resource to save the translation to.
     * @param propertyName the name of the property to save the translation to.
     * @param content      the new content to save.
     * @param markAsMerged whether to mark the property as merged
     * @return a map containing "saved" and the text saved in the resource, to verify that it went OK, otherwise empty.
     */
    Map<String, String> saveTranslation(@Nonnull Resource resource, @Nonnull String propertyName,
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
        private final String componentName;
        private final String componentTitle;
        private final String componentPath;
        private final boolean cancelled;

        public AutoTranslateProperty(String path, String componentPath, AITranslatePropertyWrapper wrapper, String componentName, String componentTitle, @Nonnull LiveRelationship relationship) {
            this.path = path;
            this.componentPath = componentPath;
            this.wrapper = wrapper;
            this.componentName = componentName;
            this.componentTitle = componentTitle;
            this.cancelled = relationship.getStatus().isCancelled() ||
                    relationship.getStatus().getCanceledProperties().contains(wrapper.getPropertyName());
        }

        public String getPath() {
            return path;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getComponentPath() {
            return componentPath;
        }

        public String getComponentTitle() {
            return StringUtils.abbreviate(StringUtils.defaultString(componentTitle), 120);
        }

        public AITranslatePropertyWrapper getWrapper() {
            return wrapper;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public String cancelledClass() {
            return isCancelled() ? "cancelled" : "uncancelled";
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
                        htmlBuf.append(wrapExcludingHTMLTags(text, "<span class=\"ins\">", "</span>"));
                        break;
                    case DELETE:
                        htmlBuf.append(wrapExcludingHTMLTags(text, "<del>", "</del>"));
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
            String src = normalizeForDiff(wrapper.getNewOriginalCopy());
            String dst = normalizeForDiff(wrapper.getOriginalCopy());
            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(src, dst);
            dmp.diff_cleanupSemanticLossless(diffs);

            StringBuilder htmlBuf = new StringBuilder();
            for (DiffMatchPatch.Diff aDiff : diffs) {
                String text = aDiff.text;
                switch (aDiff.operation) {
                    case INSERT: // strangely the html context in HTL seems to swallow ins, so we use span.ins instead
                        htmlBuf.append(wrapExcludingHTMLTags(text, "<del>", "</del>"));
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
            String src = normalizeForDiff(wrapper.getOriginalCopy());
            String dst = normalizeForDiff(wrapper.getNewOriginalCopy());
            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diff_main(src, dst);
            dmp.diff_cleanupSemanticLossless(diffs);

            StringBuilder htmlBuf = new StringBuilder();
            for (DiffMatchPatch.Diff aDiff : diffs) {
                String text = aDiff.text;
                switch (aDiff.operation) {
                    case INSERT:
                        htmlBuf.append(wrapExcludingHTMLTags(text, "<span class=\"ins\">", "</span>"));
                        break;
                    case EQUAL:
                        htmlBuf.append(text);
                        break;
                }
            }
            String html = htmlBuf.toString();
            return html;
        }

        /** Remove stuff that makes trouble with diffs. Currently rel="noopener noreferrer" */
        protected String normalizeForDiff(String text) {
            return text != null ? text.replaceAll(" rel=\"noopener noreferrer\"", " ").trim() : "";
        }

        /**
         * Matches an opening or closing HTML tag.
         */
        protected static final Pattern HTML_TAG_PATTERN = Pattern.compile("(\\s|\\u00A0|&nbsp;)*</?[a-zA-Z][^>]*/?>(\\s|\\u00A0|&nbsp;)*");

        /**
         * Several kinds of whitespace in HTML.
         */
        protected static final Pattern HTML_WHITESPACE_PATTERN = Pattern.compile("(\\s|\\u00A0|&nbsp;)+");

        /**
         * We wrap the text into wrapstart and wrapstop. If there is an opening or closing HTML tag we do not wrap that but only the texts in between.
         */
        protected static String wrapExcludingHTMLTags(@Nonnull String text, @Nonnull String wrapstart, @Nonnull String wrapstop) {
            if (StringUtils.isBlank(text) || HTML_WHITESPACE_PATTERN.matcher(text).matches()) {
                return text;
            }
            StringBuffer wrapped = new StringBuffer();
            Matcher matcher = HTML_TAG_PATTERN.matcher(text);
            int lastEnd = 0;

            while (matcher.find()) {
                // Append the text before the current HTML tag, wrapped
                if (matcher.start() > lastEnd) {
                    String content = text.substring(lastEnd, matcher.start());
                    wrapped.append(wrapstart).append(content).append(wrapstop);
                }

                // Append the HTML tag as is
                wrapped.append(matcher.group());
                lastEnd = matcher.end();
            }

            // Append any remaining text after the last HTML tag, wrapped
            if (lastEnd < text.length()) {
                String content = text.substring(lastEnd);
                wrapped.append(wrapstart).append(content).append(wrapstop);
            }

            return wrapped.toString();
        }

        /**
         * Returns the path to the resource within the page - that is, after the jcr:content node.
         */
        public String getPathInPage() {
            return StringUtils.substringAfter(getPath(), "/jcr:content/");
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
