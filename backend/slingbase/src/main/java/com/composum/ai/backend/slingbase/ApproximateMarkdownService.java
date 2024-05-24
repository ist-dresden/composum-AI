package com.composum.ai.backend.slingbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

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
     * Retrieves the text content for an URL.
     */
    @Nonnull
    String getMarkdown(@Nonnull URI uri) throws MalformedURLException, IOException, IllegalArgumentException;

    /**
     * Returns a number of links that are saved in the component or siblings of the component that could be used as
     * a proposal for the user to be used as source for the AI via markdown generation etc.
     * This heuristically collects a number of links that might be interesting.
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
        private final boolean needsVision;

        public Link(String path, String title, boolean needsVision) {
            this.path = path;
            this.title = title;
            this.needsVision = needsVision;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }

        public boolean isNeedsVision() {
            return needsVision;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Link)) return false;
            Link link = (Link) object;
            return Objects.equals(getPath(), link.getPath()) && Objects.equals(getTitle(), link.getTitle())
                    && needsVision == link.needsVision;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPath(), getTitle(), needsVision);
        }

        @Override
        public String toString() {
            return "Link{" +
                    "path='" + path + '\'' +
                    ", title='" + title + '\'' +
                    ", needsVision=" + needsVision +
                    '}';
        }
    }

}
