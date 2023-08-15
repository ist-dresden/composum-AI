package com.composum.ai.aem.core.impl;

import static com.day.cq.commons.jcr.JcrConstants.JCR_DESCRIPTION;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;

import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;
import com.day.crx.JcrConstants;


/**
 * Special handling for cq:PageContent and components
 */
@Component(service = ApproximateMarkdownServicePlugin.class)
public class AemApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AemApproximateMarkdownServicePlugin.class);

    /**
     * If a resource renders as a resource type matching that pattern, we ignore it completely, including child nodes.
     */
    protected static final Pattern FULLY_IGNORED_TYPES = Pattern.compile("core/wcm/components/list/v?/list");

    protected static final Pattern TEASER_TYPES = Pattern.compile("core/wcm/components/teaser/v?/teaser");

    @Override
    public @Nonnull PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out, @Nonnull ApproximateMarkdownService service) {
        if (resourceRendersAsComponentMatching(resource, FULLY_IGNORED_TYPES)) {
            return PluginResult.HANDLED_ALL;
        }
        if (pageHandling(resource, out, service)) {
            return PluginResult.HANDLED_ATTRIBUTES;
        }
        if (handleTeaser(resource, out, service)) {
            return PluginResult.HANDLED_ALL;
        }
        return PluginResult.NOT_HANDLED;
    }

    /**
     * Prints title and meta attributes, then continues to normal handling.
     * <p>
     * ??? pageTitle vs. jcr:title , shortDescription
     */
    protected boolean pageHandling(Resource resource, PrintWriter out, @Nonnull ApproximateMarkdownService helper) {
        ValueMap vm = resource.getValueMap();
        boolean isPage = vm.get(JcrConstants.JCR_PRIMARYTYPE, String.class).equals("cq:PageContent");
        if (isPage) {
            String path = resource.getParent().getPath(); // we don't want the content node's path but the parent's
            out.println("Content of page " + path + " in markdown syntax starts now:\n\n");

            String title = vm.get(JCR_TITLE, String.class);
            String description = vm.get(JCR_DESCRIPTION, String.class);
            String shortDescription = vm.get("shortDescription", String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + helper.getMarkdown(title) + "\n");
            }
            if (StringUtils.isNotBlank(shortDescription)) {
                out.println(helper.getMarkdown(shortDescription));
                out.println("\n");
            }
            if (StringUtils.isNotBlank(description)) {
                out.println(helper.getMarkdown(description));
                out.println("\n");
            }
        }
        return isPage;
    }

    /**
     * Creates markdown for core/wcm/components/teaser/v1/teaser and derived components.
     *
     * @see "https://github.com/adobe/aem-core-wcm-components/blob/main/content/src/content/jcr_root/apps/core/wcm/components/teaser/v1/teaser/README.md"
     */
    protected boolean handleTeaser(Resource resource, PrintWriter out, ApproximateMarkdownService service) {
        if (resourceRendersAsComponentMatching(resource, TEASER_TYPES)) {
            ValueMap vm = resource.getValueMap();
            outputIfNotBlank(out, vm, "pretitle");
            outputIfNotBlank(out, vm, "title");
            outputIfNotBlank(out, vm, JCR_TITLE);
            outputIfNotBlank(out, vm, JCR_DESCRIPTION);
            Resource actions = resource.getChild("action");
            if (actions != null) {
                for (Resource action : actions.getChildren()) {
                    ValueMap actionVm = action.getValueMap();
                    outputIfNotBlank(out, actionVm, "text");
                    String link = actionVm.get("link", String.class);
                    if (StringUtils.isNotBlank(link)) {
                        Resource linkedPage = resource.getResourceResolver().getResource(link);
                        if (linkedPage != null) {
                            outputIfNotBlank(out, linkedPage.getValueMap(), JCR_TITLE);
                            outputIfNotBlank(out, linkedPage.getValueMap(), JCR_DESCRIPTION);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void outputIfNotBlank(@Nonnull PrintWriter out, @Nonnull ValueMap vm, @Nonnull String attribute) {
        String value = vm.get(attribute, String.class);
        if (StringUtils.isNotBlank(value)) {
            out.println(value);
        }
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
