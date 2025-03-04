package com.composum.ai.aem.core.impl;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.IterableUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * Wrapper around {@link org.apache.sling.api.resource.Resource} that wraps operations around cancelling / reenabling inheritance.
 */
public class ComponentCancellationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentCancellationHelper.class);

    /**
     * Checks whether a resource has a sling:resourceType that is of type cq:Component.
     */
    public static boolean isComponent(@Nonnull Resource resource) {
        return "cq:Component".equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class));
    }

    public static boolean isPageContent(@Nonnull Resource resource) {
        return "cq:PageContent".equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class));
    }

    public static boolean isPage(@Nonnull Resource resource) {
        return "cq:Page".equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class));
    }

    /**
     * Finds next higher cq:Component in the resource tree, or the cq:PageContent if that is the next higher component.
     */
    public static Resource findNextHigherCancellableComponent(@Nonnull Resource resource) {
        if (isComponent(resource) || isPageContent(resource)) {
            return resource;
        }
        if (isPage(resource)) {
            return null;
        }
        Resource parent = resource.getParent();
        return parent == null ? null : findNextHigherCancellableComponent(parent);
    }

    /**
     * Tries to guess whether the resource is actually a container (where inheritance cancelling is shallow only.)
     * We guess this if the sling:resourceType has "cq:isContainer" true, or heuristics match:
     * if it has a lonely "items" child or one of its children or childrens children is a component.
     */
    public static boolean isContainer(@Nonnull Resource resource) {
        if (!isComponent(resource)) {
            return false;
        }
        List<Resource> children = IterableUtils.toList(resource.getChildren());
        if (children.size() == 1 && children.get(0).getName().equals("items")) {
            return true; // this is a guess, but likely right.
        }
        String resourceType = resource.getValueMap().get("sling:resourceType", String.class);
        ResourceResolver resolver = resource.getResourceResolver();
        Resource resourceTypeResource = resolver.getResource(resourceType);
        if (resourceTypeResource == null) { // should be impossible
            LOG.warn("Bug: Resource type {} not found for ", resourceType, resource.getPath());
            return false;
        }
        Resource overrideResource = resolver.getResource("/mnt/override" + resourceTypeResource.getPath());
        if (overrideResource == null) {
            LOG.warn("Bug: can't find override resource for {}", resourceTypeResource.getPath());
            overrideResource = resourceTypeResource;
        }
        boolean hasIsContainer = overrideResource.getValueMap().get("cq:isContainer", false);
        if (hasIsContainer) {
            return true;
        }
        // if one of the children is a component, we assume this is a container.
        for (Resource child : children) {
            if (isComponent(child)) {
                return true;
            }
        }
        // if one of the children's children is a component, we assume this is a container.
        for (Resource child : children) {
            if (IterableUtils.matchesAny(child.getChildren(), ComponentCancellationHelper::isComponent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * We call {@link AITranslatePropertyWrapper#adjustForReenableInheritance()} on all properties that are *not* cancelled now.
     */
    public static void adjustPropertiesReenableInheritance(LiveRelationshipManager liveRelationshipManager, Resource resource) throws WCMException {
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, true);
        if (relationship == null) {
            return;
        }
        Resource source = resource.getResourceResolver().getResource(relationship.getSourcePath());
        if (source == null) { // shouldn't happen
            LOG.warn("Bug: source resource {} not found for {}", relationship.getSourcePath(), resource.getPath());
            return;
        }
        if (isContainer(resource)) {
            for (Resource child : resource.getChildren()) {
                adjustPropertiesReenableInheritance(liveRelationshipManager, child);
            }
        }
    }
}
