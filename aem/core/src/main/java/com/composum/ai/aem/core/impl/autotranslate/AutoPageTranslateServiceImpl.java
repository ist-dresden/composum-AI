package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
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
 * We save property values : the property value before the translation is saved with prefix `ai_` and suffix `_original`
 * for the property name, and the property value after the translation is saved with prefix `ai_` and
 * suffix `_translated` for the property name.
 * </p>
 */
@Component
public class AutoPageTranslateServiceImpl implements AutoPageTranslateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoPageTranslateServiceImpl.class);

    /**
     * Boolean property that marks a resource as automatically translated by this process.
     */
    public static final String AI_TRANSLATED_MARKER = "ai_translated";

    /**
     * Prefix for property names of saved values.
     */
    public static final String AI_PREFIX = "ai_";

    /**
     * Suffix for the property name of a property that saves the original value of the property, to track when it
     * has to be re-translated.
     */
    public static final String AI_ORIGINAL_SUFFIX = "_original";

    /**
     * Suffix for the property name of a property that saves the translated value of the property, to track whether
     * it has been manually changed after automatic translation.
     */
    public static final String AI_TRANSLATED_SUFFIX = "_translated";

    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title");

    @Reference
    private GPTTranslationService translationService;

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public void translateLiveCopy(Resource resource) throws WCMException, PersistenceException {
        List<PropertyToTranslate> propertiesToTranslate = new ArrayList<>();
        collectPropertiesToTranslate(resource, propertiesToTranslate);
        LOG.info("Set of property names to translate in {} : {}", resource.getPath(),
                propertiesToTranslate.stream()
                        .map(propertyToTranslate -> propertyToTranslate.propertyName).collect(Collectors.toSet()));
        LOG.info("Translating {} properties in {}", propertiesToTranslate.size(), resource.getPath());
        List<String> valuesToTranslate = propertiesToTranslate.stream()
                .map(p -> p.resource.getValueMap().get(p.propertyName, String.class))
                .collect(Collectors.toList());
        // for testing something random but trackable.
        List<String> translatedValues = valuesToTranslate.stream().map(this::reverseString).collect(Collectors.toList());
        for (int i = 0; i < propertiesToTranslate.size(); i++) {
            PropertyToTranslate propertyToTranslate = propertiesToTranslate.get(i);
            String originalValue = valuesToTranslate.get(i);
            String translatedValue = translatedValues.get(i);
            String propertyName = propertyToTranslate.propertyName;
            Resource resourceToTranslate = propertyToTranslate.resource;
            LOG.info("Setting {} in {} to {}", propertyName, propertyToTranslate.resource.getPath(), translatedValue);
            ModifiableValueMap valueMap = resourceToTranslate.adaptTo(ModifiableValueMap.class);
            valueMap.put(AI_PREFIX + propertyName + AI_ORIGINAL_SUFFIX, originalValue);
            valueMap.put(AI_PREFIX + propertyName + AI_TRANSLATED_SUFFIX, translatedValue);
            valueMap.put(propertyName, translatedValue);
            valueMap.put(AI_TRANSLATED_MARKER, Boolean.TRUE);

            LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resourceToTranslate, false);
            if (!relationship.getStatus().isCancelled() && !relationship.getStatus().isCancelledForChildren()) {
                // experimentally, AEM cancels the relationship with "deep" in the UI when there are no child nodes.
                boolean deep = !resource.getChildren().iterator().hasNext();
                liveRelationshipManager.cancelRelationship(resource.getResourceResolver(), relationship,
                        deep, false);
            }
        }
        resource.getResourceResolver().commit();
    }

    /**
     * Searches for properties we have to translate.
     */
    protected List<PropertyToTranslate> collectPropertiesToTranslate(Resource resource, List<PropertyToTranslate> propertiesToTranslate) {
        ValueMap valueMap = resource.getValueMap();
        for (String propertyName : valueMap.keySet()) {
            if (isTranslatableProperty(propertyName, valueMap.get(propertyName))) {
                if (!valueMap.containsKey(AI_PREFIX + propertyName + AI_ORIGINAL_SUFFIX)) {
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

    protected final Pattern PATTERH_HAS_WHITESPACE = Pattern.compile("\\s.*\\s");

    /**
     * As additional heuristic - the text should have at least one word with >= 4 letters.
     * That will break down very different languages, I know, but this is a POC. :-)
     */
    protected final Pattern PATTERN_HAS_WORD = Pattern.compile("\\w{4}");

    protected boolean isTranslatableProperty(String name, Object value) {
        if (CERTAINLY_TRANSLATABLE_PROPERTIES.contains(name)) {
            return true;
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            return !(name.startsWith(AI_PREFIX) && name.endsWith(AI_TRANSLATED_SUFFIX)) &&
                    !(name.startsWith(AI_PREFIX) && name.endsWith(AI_ORIGINAL_SUFFIX)) &&
                    // heuristic for a start:
                    PATTERH_HAS_WHITESPACE.matcher(stringValue).find() &&
                    PATTERN_HAS_WORD.matcher(stringValue).find();
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

    // FIXME(hps,20.02.24) remove this - testing only.
    private String reverseString(String value) {
        return new StringBuilder(value).reverse().toString();
    }

}
