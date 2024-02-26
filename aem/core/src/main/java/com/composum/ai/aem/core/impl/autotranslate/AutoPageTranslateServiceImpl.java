package com.composum.ai.aem.core.impl.autotranslate;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.SelectorUtils;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
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
     * Saves the date when a resource was automatically translated.
     * Find translated resources with /content//*[@ai_translated] .
     */
    public static final String PROPERTY_AI_TRANSLATED_DATE = "ai_translated";

    /**
     * Saves user who triggered the automatic translation of the resource.
     */
    public static final String PROPERTY_AI_TRANSLATED_BY = "ai_translatedBy";

    /**
     * Prefix for property names of saved values.
     */
    public static final String AI_PREFIX = "ai_";

    /**
     * Prefix for property names changed to language copies
     */
    public static final String LC_PREFIX = "lc_";

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

    /**
     * List of properties that should always be translated.
     */
    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title", "alt", "cq:panelTitle", "shortDescription",
                    "actionText", "accessibilityLabel", "pretitle", "displayPopupTitle", "helpMessage",
                    "dc:title", "dc:description");

    @Reference
    private GPTTranslationService translationService;

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public Stats translateLiveCopy(Resource resource, GPTConfiguration configuration) throws WCMException, PersistenceException {
        Stats stats = new Stats();
        resource.getResourceResolver().refresh();
        List<PropertyToTranslate> propertiesToTranslate = new ArrayList<>();
        boolean changed = false;
        collectPropertiesToTranslate(resource, propertiesToTranslate, stats);

        LOG.info("Set of property names to translate in {} : {}", resource.getPath(),
                propertiesToTranslate.stream()
                        .map(propertyToTranslate -> propertyToTranslate.propertyName).collect(Collectors.toSet()));
        LOG.info("Translating {} properties in {}", propertiesToTranslate.size(), resource.getPath());

        List<String> valuesToTranslate = propertiesToTranslate.stream()
                .map(p -> p.resource.getValueMap().get(p.propertyName, String.class))
                .collect(Collectors.toList());
        String language = SelectorUtils.findLanguage(resource);
        String languageName = SelectorUtils.getLanguageName(language);
        List<String> translatedValues =
                translationService.fragmentedTranslation(valuesToTranslate, languageName, configuration);
        translationService.fragmentedTranslation(valuesToTranslate, languageName, configuration);

        for (int i = 0; i < propertiesToTranslate.size(); i++) {
            PropertyToTranslate propertyToTranslate = propertiesToTranslate.get(i);
            String originalValue = valuesToTranslate.get(i);
            String translatedValue = translatedValues.get(i);
//            if (StringUtils.equals(StringUtils.trim(originalValue), StringUtils.trim(translatedValue))) {
//                LOG.trace("Translation of {} in {} is the same as the original, not setting it.",
//                        propertyToTranslate.propertyName, propertyToTranslate.resource.getPath());
//                continue; // not quite sure whether that's right - that could lead to multiple user alerts
//            }
            String propertyName = propertyToTranslate.propertyName;
            Resource resourceToTranslate = propertyToTranslate.resource;
            LOG.trace("Setting {} in {} to {}", propertyName, propertyToTranslate.resource.getPath(), translatedValue);
            ModifiableValueMap valueMap = requireNonNull(resourceToTranslate.adaptTo(ModifiableValueMap.class));
            valueMap.put(encodePropertyName(AI_PREFIX, propertyName, AI_ORIGINAL_SUFFIX), originalValue);
            valueMap.put(encodePropertyName(AI_PREFIX, propertyName, AI_TRANSLATED_SUFFIX), translatedValue);
            valueMap.put(propertyName, translatedValue);
            markAsAiTranslated(resourceToTranslate);
            stats.translatedProperties++;
            changed = true;

            cancelInheritance(resource, resourceToTranslate, propertyToTranslate);
        }

        changed |= migratePathsToLanguageCopy(resource, language, stats);
        if (changed) {
            markAsAiTranslated(resource);
        }
        resource.getResourceResolver().commit();
        return stats;
    }

    protected static void markAsAiTranslated(Resource resource) {
        ModifiableValueMap valueMap = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        valueMap.put(PROPERTY_AI_TRANSLATED_BY, true);
        valueMap.put(PROPERTY_AI_TRANSLATED_DATE, Calendar.getInstance());
    }

    /**
     * Traverses the resource tree looking for paths pointing to /content/dam/ and /content/experience-fragments/ and
     * changes them if there is an unique language copy in our language.
     *
     * @return true if something was changed.
     */
    protected boolean migratePathsToLanguageCopy(Resource resource, String language, Stats stats) {
        boolean changed = false;
        for (Resource child : resource.getChildren()) {
            changed |= migratePathsToLanguageCopy(child, language, stats);
        }
        ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        if (mvm != null) {
            Map<String, Object> newEntries = new java.util.HashMap<>(); // avoid concurrency problems with the iterator
            for (Map.Entry<String, Object> entry : mvm.entrySet()) {
                String key = entry.getKey();
                if (key.contains(":") || isAiTranslateProperty(key)) {
                    continue; // don't touch system stuff with : - usually the relevant paths are in fileReference or similar.
                }
                if (entry.getValue() instanceof String) {
                    String value = (String) entry.getValue();
                    if (value.startsWith("/content/dam/") || value.startsWith("/content/experience-fragments/")) {
                        stats.paths++;
                        Resource referencedResource = resource.getResourceResolver().getResource(value);
                        List<Resource> languageSiblings = SelectorUtils.getLanguageSiblings(referencedResource, language);
                        if (languageSiblings.size() == 1) {
                            stats.relocatedPaths++;
                            Resource languageCopy = languageSiblings.get(0);
                            newEntries.put(key, languageCopy.getPath());
                            String origKey = encodePropertyName(LC_PREFIX, key, AI_ORIGINAL_SUFFIX);
                            if (mvm.get(origKey) == null) {
                                newEntries.put(origKey, value);
                            }
                            newEntries.put(encodePropertyName(LC_PREFIX, key, AI_TRANSLATED_SUFFIX), languageCopy.getPath());
                            changed = true;
                        } else if (languageSiblings.size() > 1) {
                            LOG.warn("More than one language copy for {} in {} - {}", key, resource.getPath(),
                                    languageSiblings.stream().map(Resource::getPath).collect(Collectors.toList()));
                        }
                    }
                }
            }
            mvm.putAll(newEntries);
        }
        return changed;
    }

    protected void cancelInheritance(Resource resource, Resource resourceToTranslate, PropertyToTranslate propertyToTranslate) throws WCMException {
        try {
            LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resourceToTranslate, false);
            if (relationship == null) {
                // a bit doubtful, but this way everything is revertable.
                throw new IllegalArgumentException("No live relationship for translated path " + resourceToTranslate.getPath());
            }
            if (resourceToTranslate.getName().equals(JcrConstants.JCR_CONTENT) ||
                    resourceToTranslate.getName().equals("metadata")) { // metadata for assets
                // here we have to cancel the properties individually.
                // cancelling the relationship for the whole resource seems to lead to trouble with the editor.
                liveRelationshipManager.cancelPropertyRelationship(
                        resource.getResourceResolver(), relationship, new String[]{propertyToTranslate.propertyName}, false);
            } else if (!relationship.getStatus().isCancelled() && !relationship.getStatus().isCancelledForChildren()) {
                // experimentally, AEM cancels the relationship with "deep" in the UI when there are no child nodes.
                boolean deep = !resource.getChildren().iterator().hasNext();
                liveRelationshipManager.cancelRelationship(resource.getResourceResolver(), relationship,
                        deep, false);
            }
        } catch (WCMException | RuntimeException e) {
            LOG.error("Error cancelling inheritance for {} property {} : {}",
                    resourceToTranslate.getPath(), propertyToTranslate.propertyName, e.toString());
            throw e;
        }
    }

    /**
     * Inverse to {@link #cancelInheritance(Resource, Resource, PropertyToTranslate)}.
     */
    protected void reenableInheritance(Resource resource, String key, LiveRelationship relationship) throws WCMException {
        if (relationship == null) {
            LOG.warn("No live relationship for translated key {} path {}", key, resource.getPath());
            return;
        }
        try {
            if (resource.getName().equals(JcrConstants.JCR_CONTENT) || resource.getName().equals("metadata")) {
                liveRelationshipManager.reenablePropertyRelationship(resource.getResourceResolver(), relationship, new String[]{key}, false);
            } else {
                liveRelationshipManager.reenableRelationship(resource.getResourceResolver(), relationship, false);
            }
        } catch (IllegalStateException e) {
            if (!e.toString().contains("is not cancelled")) {
                LOG.error("Error reenabling inheritance for {} property {} : {}",
                        resource.getPath(), key, e.toString());
                throw e;
            }
            // else ignore - was already done for another property.
        } catch (WCMException | RuntimeException e) {
            LOG.error("Error reenabling inheritance for {} property {} : {}",
                    resource.getPath(), key, e.toString());
            throw e;
        }
    }

    @Override
    public void rollback(Resource resource) throws WCMException {
        for (Resource child : resource.getChildren()) {
            rollback(child);
        }
        ModifiableValueMap mvm = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        for (String key : new ArrayList<>(mvm.keySet())) {
            String originalKey = encodePropertyName(AI_PREFIX, key, AI_ORIGINAL_SUFFIX);
            String translatedKey = encodePropertyName(AI_PREFIX, key, AI_TRANSLATED_SUFFIX);
            String originalPathKey = encodePropertyName(LC_PREFIX, key, AI_ORIGINAL_SUFFIX);
            String translatedPathKey = encodePropertyName(LC_PREFIX, key, AI_TRANSLATED_SUFFIX);
            if (mvm.containsKey(originalKey)) {
                mvm.put(key, mvm.get(originalKey));
                mvm.remove(originalKey);
                mvm.remove(translatedKey);
                reenableInheritance(resource, key, relationship);
            }
            if (mvm.containsKey(originalPathKey)) {
                mvm.put(key, mvm.get(originalPathKey));
                mvm.remove(originalPathKey);
                mvm.remove(translatedPathKey);
                reenableInheritance(resource, key, relationship);
            }
        }
        mvm.remove(PROPERTY_AI_TRANSLATED_BY);
        mvm.remove(PROPERTY_AI_TRANSLATED_DATE);
    }

    /**
     * Searches for properties we have to translate.
     */
    protected void collectPropertiesToTranslate(Resource resource, List<PropertyToTranslate> propertiesToTranslate, Stats stats) {
        ValueMap valueMap = resource.getValueMap();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            if (isTranslatableProperty(entry.getKey(), entry.getValue())) {
                stats.translateableProperties++;
                if (!valueMap.containsKey(encodePropertyName(AI_PREFIX, entry.getKey(), AI_ORIGINAL_SUFFIX))) {
                    PropertyToTranslate propertyToTranslate = new PropertyToTranslate();
                    propertyToTranslate.resource = resource;
                    propertyToTranslate.propertyName = entry.getKey();
                    propertiesToTranslate.add(propertyToTranslate);
                }
            }
        }
        resource.getChildren().forEach(child -> collectPropertiesToTranslate(child, propertiesToTranslate, stats));
    }

    /**
     * Searches for properties
     */

    protected String encodePropertyName(String prefix, String propertyName, String suffix) {
        return prefix + propertyName.replace(":", "_") + suffix;
    }

    protected static final Pattern PATTERN_HAS_WHITESPACE = Pattern.compile("\\s");

    /**
     * As additional heuristic - the text should have at least one word with >= 4 letters.
     * That will break down very different languages, I know, but this is a POC. :-)
     */
    protected static final Pattern PATTERN_HAS_WORD = Pattern.compile("\\p{L}{4}");

    protected static final Pattern PATTERN_HAS_LETTER = Pattern.compile("\\p{L}");

    /**
     * Checks whether the property is one of jcr:title, jcr:description, title, alt, cq:panelTitle, shortDescription,
     * actionText, accessibilityLabel, pretitle, displayPopupTitle, helpMessage , or alternatively don't have a colon
     * in the name, have a String value, don't start with /{content,apps,libs,mnt}/ in the value and the value has
     * a whitespace and at least one 4 letter sequence.
     */
    protected static boolean isTranslatableProperty(String name, Object value) {
        if (CERTAINLY_TRANSLATABLE_PROPERTIES.contains(name) &&
                (value instanceof String) &&
                PATTERN_HAS_LETTER.matcher((String) value).find()) {
            return true;
        }
        if (name.contains(":")) {
            return false;
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.startsWith("/content/") || stringValue.startsWith("/apps/") ||
                    stringValue.startsWith("/libs/") || stringValue.startsWith("/mnt/")) {
                return false; // looks like path
            }
            if (isAiTranslateProperty(name)) {
                return false;
            }
            return PATTERN_HAS_WHITESPACE.matcher(stringValue).find() &&
                    PATTERN_HAS_WORD.matcher(stringValue).find();
        }
        return false;
    }

    /**
     * Checks whether a property was created by us and must not be translated etc.
     */
    protected static boolean isAiTranslateProperty(String name) {
        return name.startsWith(AI_PREFIX) || name.startsWith(LC_PREFIX);
    }


    protected static class PropertyToTranslate {
        private Resource resource;
        private String propertyName;

        @Override
        public String toString() {
            return resource.getPath() + "/" + propertyName;
        }
    }

}
