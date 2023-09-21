package com.composum.ai.aem.core.impl;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;
import static com.day.cq.commons.jcr.JcrConstants.JCR_DESCRIPTION;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    protected static final Pattern FULLY_IGNORED_TYPES = Pattern.compile("core/wcm/components/list/v./list");

    protected static final Pattern TEASER_TYPES = Pattern.compile("core/wcm/components/teaser/v./teaser");

    protected static final Pattern EXPERIENCEFRAGMENT_TYPES = Pattern.compile("core/wcm/components/experiencefragment/v./experiencefragment");

    protected static final Pattern CONTENTFRAGMENT_TYPES = Pattern.compile("core/wcm/components/contentfragment/v./contentfragment");

    @Override
    public @Nonnull PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out, @Nonnull ApproximateMarkdownService service) {
        if (resourceRendersAsComponentMatching(resource, FULLY_IGNORED_TYPES)) {
            return PluginResult.HANDLED_ALL;
        }
        if (pageHandling(resource, out, service)) {
            return PluginResult.HANDLED_ATTRIBUTES;
        }
        if (handleTeaser(resource, out, service) || handleExperienceFragment(resource, out, service) || handleContentFragment(resource, out, service)) {
            return PluginResult.HANDLED_ALL;
        }
        return PluginResult.NOT_HANDLED;
    }

    /**
     * Prints title and meta attributes, then continues to normal handling.
     * <p>
     * ??? pageTitle vs. jcr:title , shortDescription
     */
    protected boolean pageHandling(Resource resource, PrintWriter out, @Nonnull ApproximateMarkdownService service) {
        ValueMap vm = resource.getValueMap();
        boolean isPage = Objects.equals(vm.get(JcrConstants.JCR_PRIMARYTYPE, String.class), "cq:PageContent");
        if (isPage) {
            String path = resource.getParent().getPath(); // we don't want the content node's path but the parent's
            out.println("Content of page " + path + " :\n\n");

            String title = vm.get(JCR_TITLE, String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + service.getMarkdown(title) + "\n");
            }
            outputIfNotBlank(out, vm, "shortDescription", service);
            outputIfNotBlank(out, vm, JCR_DESCRIPTION, service);
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
            outputIfNotBlank(out, vm, "pretitle", service);
            outputIfNotBlank(out, vm, "title", service);
            outputIfNotBlank(out, vm, JCR_TITLE, service);
            boolean titleFromPage = vm.get("titleFromPage", false);
            boolean descriptionFromPage = vm.get("descriptionFromPage", false);
            Resource actions = resource.getChild("actions");
            if (actions != null) {
                for (Resource action : actions.getChildren()) {
                    Resource linkedPage = getLinkedPage(action);
                    if (titleFromPage && linkedPage != null) {
                        outputIfNotBlank(out, linkedPage.getValueMap(), JCR_TITLE, service);
                    }
                }
            }

            outputIfNotBlank(out, vm, JCR_DESCRIPTION, service);

            if (actions != null) {
                for (Resource action : actions.getChildren()) {
                    ValueMap actionVm = action.getValueMap();
                    String text = actionVm.get("text", String.class);
                    String link = actionVm.get("link", String.class);
                    if (StringUtils.isNotBlank(text)) {
                        out.println("[" + text + "](" + link + ")\n");
                    }
                    Resource linkedPage = getLinkedPage(action);
                    if (descriptionFromPage && linkedPage != null) {
                        outputIfNotBlank(out, linkedPage.getValueMap(), JCR_DESCRIPTION, service);
                    }
                }
            }
            return true;
        }
        return false;
    }

    protected @Nullable Resource getLinkedPage(Resource action) {
        ValueMap actionVm = action.getValueMap();
        String link = actionVm.get("link", String.class);
        if (StringUtils.isNotBlank(link)) {
            Resource linkedPage = action.getResourceResolver().getResource(link);
            if (linkedPage != null) {
                linkedPage = linkedPage.getChild(JCR_CONTENT);
                if (linkedPage != null) {
                    return linkedPage;
                }
            }
        }
        return null;
    }

    private void outputIfNotBlank(@Nonnull PrintWriter out, @Nonnull ValueMap vm, @Nonnull String attribute, ApproximateMarkdownService service) {
        String value = vm.get(attribute, String.class);
        if (StringUtils.isNotBlank(value)) {
            out.println(service.getMarkdown(value));
        }
    }

    /**
     * Creates markdown for core/wcm/components/experiencefragment/v1/experiencefragment and derived components.
     *
     * @see "https://github.com/adobe/aem-core-wcm-components/blob/main/content/src/content/jcr_root/apps/core/wcm/components/experiencefragment/v2/experiencefragment/README.md"
     */
    protected boolean handleExperienceFragment(Resource resource, PrintWriter out, ApproximateMarkdownService service) {
        if (resourceRendersAsComponentMatching(resource, EXPERIENCEFRAGMENT_TYPES)) {
            String reference = resource.getValueMap().get("fragmentVariationPath", String.class);
            if (StringUtils.startsWith(reference, "/content/")) {
                Resource referencedResource = resource.getResourceResolver().getResource(reference);
                if (referencedResource != null) {
                    if (referencedResource.getChild(JCR_CONTENT) != null) {
                        referencedResource = referencedResource.getChild(JCR_CONTENT);
                        if (referencedResource.getChild("root") != null) {
                            referencedResource = referencedResource.getChild("root");
                        }
                    }
                    service.approximateMarkdown(referencedResource, out);
                } else {
                    LOG.warn("Resource {} referenced from {} attribute {} not found.", reference, resource.getPath(), "fragmentVariationPath");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Creates markdown for core/wcm/components/contentfragment/v1/contentfragment and derived components.
     *
     * @see "https://github.com/adobe/aem-core-wcm-components/blob/main/content/src/content/jcr_root/apps/core/wcm/components/contentfragment/v1/contentfragment/README.md"
     */
    protected boolean handleContentFragment(Resource resource, PrintWriter out, ApproximateMarkdownService service) {
        if (resourceRendersAsComponentMatching(resource, CONTENTFRAGMENT_TYPES)) {
            String reference = resource.getValueMap().get("fragmentPath", String.class);
            if (StringUtils.startsWith(reference, "/content/")) {
                String variation = resource.getValueMap().get("variationName", "master");
                String[] elementNames = resource.getValueMap().get("elementNames", String[].class);
                Resource referencedResource = resource.getResourceResolver().getResource(reference);
                if (referencedResource != null) {
                    Resource dataNode = referencedResource.getChild("jcr:content/data");
                    Map<String, String> elementLabels = new HashMap<>();
                    Map<String, Integer> listOrder = new HashMap<>();
                    findElementLabels(dataNode, elementLabels, listOrder);
                    if (referencedResource.getChild("jcr:content/data/" + variation) != null) {
                        referencedResource = referencedResource.getChild("jcr:content/data/" + variation);
                    } else {
                        LOG.warn("Content fragment {} referenced in {} does not have a variation named {}.", reference, resource.getPath(), variation);
                    }
                    ValueMap vm = referencedResource.getValueMap();
                    if (elementNames == null) {
                        elementNames = listOrder.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .toArray(String[]::new);
                        if (elementNames.length == 0) {
                            // all attributes that are not jcr: and don't end with _LastModified
                            elementNames = vm.keySet().stream()
                                    .filter(key -> !key.startsWith("jcr:") && !key.contains("@"))
                                    .toArray(String[]::new);
                        }
                    }
                    for (String elementName : elementNames) {
                        String value = vm.get(elementName, String.class);
                        String contentType = vm.get(elementName + "@ContentType", String.class);
                        if (StringUtils.isNotBlank(value)) {
                            if ("text/html".equals(contentType)) {
                                value = service.getMarkdown(value);
                            }
                            String label = elementLabels.get(elementName);
                            if (StringUtils.isNotBlank(label)) {
                                out.println(label + ": " + value);
                            } else {
                                out.println(value);
                            }
                        }
                    }
                } else {
                    LOG.warn("Resource {} referenced from {} attribute {} not found.", reference, resource.getPath(), "fragmentPath");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Looks for the cq:model and determines the labels.
     */
    protected void findElementLabels(Resource dataNode, Map<String, String> labels, Map<String, Integer> listOrder) {
        if (dataNode != null) {
            String cqModelPath = dataNode.getValueMap().get("cq:model", String.class);
            Resource cqModel = dataNode.getResourceResolver().getResource(cqModelPath);
            if (cqModel != null) {
                // recursively search all cq:model descendants for elements with fieldLabel and name
                List<Resource> modelResources = listModelResources(new ArrayList<>(), cqModel);
                for (Resource r : modelResources) {
                    String name = r.getValueMap().get("name", String.class);
                    String fieldLabel = r.getValueMap().get("fieldLabel", String.class);
                    String listOrderString = r.getValueMap().get("listOrder", String.class);
                    if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(fieldLabel)) {
                        labels.put(name, fieldLabel);
                        if (StringUtils.isNotBlank(listOrderString)) {
                            try {
                                listOrder.put(name, Integer.parseInt(listOrderString));
                            } catch (NumberFormatException e) {
                                LOG.warn("Unable to parse listOrder {} for element {}.", listOrderString, name);
                            }
                        }
                    }
                }
            } else {
                LOG.warn("cq:model {} referenced from {} not found.", cqModelPath, dataNode.getPath());
            }
        }
    }

    // recursively search all cq:model descendants for elements with fieldLabel and name
    protected List<Resource> listModelResources(List<Resource> list, Resource traversed) {
        if (traversed.getValueMap().get("fieldLabel") != null && traversed.getValueMap().get("name") != null) {
            list.add(traversed);
        } else {
            for (Resource child : traversed.getChildren()) {
                listModelResources(list, child);
            }
        }
        return list;
    }

}
