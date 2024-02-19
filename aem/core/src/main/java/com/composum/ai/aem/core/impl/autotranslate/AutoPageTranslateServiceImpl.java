package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.RangeIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * <p>
 * Translated would normally be properties that "obviously" contain text, like jcr:title, jcr:description, text, title
 * etc. (Let's search the wcm core components documentation for that), and properties that heuristically "look like text",
 * that is, contain multiple whitespace sequences. Since that's bound to fail sometimes, we later need a rule configuration
 * mechanism in the OSGI configuration that defines positive / negative exceptions, but that's not in scope for now.
 * </p> <p>
 * We save property values : the property value before the translation is saved with prefix `ai_original_` and the
 * property name, and the property value after the translation is saved with prefix `ai_translated_ and the property name.
 * </p>
 */
@Component
public class AutoPageTranslateServiceImpl implements AutoPageTranslateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoPageTranslateServiceImpl.class);

    /**
     * Prefix for the property name of a property that saves the original value of the property, to track when it has to be re-translated.
     */
    public static final String AI_ORIGINAL_PREFIX = "ai_original_";

    /**
     * Prefix for the property name of a property that saves the translated value of the property, to track whether it has been manually changed after automatic translation.
     */
    public static final String AI_TRANSLATED_PREFIX = "ai_translated_";

    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title");

    @Reference
    private GPTTranslationService translationService;

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public void translateLiveCopy(Resource resource) throws WCMException {
        List<LiveRelationship> relationships = new ArrayList<>();
        for (RangeIterator it = liveRelationshipManager.getLiveRelationships(resource, null, null); it.hasNext(); ) {
            relationships.add((LiveRelationship) it.next());
        }

        List<PropertyToTranslate> propertiesToTranslate = new ArrayList<>();
        collectPropertiesToTranslate(resource, propertiesToTranslate);
        LOG.info("Set of property names to translate in {} : {}", resource.getPath(),
                propertiesToTranslate.stream()
                        .map(propertyToTranslate -> propertyToTranslate.propertyName).collect(Collectors.toSet()));
        LOG.warn("Translating {} properties", propertiesToTranslate.size());
    }

    /**
     * Searches for properties we have to translate.
     */
    protected List<PropertyToTranslate> collectPropertiesToTranslate(Resource resource, List<PropertyToTranslate> propertiesToTranslate) {
        ValueMap valueMap = resource.getValueMap();
        for (String propertyName : valueMap.keySet()) {
            if (isTranslatableProperty(propertyName, valueMap.get(propertyName))) {
                if (!valueMap.containsKey(AI_ORIGINAL_PREFIX + propertyName)) {
                    PropertyToTranslate propertyToTranslate = new PropertyToTranslate();
                    propertyToTranslate.resource = resource;
                    propertyToTranslate.propertyName = propertyName;
                    propertiesToTranslate.add(propertyToTranslate);
                }
            }
        }
        for (Resource child : resource.getChildren()) {
            collectPropertiesToTranslate(child, propertiesToTranslate);
        }
        return propertiesToTranslate;
    }

    protected boolean isTranslatableProperty(String name, Object value) {
        if (CERTAINLY_TRANSLATABLE_PROPERTIES.contains(name)) {
            return true;
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            return stringValue.contains("  ") && // heuristic for a start.
                    !name.startsWith(AI_ORIGINAL_PREFIX) && !name.startsWith(AI_TRANSLATED_PREFIX);
        }
        return false;
    }

    protected class PropertyToTranslate {
        private Resource resource;
        private String propertyName;

        @Override
        public String toString() {
            return resource.getPath() + "/" + propertyName;
        }
    }

}
