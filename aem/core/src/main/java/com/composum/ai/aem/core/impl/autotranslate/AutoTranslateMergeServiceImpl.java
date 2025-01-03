package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.commons.collections4.IterableUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * Implementation of the AutoTranslateMergeService.
 * Provides methods to retrieve properties for resources in the context of translations.
 */
@Component(service = AutoTranslateMergeService.class)
public class AutoTranslateMergeServiceImpl implements AutoTranslateMergeService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateMergeServiceImpl.class);

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public List<AutoTranslateProperty> getProperties(Resource pageResource) {
        List<AutoTranslateProperty> list = new ArrayList<>();
        descendantsStream(pageResource).forEach(res -> {
            ModifiableValueMap properties = res.adaptTo(ModifiableValueMap.class);
            try {
                LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(res, false);
                if (relationship != null) {
                    String sourcePath = relationship.getSourcePath();
                    Resource sourceResource = res.getResourceResolver().getResource(sourcePath);
                    if (sourceResource != null) {
                        for (String key : properties.keySet()) {
                            String propertyName = AITranslatePropertyWrapper.decodePropertyName(
                                    AITranslatePropertyWrapper.AI_PREFIX, key,
                                    AITranslatePropertyWrapper.AI_NEW_TRANSLATED_SUFFIX, res);
                            if (propertyName != null) {
                                LOG.debug("Found property: {}", propertyName);
                                AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                                if (StringUtils.isNotBlank(wrapper.getNewOriginalCopy()) && StringUtils.isNotBlank(wrapper.getNewTranslatedCopy())) {
                                    list.add(new AutoTranslateProperty(res.getPath(), wrapper));
                                } else {
                                    LOG.warn("Property {} has empty original or translated copy", propertyName);
                                }
                            }
                        }
                    }
                }
            } catch (WCMException e) {
                LOG.error("Could not determine relationships of " + res.getPath(), e);
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
