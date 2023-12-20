package com.composum.ai.composum.bundle.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

/**
 * Special handling for composum/pages/components/page and components
 */
@Component(service = ApproximateMarkdownServicePlugin.class,
        // higher priority than HtmlToApproximateMarkdownServicePlugin since this does a better job on tables
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
public class ComposumApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ComposumApproximateMarkdownServicePlugin.class);

    @Override
    public @NotNull PluginResult maybeHandle(
            @NotNull Resource resource, @NotNull PrintWriter out,
            @Nonnull ApproximateMarkdownService service,
            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        if (handleImage(resource, out, response)) {
            return PluginResult.HANDLED_ALL;
        }
        boolean wasHandledAsPage = pageHandling(resource, out, service);
        boolean wasHandledAsTable = !wasHandledAsPage && tableHandling(resource, out, service);
        handleContentReference(resource, out, service, request, response);
        if (wasHandledAsPage) {
            return PluginResult.HANDLED_ATTRIBUTES;
        } else if (wasHandledAsTable) {
            return PluginResult.HANDLED_ALL;
        } else {
            return PluginResult.NOT_HANDLED;
        }
    }

    /**
     * Prints title and meta attributes, then continues to normal handling.
     */
    protected boolean pageHandling(Resource resource, PrintWriter out, @Nonnull ApproximateMarkdownService helper) {
        boolean isPage = resource.getResourceType().equals("composum/pages/components/page");
        if (isPage) {
            String path = resource.getParent().getPath(); // we don't want the content node's path but the parent's
            // out.println(" " + path + " :\n\n"); // not sure whether that's needed

            String title = resource.getValueMap().get("jcr:title", String.class);
            String description = resource.getValueMap().get("jcr:description", String.class);
            List<String> categories = resource.getValueMap().get("category", List.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + helper.getMarkdown(title) + "\n");
            }
            if (categories != null && !categories.isEmpty()) {
                out.println("Categories: " + categories.stream().collect(Collectors.joining(", ")));
            }
            if (StringUtils.isNotBlank(description)) {
                out.println(helper.getMarkdown(description));
            }
            out.println();
        }
        return isPage;
    }


    /**
     * If it's a table, handles everything including children.
     */
    protected boolean tableHandling(Resource resource, PrintWriter out, @Nonnull ApproximateMarkdownService helper) {
        boolean isTable = resource.getResourceType().equals("composum/pages/components/composed/table");
        if (isTable) {
            String title = resource.getValueMap().get("title", String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("#### " + helper.getMarkdown(title) + "\n");
            }
            // for each child of type "row" we print a line with the values of the children of type "cell"
            StreamSupport.stream(resource.getChildren().spliterator(), true)
                    .filter(row -> row.getResourceType().equals("composum/pages/components/composed/table/row"))
                    .forEach(row -> {
                        out.print("| ");
                        StreamSupport.stream(row.getChildren().spliterator(), true)
                                .filter(cell -> cell.getResourceType().equals("composum/pages/components/composed/table/cell"))
                                .map(cell -> cell.getValueMap().get("text", String.class))
                                .forEach(text -> out.print(helper.getMarkdown(text) + " | "));
                        out.println(" |");
                    });
            out.println();
        }
        return isTable;
    }

    protected void handleContentReference(Resource resource, PrintWriter out, ApproximateMarkdownService service,
                                          SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String reference = resource.getValueMap().get("contentReference", String.class);
        if (StringUtils.startsWith(reference, "/content/")) {
            Resource referencedResource = resource.getResourceResolver().getResource(reference);
            if (referencedResource != null) {
                service.approximateMarkdown(referencedResource, out, request, response);
            } else {
                LOG.info("Resource {} referenced from {} not found.", reference, resource.getPath());
            }
        }
    }

    /**
     * Handle resource that is a jcr:content of type nt:resource with a jcr:mimeType starting with image/
     * as a markdown image reference to that path.
     *
     * @return whether it was an image for which we have written a markdown reference
     */
    protected boolean handleImage(Resource resource, PrintWriter out, SlingHttpServletResponse response) {
        if (JcrConstants.JCR_CONTENT.equals(resource.getName()) && resource.isResourceType("nt:resource")) {
            String mimeType = resource.getValueMap().get("jcr:mimeType", String.class);
            if (StringUtils.startsWith(mimeType, "image/")) {
                String name = StringUtils.defaultString(resource.getValueMap().get("jcr:title", String.class), resource.getName());
                out.println("![" + name + "](" + resource.getParent().getPath() + ")");
                try {
                    response.addHeader(ApproximateMarkdownService.HEADER_IMAGEPATH, resource.getParent().getPath());
                } catch (RuntimeException e) {
                    LOG.warn("Unable to set header " + ApproximateMarkdownService.HEADER_IMAGEPATH + " to " + resource.getParent().getPath(), e);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the imageURL in a way useable for ChatGPT - usually data:image/jpeg;base64,{base64_image}
     */
    @Nullable
    @Override
    public String getImageUrl(@Nullable Resource imageResource) {
        Resource imageContentResource = imageResource;
        if (imageContentResource != null && imageContentResource.isResourceType("nt:file")) {
            imageContentResource = imageContentResource.getChild(JcrConstants.JCR_CONTENT);
        }
        if (imageContentResource != null && imageContentResource.isResourceType("nt:resource")) {
            String mimeType = imageContentResource.getValueMap().get("jcr:mimeType", String.class);
            if (StringUtils.startsWith(mimeType, "image/")) {
                try (InputStream is = imageContentResource.adaptTo(InputStream.class)) {
                    if (is == null) {
                        LOG.warn("Unable to get InputStream from image resource {}", imageContentResource.getPath());
                        return null;
                    }
                    byte[] data = is.readAllBytes();
                    return "data:" + mimeType + ";base64," + new String(Base64.getEncoder().encode(data));
                } catch (IOException e) {
                    LOG.warn("Unable to get InputStream from image resource {}", imageContentResource.getPath(), e);
                }
            }
        }
        return null;
    }
}
