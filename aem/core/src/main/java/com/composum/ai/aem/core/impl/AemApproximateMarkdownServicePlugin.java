package com.composum.ai.aem.core.impl;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;
import static com.day.cq.commons.jcr.JcrConstants.JCR_DESCRIPTION;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.crx.JcrConstants;


/**
 * Special handling for cq:PageContent and components
 */
@Component(service = ApproximateMarkdownServicePlugin.class,
        // lower priority than HtmlToApproximateMarkdownServicePlugin since that does do a better job on experience fragments / content fragments if enabled
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
public class AemApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(AemApproximateMarkdownServicePlugin.class);

    /**
     * If a resource renders as a resource type matching that pattern, we ignore it completely, including child nodes.
     */
    protected static final Pattern FULLY_IGNORED_TYPES = Pattern.compile("core/wcm/components/list/v./list");

    protected static final Pattern TEASER_TYPES = Pattern.compile("core/wcm/components/teaser/v./teaser");

    protected static final Pattern EXPERIENCEFRAGMENT_TYPES = Pattern.compile("core/wcm/components/experiencefragment/v./experiencefragment");

    protected static final Pattern CONTENTFRAGMENT_TYPES = Pattern.compile("core/wcm/components/contentfragment/v./contentfragment");

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public @Nonnull PluginResult maybeHandle(
            @Nonnull Resource resource, @Nonnull PrintWriter out,
            @Nonnull ApproximateMarkdownService service,
            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        if (renderDamAssets(resource, out, response)) {
            return PluginResult.HANDLED_ALL;
        }
        if (resourceRendersAsComponentMatching(resource, FULLY_IGNORED_TYPES)) {
            return PluginResult.HANDLED_ALL;
        }
        if (pageHandling(resource, out, service)) {
            return PluginResult.HANDLED_ATTRIBUTES;
        }
        if (handleTeaser(resource, out, service) || handleExperienceFragment(resource, out, service, request, response)
                || handleContentFragment(resource, out, service)) {
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
            // out.println("Content of page " + path + " :\n\n"); // confuses the summary, not sure whether needed for something.

            String title = vm.get(JCR_TITLE, String.class);
            if (StringUtils.isNotBlank(title)) {
                out.println("# " + service.getMarkdown(title) + "\n");
            }
            outputIfNotBlank(out, vm, "shortDescription", service);
            outputIfNotBlank(out, vm, JCR_DESCRIPTION, service);
            out.println();
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
    protected boolean handleExperienceFragment(Resource resource, PrintWriter out, ApproximateMarkdownService service,
                                               SlingHttpServletRequest request, SlingHttpServletResponse response) {
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
                    service.approximateMarkdown(referencedResource, out, request, response);
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
                    renderReferencedContentFragment(resource, out, service, referencedResource, variation, reference, elementNames);
                } else {
                    LOG.warn("Resource {} referenced from {} attribute {} not found.", reference, resource.getPath(), "fragmentPath");
                }
            }
            return true;
        } else if (resource.getPath().startsWith("/content/dam") && Boolean.TRUE == resource.getValueMap().get("contentFragment", Boolean.class)) {
            // somewhat dubious: master might not the right one, but we don't have the variation name.
            renderReferencedContentFragment(resource, out, service, resource.getParent(), "master", resource.getPath(), null);
            return true;
        }
        return false;
    }

    protected void renderReferencedContentFragment(
            Resource resource, PrintWriter out, ApproximateMarkdownService service,
            Resource referencedResource, String variation, String reference, String[] elementNames) {
        Resource dataNode = referencedResource.getChild("jcr:content/data");
        String title = referencedResource.getValueMap().get("jcr:content/jcr:title", String.class);
        if (StringUtils.isNotBlank(title)) {
            out.println("## " + title);
        }
        Map<String, String> elementLabels = new LinkedHashMap<>();
        Map<String, Integer> listOrder = new LinkedHashMap<>();
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
                    String fieldLabel = r.getValueMap().get("fieldLabel",
                            r.getValueMap().get("fieldDescription",
                                    r.getValueMap().get("cfm-element", name)));
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
        if (traversed.getValueMap().get("name") != null) {
            list.add(traversed);
        } else {
            for (Resource child : traversed.getChildren()) {
                listModelResources(list, child);
            }
        }
        return list;
    }

    /**
     * If the resource is a dam:Asset or a dam:AssetContent jcr:content then we return an image link
     */
    protected boolean renderDamAssets(Resource resource, PrintWriter out, SlingHttpServletResponse response) {
        Resource assetNode = resource;
        if (resource.isResourceType("dam:AssetContent")) {
            assetNode = resource.getParent();
        }
        if (assetNode.isResourceType("dam:Asset")) {
            String mimeType = assetNode.getValueMap().get("jcr:content/metadata/dc:format", String.class);
            if (StringUtils.startsWith(mimeType, "image/")) {
                String name = StringUtils.defaultString(assetNode.getValueMap().get("jcr:content/jcr:title", String.class), assetNode.getName());
                out.println("![" + name + "](" + assetNode.getPath());
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
        Resource assetNode = imageResource;
        if (imageResource.isResourceType("dam:AssetContent")) {
            assetNode = imageResource.getParent();
        }
        if (assetNode.isResourceType("dam:Asset")) {
            String mimeType = assetNode.getValueMap().get("jcr:content/metadata/dc:format", String.class);
            Resource originalRendition = assetNode.getChild("jcr:content/renditions/original/jcr:content");
            if (StringUtils.startsWith(mimeType, "image/") && originalRendition != null) {
                try (InputStream is = originalRendition.adaptTo(InputStream.class)) {
                    if (is == null) {
                        LOG.warn("Unable to get InputStream from image resource {}", assetNode.getPath());
                        return null;
                    }
                    byte[] data = IOUtils.toByteArray(is);
                    data = resizeToMaxSize(data, mimeType, 512);
                    return "data:" + mimeType + ";base64," + new String(Base64.getEncoder().encode(data));
                } catch (IOException e) {
                    LOG.warn("Unable to get InputStream from image resource {}", assetNode.getPath(), e);
                }
            }
        }
        return null;
    }

    /**
     * We resize the image to a maximum width and height of maxSize, keeping the aspect ratio. If it's smaller, it's
     * returned as is. It could be of types image/jpeg, image/png or image/gif .
     */
    protected byte[] resizeToMaxSize(@Nonnull byte[] imageData, String mimeType, int maxSize) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(inputStream);
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return imageData;
        }
        double factor = maxSize * 1.0 / (Math.max(width, height) + 1);
        int newWidth = (int) (width * factor);
        int newHeight = (int) (height * factor);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        resizedImage.createGraphics().drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, mimeType.substring("image/".length()), outputStream);
        return outputStream.toByteArray();
    }

    @Nonnull
    @Override
    public Collection<ApproximateMarkdownService.Link> getMasterLinks(Resource resource) {
        if (resource == null) {
            return Collections.emptyList();
        }
        try {
            LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(resource, false);
            if (liveRelationship != null) {
                String masterPath = liveRelationship.getSourcePath();
                Resource master = masterPath != null ? resource.getResourceResolver().getResource(masterPath) : null;
                if (master != null) {
                    ApproximateMarkdownService.Link componentLink =
                            new ApproximateMarkdownService.Link(master.getPath(), "Text in livecopy blueprint", false);
                    String pagePath = masterPath.substring(0, masterPath.lastIndexOf("/jcr:content") + 12);
                    ApproximateMarkdownService.Link pageLink =
                            new ApproximateMarkdownService.Link(pagePath, "Blueprint Page", false);
                    return Arrays.asList(componentLink, pageLink);
                }
            }
        } catch (WCMException e) {
            LOG.error("Cannot get live relationships of " + resource.getPath(), e);
        }
        return Collections.emptyList();
    }
}
