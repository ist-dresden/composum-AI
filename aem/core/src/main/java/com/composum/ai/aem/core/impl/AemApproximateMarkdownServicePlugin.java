package com.composum.ai.aem.core.impl;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

// export as OSGI service

public class AemApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AemApproximateMarkdownServicePlugin.class);

    @Override
    public @Nonnull PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out, @Nonnull ApproximateMarkdownService service) {
        boolean wasHandledAsPage = pageHandling(resource, out, service);
        handleContentReference(resource, out, service);
        if (wasHandledAsPage) {
            return PluginResult.HANDLED_ATTRIBUTES;
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
            out.println("Content of page " + path + " in markdown syntax starts now:\n\n");

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
