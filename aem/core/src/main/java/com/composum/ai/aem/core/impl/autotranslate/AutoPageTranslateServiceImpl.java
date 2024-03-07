package com.composum.ai.aem.core.impl.autotranslate;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        boolean changed = collectPropertiesToTranslate(resource, propertiesToTranslate, stats, translationParameters);

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

        for (int i = 0; i < propertiesToTranslate.size(); i++) {
            PropertyToTranslate propertyToTranslate = propertiesToTranslate.get(i);
            String originalValue = valuesToTranslate.get(i);
            String translatedValue = translatedValues.get(i);
            String propertyName = propertyToTranslate.propertyName;
            Resource resourceToTranslate = propertyToTranslate.targetResource;
            LOG.trace("Setting {} in {} to {}", propertyName, propertyToTranslate.targetResource.getPath(), translatedValue);
            ModifiableValueMap valueMap = requireNonNull(resourceToTranslate.adaptTo(ModifiableValueMap.class));
            AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(propertyToTranslate.sourceResource.getValueMap(), valueMap, propertyName);
            targetWrapper.setOriginalCopy(originalValue);
            targetWrapper.setTranslatedCopy(translatedValue);
            targetWrapper.setCurrentValue(translatedValue);

            LiveRelationship liveRelationship = relationships.get(propertyToTranslate.targetResource.getPath());
            if (liveRelationship == null) {
                liveRelationship = liveRelationshipManager.getLiveRelationship(propertyToTranslate.targetResource, false);
                relationships.put(propertyToTranslate.targetResource.getPath(), liveRelationship);
            }

            liveRelationshipManager.cancelPropertyRelationship(propertyToTranslate.targetResource.getResourceResolver(),
                    liveRelationship, targetWrapper.allAiKeys(), false);

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
        AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(null, valueMap, "dummy");
        targetWrapper.setAiTranslatedBy(resource.getResourceResolver().getUserID());
        targetWrapper.setAiTranslatedDate(Calendar.getInstance());
        if (liveRelationship != null) {
            liveRelationshipManager.cancelPropertyRelationship(resource.getResourceResolver(),
                    liveRelationship, targetWrapper.allGeneralKeys(), false);
        }
    }

    /**
     * Traverses the resource tree looking for paths pointing to /content/dam/ and /content/experience-fragments/ and
     * changes them if there is an unique language copy in our language.
     *
     * @return true if something was changed.
     */
    protected boolean migratePathsToLanguageCopy(Resource resource, String language, Stats stats) throws WCMException {
        boolean changed = false;
        for (Resource child : resource.getChildren()) {
            changed |= migratePathsToLanguageCopy(child, language, stats);
        }
        ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        if (mvm != null) {
            for (Map.Entry<String, Object> entry : new HashMap<String, Object>(mvm).entrySet()) {
                String key = entry.getKey();
                if (key.contains(":") || AITranslatePropertyWrapper.isAiTranslateProperty(key)) {
                    continue; // don't touch system stuff with : - usually the relevant paths are in fileReference or similar.
                }
                if (entry.getValue() instanceof String) {
                    AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(null, mvm, key);
                    String value = (String) entry.getValue();
                    if (value.startsWith("/content/dam/") || value.startsWith("/content/experience-fragments/")) {
                        stats.paths++;
                        Resource referencedResource = resource.getResourceResolver().getResource(value);
                        List<Resource> languageSiblings = SelectorUtils.getLanguageSiblings(referencedResource, language);
                        if (languageSiblings.size() == 1) {
                            stats.relocatedPaths++;
                            Resource languageCopy = languageSiblings.get(0);
                            targetWrapper.setCurrentValue(languageCopy.getPath());
                            if (targetWrapper.getLcOriginal() == null) {
                                targetWrapper.setLcOriginal(value);
                            }
                            targetWrapper.setLcTranslated(languageCopy.getPath());
                            LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(resource, false);
                            liveRelationshipManager.cancelPropertyRelationship(resource.getResourceResolver(),
                                    liveRelationship, targetWrapper.allLcKeys(), false);
                            changed = true;
                        } else if (languageSiblings.size() > 1) {
                            LOG.warn("More than one language copy for {} in {} - {}", key, resource.getPath(),
                                    languageSiblings.stream().map(Resource::getPath).collect(Collectors.toList()));
                        }
                    }
                }
            }
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
        Set<String> resetPropertyExclusionKeys = new HashSet<>();
        for (String key : new ArrayList<>(mvm.keySet())) {
            if (AITranslatePropertyWrapper.isAiTranslateProperty(key)) {
                continue; // will be removed when working on the original key.
            }

            AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(null, mvm, key);
            if (targetWrapper.hasSavedTranslation()) {
                targetWrapper.setCurrentValue(targetWrapper.getOriginalCopy());
                reenableInheritance(resource, key, relationship);
            }
            if (isNotBlank(targetWrapper.getLcOriginal())) {
                targetWrapper.setCurrentValue(targetWrapper.getLcOriginal());
                reenableInheritance(resource, key, relationship);
            }
            String[] allKeys = targetWrapper.allKeys();
            Arrays.stream(allKeys).forEach(mvm::remove);
            resetPropertyExclusionKeys.addAll(Arrays.asList(allKeys));
            targetWrapper.setAiTranslatedBy(null);
            targetWrapper.setAiTranslatedDate(null);
            resetPropertyExclusionKeys.addAll(Arrays.asList(targetWrapper.allGeneralKeys()));
        }
        if (relationship != null) {
            liveRelationshipManager.reenablePropertyRelationship(resource.getResourceResolver(), relationship,
                    resetPropertyExclusionKeys.toArray(new String[0]), false);
        }
    }

    /**
     * Searches for properties we have to translate.
     *
     * @return true if something was changed already
     */
    protected boolean collectPropertiesToTranslate(
            @Nonnull Resource resource, @Nonnull List<PropertyToTranslate> propertiesToTranslate, @Nonnull Stats stats,
            @Nonnull AutoTranslateService.TranslationParameters translationParameters) throws WCMException {
        boolean changed = false;
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship == null) {
            LOG.warn("No live relationship for {}", resource.getPath());
            return false;
        }
        String sourcePath = relationship.getSourcePath();
        Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
        if (sourceResource != null) {
            ValueMap sourceValueMap = sourceResource.getValueMap();
            ModifiableValueMap targetValueMap = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
            for (Map.Entry<String, Object> entry : sourceValueMap.entrySet()) {
                if (isTranslatableProperty(entry.getKey(), entry.getValue())) {
                    stats.translateableProperties++;
                    AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(sourceValueMap, targetValueMap, entry.getKey());
                    boolean isCancelled = relationship.getStatus() != null && (
                            relationship.getStatus().isCancelled() ||
                                    relationship.getStatus().getCanceledProperties().contains(entry.getKey())
                    );

                    // we will translate except if the property is cancelled and we don't want to touch cancelled properties,
                    // or if we have a current translation.

                    if (isCancelled && !translationParameters.translateWhenChanged) {
                        continue; // don't touch cancelled properties
                    }

                    if (targetWrapper.isOriginalAsWhenLastTranslating()) {
                        // shortcut: we have a recent translation already
                        targetWrapper.setCurrentValue(targetWrapper.getTranslatedCopy());
                        changed = changed || !StringUtils.equals(targetWrapper.getTranslatedCopy(), targetWrapper.getOriginalCopy());
                        continue;
                    }

                    if (isCancelled && targetWrapper.hasSavedTranslation()
                            && !StringUtils.equals(targetWrapper.getTranslatedCopy(), targetWrapper.getCurrentValue())
                            && !StringUtils.equals(targetWrapper.getOriginal(), targetWrapper.getCurrentValue())) {
                        // = translateWhenChanged override; save manual change. We also exclude the phase during rollout
                        // where the property is reset to the original value and we have to restore the translation.
                        LOG.info("Re-translating {} in {} despite manual change", entry.getKey(), resource.getPath());
                        targetWrapper.saveManualChange();
                        stats.modifiedButRetranslatedProperties++;
                    }

                    if (targetWrapper.hasSavedTranslation()) {
                        stats.retranslatedProperties++;
                    }

                    LOG.debug("Re-translating {} in {}", entry.getKey(), resource.getPath());
                    PropertyToTranslate propertyToTranslate = new PropertyToTranslate();
                    propertyToTranslate.sourceResource = sourceResource;
                    propertyToTranslate.targetResource = resource;
                    propertyToTranslate.propertyName = entry.getKey();
                    propertiesToTranslate.add(propertyToTranslate);
                }
            }
        } else {
            LOG.info("No source resource found - translation not touching {}", resource.getPath());
            // that can happen for new resources in the live copy. Unfortunately it's not really clear if we should
            // try to translate that or not - we'll probably learn about that in practice.
        }
        for (Resource child : resource.getChildren()) {
            if (!PATTERN_IGNORED_SUBNODE_NAMES.matcher(child.getName()).matches()) {
                boolean childChanged = collectPropertiesToTranslate(child, propertiesToTranslate, stats, translationParameters);
                changed |= childChanged;
            }
        }
        return changed;
    }

    /**
     * Searches for properties
     */

    protected static String encodePropertyName(String prefix, String propertyName, String suffix) {
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

            if (AITranslatePropertyWrapper.isAiTranslateProperty(name)) {
                return false;
            }
            return PATTERN_HAS_WHITESPACE.matcher(stringValue).find() &&
                    PATTERN_HAS_WORD.matcher(stringValue).find();
        }
        return false;
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
