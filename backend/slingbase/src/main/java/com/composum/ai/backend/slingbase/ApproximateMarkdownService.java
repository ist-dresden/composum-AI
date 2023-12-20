package com.composum.ai.backend.slingbase;

import java.io.PrintWriter;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

/**
 * A service to create markdown with an approximate text content from a page or resource, for use with querying the AI about it.
 */
public interface ApproximateMarkdownService {

    /**
     * An additional header for the response that tells that the path is actually an image and gives its path.
     */
    String HEADER_IMAGEPATH = "imagepath";

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with the AI.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown. Caution: if this is not the content resource of a page but the cpp:Page, the markdown will contain all subpages as well!
     * @param request
     * @param response
     * @return the markdown representation
     */
    @Nonnull
    String approximateMarkdown(@Nullable Resource resource,
                               @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response);

    /**
     * Generates a text formatted with markdown that heuristically represents the text content of a page or resource, mainly for use with the AI.
     * That is rather heuristically - it cannot faithfully represent the page, but will probably be enough to generate summaries, keywords and so forth.
     *
     * @param resource the resource to render to markdown. Caution: if this is not the content resource of a page but the cpp:Page, the markdown will contain all subpages as well!
     * @param out      destination where the markdown rendering will be written.
     * @param request
     * @param response
     */
    void approximateMarkdown(@Nullable Resource resource, @Nonnull PrintWriter out,
                             @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response);

    /**
     * Returns a markdown representation of an attribute value, which might be plain text or HTML. We determine whether
     * it's HTML heuristically - in that case it's transformed to markdown, otherwise we just return the value.
     */
    @Nonnull
    String getMarkdown(@Nullable String value);

    /**
     * Determines whether there are links saved in the component that could be used as a proposal for the user to be used as source for the AI via markdown generation etc.
     *
     * @param resource the resource to check
     * @return a list of links, or an empty list if there are none.
     */
    @Nonnull
    List<Link> getComponentLinks(@Nullable Resource resource);

    /**
     * Retrieves the imageURL in a way useable for ChatGPT - usually data:image/jpeg;base64,{base64_image}
     */
    @Nullable
    String getImageUrl(@Nullable Resource imageResource);

    /**
     * A link from a component.
     *
     * @see #getComponentLinks(Resource)
     */
    class Link {
        private final String path;
        private final String title;

        public Link(String path, String title) {
            this.path = path;
            this.title = title;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }
    }

}
