package com.composum.chatgpt.bundle;

import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;

/**
 * A service to create markdown with an approximate text content from a page or resource, for use with querying ChatGPT about it.
 */
public interface ApproximateMarkdownService {

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with ChatGPT.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown
     * @return the markdown representation
     */
    @Nonnull
    String approximateMarkdown(@Nullable Resource resource);

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with ChatGPT.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown
     * @param out      destination where the markdown rendering will be written.
     */
    void approximateMarkdown(@Nullable Resource resource, PrintWriter out);
}
