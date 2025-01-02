package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateMergeModel {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateMergeModel.class);

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @OSGiService
    private LiveRelationshipManager liveRelationshipManager;

    @Self
    private SlingHttpServletRequest request;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled();
    }

    public String getHelloWorld() {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();
        return "Hello World! " + suffix;
    }

    /**
     * Finds the page that is in the request suffix.
     */
    protected Resource getResource() {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();
        if (suffix != null) {
            return request.getResourceResolver().getResource(suffix);
        }
        return null;
    }

    /**
     * Recursively finds all properties from the #getResource() and its children that have names starting with
     * {@link AITranslatePropertyWrapper#AI_NEW_TRANSLATED_SUFFIX} and creates the AI Translate Property Wrapper for each.
     */
    public List<AITranslatePropertyWrapper> getProperties() {
        List<AITranslatePropertyWrapper> list = new ArrayList<>();
        descendantsStream(getResource()).forEach(resource -> {
            ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            try {
                LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
                if (relationship != null) {
                    String sourcePath = relationship.getSourcePath();
                    Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
                    if (sourceResource != null) {
                        for (String key : properties.keySet()) {
                            if (key.startsWith(AITranslatePropertyWrapper.AI_PREFIX) &&
                                    key.endsWith(AITranslatePropertyWrapper.AI_NEW_TRANSLATED_SUFFIX)) {
                                String propertyName = key.substring(AITranslatePropertyWrapper.AI_PREFIX.length(),
                                        key.length() - AITranslatePropertyWrapper.AI_NEW_TRANSLATED_SUFFIX.length());
                                LOG.debug("Found property: {}", propertyName);
                                AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                                if (StringUtils.isNotBlank(wrapper.getNewOriginalCopy()) && StringUtils.isNotBlank(wrapper.getNewTranslatedCopy())) {
                                    list.add(wrapper);
                                } else { // should not happen
                                    LOG.warn("Property {} has empty original or translated copy", propertyName);
                                }
                            }
                        }
                    }
                }
            } catch (WCMException e) {
                LOG.error("Could not determine relationships of " + resource.getPath(), e);
            }
        });
        return list;
    }

    protected Stream<Resource> descendantsStream(Resource resource) {
        if (resource == null) {
            return Stream.empty();
        }
        List<Resource> children = IterableUtils.toList(resource.getChildren());
        return Stream.concat(
                Stream.of(resource),
                children.stream().flatMap(this::descendantsStream));
    }

}
