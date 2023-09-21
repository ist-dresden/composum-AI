package com.composum.ai.composum.bundle.service;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

/**
 * Special handling for composum/pages/components/page and components
 */
@Component(service = ApproximateMarkdownServicePlugin.class)
public class ComposumApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ComposumApproximateMarkdownServicePlugin.class);

    @Override
    public @NotNull PluginResult maybeHandle(@NotNull Resource resource, @NotNull PrintWriter out, @Nonnull ApproximateMarkdownService service) {
        boolean wasHandledAsPage = pageHandling(resource, out, service);
        boolean wasHandledAsTable = !wasHandledAsPage && tableHandling(resource, out, service);
        handleContentReference(resource, out, service);
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
            out.println("Content of page " + path + " :\n\n");

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

    protected void handleContentReference(Resource resource, PrintWriter out, ApproximateMarkdownService service) {
        String reference = resource.getValueMap().get("contentReference", String.class);
        if (StringUtils.startsWith(reference, "/content/")) {
            Resource referencedResource = resource.getResourceResolver().getResource(reference);
            if (referencedResource != null) {
                service.approximateMarkdown(referencedResource, out);
            } else {
                LOG.info("Resource {} referenced from {} not found.", reference, resource.getPath());
            }
        }
    }


}
