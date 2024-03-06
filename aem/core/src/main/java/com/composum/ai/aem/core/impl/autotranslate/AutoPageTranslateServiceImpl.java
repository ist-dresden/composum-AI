package com.composum.ai.aem.core.impl.autotranslate;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
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

    protected static final Logger LOG = LoggerFactory.getLogger(AutoPageTranslateServiceImpl.class);

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
     * Suffix for a property name where a manual change is saved when a retranslation is done despite a manual modification.
     * Will be overwritten if another retranslation is done.
     */
    public static final String AI_MANUAL_CHANGE_SUFFIX = "_manualChange";


    /**
     * List of properties that should always be translated.
     */
    public static final List<String> CERTAINLY_TRANSLATABLE_PROPERTIES =
            Arrays.asList("jcr:title", "jcr:description", "text", "title", "alt", "cq:panelTitle", "shortDescription",
                    "actionText", "accessibilityLabel", "pretitle", "helpMessage",
                    "dc:title", "dc:description");

    protected static final Pattern PATTERN_IGNORED_SUBNODE_NAMES =
            Pattern.compile("i18n|rep:.*|cq:.*|xmpMM:.*|exif:.*|crs:.*|Iptc4xmpCore:.*|sling:members");

    @Reference
    protected GPTTranslationService translationService;

    @Reference
    protected LiveRelationshipManager liveRelationshipManager;

    @Override
    public Stats translateLiveCopy(@Nonnull Resource resource, @Nullable GPTConfiguration configuration,
                                   @Nonnull AutoTranslateService.TranslationParameters translationParameters)
            throws WCMException, PersistenceException {
        LOG.debug(">>> translateLiveCopy: {}", resource.getPath());
        Stats stats = new Stats();
        List<PropertyToTranslate> propertiesToTranslate = new ArrayList<>();
        boolean changed = false;
        collectPropertiesToTranslate(resource, propertiesToTranslate, stats, translationParameters);

        LOG.debug("Set of property names to translate in {} : {}", resource.getPath(),
                propertiesToTranslate.stream()
                        .map(propertyToTranslate -> propertyToTranslate.propertyName).collect(Collectors.toSet()));
        LOG.info("Translating {} properties in {}", propertiesToTranslate.size(), resource.getPath());

        List<String> valuesToTranslate = propertiesToTranslate.stream()
                .map(p -> p.sourceResource.getValueMap().get(p.propertyName, String.class))
                .collect(Collectors.toList());
        String language = SelectorUtils.findLanguage(resource);
        if (language == null) {
            throw new IllegalArgumentException("No language found for " + resource.getPath());
        }
        String languageName = SelectorUtils.getLanguageName(language);
        List<String> translatedValues =
                translationService.fragmentedTranslation(valuesToTranslate, languageName, configuration);
        translationService.fragmentedTranslation(valuesToTranslate, languageName, configuration);

        Map<String, LiveRelationship> relationships = new HashMap<>();
        for (PropertyToTranslate propertyToTranslate : propertiesToTranslate) {
            relationships.put(propertyToTranslate.targetResource.getPath(),
                    liveRelationshipManager.getLiveRelationship(propertyToTranslate.targetResource, false));
        }

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
            Resource resourceToTranslate = propertyToTranslate.targetResource;
            LOG.trace("Setting {} in {} to {}", propertyName, propertyToTranslate.targetResource.getPath(), translatedValue);
            ModifiableValueMap valueMap = requireNonNull(resourceToTranslate.adaptTo(ModifiableValueMap.class));
            String originalKey = encodePropertyName(AI_PREFIX, propertyName, AI_ORIGINAL_SUFFIX);
            valueMap.put(originalKey, originalValue);
            String translatedKey = encodePropertyName(AI_PREFIX, propertyName, AI_TRANSLATED_SUFFIX);
            valueMap.put(translatedKey, translatedValue);
            valueMap.put(propertyName, translatedValue);
            LiveRelationship liveRelationship = relationships.get(propertyToTranslate.targetResource.getPath());
            liveRelationshipManager.cancelPropertyRelationship(propertyToTranslate.targetResource.getResourceResolver(),
                    liveRelationship, new String[]{originalKey, translatedKey}, false);

            markAsAiTranslated(resourceToTranslate, liveRelationship);
            stats.translatedProperties++;
            changed = true;

            if (translationParameters.breakInheritance) {
                cancelInheritance(resource, resourceToTranslate, propertyToTranslate);
            }
        }

        changed |= migratePathsToLanguageCopy(resource, language, stats);
        if (changed) {
            markAsAiTranslated(resource, liveRelationshipManager.getLiveRelationship(resource, false));
        }
        if (translationParameters.autoSave) {
            resource.getResourceResolver().commit();
        }
        LOG.debug("<<< translateLiveCopy: {} {}", resource.getPath(), stats);
        return stats;
    }

    protected void markAsAiTranslated(Resource resource, LiveRelationship liveRelationship) throws WCMException {
        ModifiableValueMap valueMap = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        valueMap.put(PROPERTY_AI_TRANSLATED_BY, true);
        valueMap.put(PROPERTY_AI_TRANSLATED_DATE, Calendar.getInstance());
        if (liveRelationship != null) {
            liveRelationshipManager.cancelPropertyRelationship(resource.getResourceResolver(),
                    liveRelationship, new String[]{PROPERTY_AI_TRANSLATED_BY, PROPERTY_AI_TRANSLATED_DATE}, false);
        }
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
        List<String> keysToKeep = new ArrayList<>();
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
                keysToKeep.add(originalKey);
                keysToKeep.add(translatedKey);
            }
            if (mvm.containsKey(originalPathKey)) {
                mvm.put(key, mvm.get(originalPathKey));
                mvm.remove(originalPathKey);
                mvm.remove(translatedPathKey);
                reenableInheritance(resource, key, relationship);
                keysToKeep.add(originalPathKey);
                keysToKeep.add(translatedPathKey);
            }
        }
        mvm.remove(PROPERTY_AI_TRANSLATED_BY);
        mvm.remove(PROPERTY_AI_TRANSLATED_DATE);
        keysToKeep.add(PROPERTY_AI_TRANSLATED_BY);
        keysToKeep.add(PROPERTY_AI_TRANSLATED_DATE);
        if (relationship != null) {
            liveRelationshipManager.reenablePropertyRelationship(resource.getResourceResolver(), relationship, keysToKeep.toArray(new String[0]), false);
        }
    }

    /**
     * Searches for properties we have to translate.
     */
    protected void collectPropertiesToTranslate(
            @Nonnull Resource resource, @Nonnull List<PropertyToTranslate> propertiesToTranslate, @Nonnull Stats stats,
            @Nonnull AutoTranslateService.TranslationParameters translationParameters) throws WCMException {
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship == null) {
            LOG.warn("No live relationship for {}", resource.getPath());
            return;
        }
        String sourcePath = relationship.getSourcePath();
        Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
        if (sourceResource != null) {
            ValueMap sourceValueMap = sourceResource.getValueMap();
            ValueMap targetValueMap = resource.getValueMap();
            for (Map.Entry<String, Object> entry : sourceValueMap.entrySet()) {
                if (isTranslatableProperty(entry.getKey(), entry.getValue())) {
                    stats.translateableProperties++;
                    String originallyTranslatedValue = targetValueMap.get(encodePropertyName(AI_PREFIX, entry.getKey(), AI_ORIGINAL_SUFFIX), String.class);
                    boolean alreadyTranslated = originallyTranslatedValue != null;
                    boolean addProperty = !alreadyTranslated;
                    if (alreadyTranslated && translationParameters.translateWhenChanged) {
                        addProperty = !StringUtils.equals(originallyTranslatedValue, sourceValueMap.get(entry.getKey(), String.class));
                        if (addProperty) {
                            LOG.debug("Re-translating because of change: {} in {}", entry.getKey(), resource.getPath());
                        }
                    }
                    if (addProperty) {
                        PropertyToTranslate propertyToTranslate = new PropertyToTranslate();
                        propertyToTranslate.sourceResource = sourceResource;
                        propertyToTranslate.targetResource = resource;
                        propertyToTranslate.propertyName = entry.getKey();
                        propertiesToTranslate.add(propertyToTranslate);
                    }
                }
            }
        } else {
            LOG.info("No source resource found - translation not touching {}", resource.getPath());
            // that can happen for new resources in the live copy. Unfortunately it's not really clear if we should
            // try to translate that or not - we'll probably learn about that in practice.
        }
        for (Resource child : resource.getChildren()) {
            if (!PATTERN_IGNORED_SUBNODE_NAMES.matcher(child.getName()).matches()) {
                collectPropertiesToTranslate(child, propertiesToTranslate, stats, translationParameters);
            }
        }
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
        if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.startsWith("/content/") || stringValue.startsWith("/apps/") ||
                    stringValue.startsWith("/libs/") || stringValue.startsWith("/mnt/") ||
                    stringValue.equals("true") || stringValue.equals("false")) {
                return false; // looks like path or boolean
            }

            if (CERTAINLY_TRANSLATABLE_PROPERTIES.contains(name) &&
                    PATTERN_HAS_LETTER.matcher(stringValue).find()
            ) {
                return true;
            }
            if (name.contains(":")) {
                return false;
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
        /**
         * The resource where we take the translation source from. Can be the {@link #targetResource} but
         * also the source of a live copy.
         */
        protected Resource sourceResource;
        /**
         * The resource where we write the translation.
         */
        protected Resource targetResource;
        protected String propertyName;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(propertyName).append(" in ");
            sb.append(targetResource != null ? targetResource.getPath() : "null");
            if (sourceResource != null && targetResource != null && !sourceResource.getPath().equals(targetResource.getPath())) {
                sb.append(" (from ");
                sb.append(sourceResource.getPath());
                sb.append(")");
            }
            return sb.toString();
        }
    }

}
