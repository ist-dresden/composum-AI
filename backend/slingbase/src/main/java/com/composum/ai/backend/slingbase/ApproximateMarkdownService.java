package com.composum.ai.backend.slingbase;

import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;

/**
 * A service to create markdown with an approximate text content from a page or resource, for use with querying the AI about it.
 */
public interface ApproximateMarkdownService {

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with the AI.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown. Caution: if this is not the content resource of a page but the cpp:Page, the markdown will contain all subpages as well!
     * @return the markdown representation
     */
    @Nonnull
    String approximateMarkdown(@Nullable Resource resource);

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with the AI.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown. Caution: if this is not the content resource of a page but the cpp:Page, the markdown will contain all subpages as well!
     * @param out      destination where the markdown rendering will be written.
     */
    void approximateMarkdown(@Nullable Resource resource, PrintWriter out);

    /**
     * Returns a markdown representation of an attribute value, which might be plain text or HTML. We determine whether
     * it's HTML heuristically - in that case it's transformed to markdown, otherwise we just return the value.
     */
    @Nonnull
    String getMarkdown(@Nullable String value);
}
