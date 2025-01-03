package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_TRANSLATION_ERRORMARKER;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.LASTID;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_END;
import static com.composum.ai.backend.base.service.chat.impl.GPTTranslationServiceImpl.MULTITRANSLATION_SEPARATOR_START;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.SelectorUtils;
import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTResponseCheck;
import com.composum.ai.backend.base.service.chat.GPTTranslationService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
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

    protected static final Pattern PATTERN_IGNORED_SUBNODE_NAMES =
            Pattern.compile("i18n|rep:.*|cq:.*|xmpMM:.*|exif:.*|crs:.*|Iptc4xmpCore:.*|sling:members");

    public static final String MARKER_DEBUG_ADDITIONAL_INSTRUCTIONS = "DEBUGADDINSTRUCTIONS";

    protected final String DEFAULT_TRANSLATION_RULE_PATTERN = "Translate '{0}' as '{1}'.";

    @Reference
    protected GPTTranslationService translationService;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile AutoTranslateConfigService autoTranslateConfigService;

    @Reference
    protected AIConfigurationService configurationService;

    @Reference
    protected LiveRelationshipManager liveRelationshipManager;

    protected final Map<String, LocalDateTime> runningTranslations = new ConcurrentHashMap<>();

    @Override
    public Map<String, LocalDateTime> getRunningTranslations() {
        return Collections.unmodifiableMap(new HashMap<>(runningTranslations));
    }

    @Override
    public Stats translateLiveCopy(@Nonnull Resource resource,
                                   @Nonnull AutoTranslateService.TranslationParameters translationParameters)
            throws WCMException, PersistenceException {
        LOG.debug(">>> translateLiveCopy: {}", resource.getPath());
        Resource child = resource.getChild("jcr:content");
        if (child != null) {
            resource = child;
        }
        Stats stats = new Stats();
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship == null) {
            throw new IllegalArgumentException("No live relationship for " + resource.getPath());
        }

        try {
            runningTranslations.put(resource.getPath(), LocalDateTime.now());

            String language = SelectorUtils.findLanguage(resource);
            if (language == null) {
                throw new IllegalArgumentException("No language found for " + resource.getPath());
            }
            String languageName = SelectorUtils.getLanguageName(language, Locale.ENGLISH);
            String sourceLanguage = determineSourceLanguage(resource);
            if (sourceLanguage == null || StringUtils.equals(language, sourceLanguage)) {
                LOG.info("Skipping translation because language and source language are {} for {}", language, resource.getPath());
                return stats;
            }

            ConfigurationBuilder confBuilder = Objects.requireNonNull(resource.adaptTo(ConfigurationBuilder.class));
            AutoTranslateCaConfig autoTranslateCaConfig = confBuilder.as(AutoTranslateCaConfig.class);
            GPTConfiguration configuration = determineConfiguration(resource, autoTranslateCaConfig, translationParameters, stats);
            String additionalInstructions = configuration.getAdditionalInstructions();

            String previousAdditionalInstructions = resource.getValueMap().get(AITranslatePropertyWrapper.PROPERTY_AI_ADDINSTRUCTIONS, String.class);
            boolean additionalInstructionsChanged = !StringUtils.equals(additionalInstructions, previousAdditionalInstructions);
            stats.collectedAdditionalInstructions = additionalInstructions;
            if (additionalInstructionsChanged) {
                LOG.info("Retranslating because additional instructions changed for {} : {}", resource.getPath(), additionalInstructions);
            }

            List<PropertyToTranslate> propertiesToTranslate = new ArrayList<>();
            boolean changed = collectPropertiesToTranslate(resource, propertiesToTranslate, stats, translationParameters, additionalInstructionsChanged);

            int countPropertiesToTranslate = propertiesToTranslate.stream()
                    .filter(propertyToTranslate -> !propertyToTranslate.isAlreadyCorrectlyTranslated)
                    .collect(Collectors.counting()).intValue();
            if (countPropertiesToTranslate <= 0) {
                LOG.debug("Nothing to translate in {}", resource.getPath());
            } else {
                LOG.debug("Set of property names to newly translate in {} : {}", resource.getPath(),
                        propertiesToTranslate.stream()
                                .filter(propertyToTranslate -> !propertyToTranslate.isAlreadyCorrectlyTranslated)
                                .map(propertyToTranslate -> propertyToTranslate.propertyName).collect(Collectors.toSet()));
                LOG.info("Translating {} properties in {} using additional instructions", countPropertiesToTranslate, resource.getPath(), additionalInstructions);
                if (StringUtils.contains(additionalInstructions, MARKER_DEBUG_ADDITIONAL_INSTRUCTIONS)) {
                    throw new GPTException.GPTUserNotificationException(
                            "As requested: the additional instructions for " + resource.getPath() + " are as follows (translation is aborted):\n\n" +
                                    additionalInstructions.replaceAll(MARKER_DEBUG_ADDITIONAL_INSTRUCTIONS, "").trim());
                }

                configuration = maybeIncludeAlreadyTranslatedTextAsExample(propertiesToTranslate, autoTranslateCaConfig, configuration);

                propertiesToTranslate = reducePropertiesToTranslate(propertiesToTranslate, autoTranslateCaConfig);
                List<String> valuesToTranslate = propertiesToTranslate.stream()
                        .map(PropertyToTranslate::getSourceValue)
                        .collect(Collectors.toList());

                List<String> translatedValues =
                        translationService.fragmentedTranslation(valuesToTranslate, languageName, configuration,
                                Collections.singletonList(GPTResponseCheck.KEEP_HREF_TRANSLATION_CHECK));
                translatedValues = remapPaths(translatedValues, relationship.getLiveCopy().getBlueprintPath(), relationship.getLiveCopy().getPath());

                Map<String, LiveRelationship> relationships = new HashMap<>();

                for (int i = 0; i < propertiesToTranslate.size(); i++) {
                    PropertyToTranslate propertyToTranslate = propertiesToTranslate.get(i);
                    if (propertyToTranslate.isAlreadyCorrectlyTranslated) {
                        continue; // was just included for context
                    }
                    String originalValue = valuesToTranslate.get(i);
                    String translatedValue = translatedValues.get(i);
                    String propertyName = propertyToTranslate.propertyName;
                    Resource resourceToTranslate = propertyToTranslate.targetResource;
                    LOG.trace("Setting {} in {} to {}", propertyName, propertyToTranslate.targetResource.getPath(), translatedValue);
                    ModifiableValueMap valueMap = requireNonNull(resourceToTranslate.adaptTo(ModifiableValueMap.class));
                    AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(propertyToTranslate.sourceResource.getValueMap(), valueMap, propertyName);
                    if (!propertyToTranslate.isCancelled) {
                        targetWrapper.setOriginalCopy(originalValue);
                        targetWrapper.setTranslatedCopy(translatedValue);
                        targetWrapper.setCurrentValue(translatedValue);
                        targetWrapper.setNewOriginalCopy(null);
                        targetWrapper.setNewTranslatedCopy(null);
                    } else if (!StringUtils.equals(targetWrapper.getOriginalCopy(), originalValue)) {
                        // is cancelled - we just save the data for merging
                        targetWrapper.setNewOriginalCopy(originalValue);
                        targetWrapper.setNewTranslatedCopy(translatedValue);
                    }

                    LiveRelationship liveRelationship = relationships.get(propertyToTranslate.targetResource.getPath());
                    if (liveRelationship == null) {
                        liveRelationship = liveRelationshipManager.getLiveRelationship(propertyToTranslate.targetResource, false);
                        relationships.put(propertyToTranslate.targetResource.getPath(), liveRelationship);
                    }

                    liveRelationshipManager.cancelPropertyRelationship(propertyToTranslate.targetResource.getResourceResolver(),
                            liveRelationship, targetWrapper.allAiKeys(), false);

                    markAsAiTranslated(resourceToTranslate, liveRelationship, translationParameters, configuration);
                    stats.translatedProperties++;
                    changed = true;
                }
            }

            if (additionalInstructionsChanged) {
                ModifiableValueMap mvm = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
                LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(resource, false);
                liveRelationshipManager.cancelPropertyRelationship(resource.getResourceResolver(),
                        liveRelationship, new String[]{AITranslatePropertyWrapper.PROPERTY_AI_ADDINSTRUCTIONS}, false);
                if (additionalInstructions == null) {
                    mvm.remove(AITranslatePropertyWrapper.PROPERTY_AI_ADDINSTRUCTIONS);
                } else {
                    mvm.put(AITranslatePropertyWrapper.PROPERTY_AI_ADDINSTRUCTIONS, additionalInstructions);
                }
                changed = true;
            }

            boolean pathsChanged = migratePathsToLanguageCopy(resource, language, stats);
            changed = pathsChanged || changed;
            if (changed) {
                markAsAiTranslated(resource, liveRelationshipManager.getLiveRelationship(resource, false), translationParameters, configuration);
            }
            if (translationParameters.autoSave) {
                resource.getResourceResolver().commit();
            }
            LOG.debug("<<< translateLiveCopy: {} {}", resource.getPath(), stats);
            return stats;
        } finally {
            runningTranslations.remove(resource.getPath());
        }
    }

    protected GPTConfiguration determineConfiguration(@Nonnull Resource resource, AutoTranslateCaConfig autoTranslateCaConfig, @Nonnull AutoTranslateService.TranslationParameters translationParameters, Stats stats) throws WCMException {
        GPTConfiguration configuration = configurationService.getGPTConfiguration(resource.getResourceResolver(), resource.getPath());

        if (autoTranslateCaConfig.temperature() != null && !autoTranslateCaConfig.temperature().trim().isEmpty()) {
            try {
                double temperature = Double.parseDouble(autoTranslateCaConfig.temperature());
                configuration = GPTConfiguration.ofTemperature(temperature).merge(configuration);
            } catch (NumberFormatException e) {
                LOG.error("Invalid temperature value {} for path {}", autoTranslateCaConfig.temperature(), resource.getPath());
            }
        }
        if (autoTranslateCaConfig.preferHighIntelligenceModel()) {
            configuration = GPTConfiguration.HIGH_INTELLIGENCE.merge(configuration);
        } else if (autoTranslateCaConfig.preferStandardModel()) {
            configuration = GPTConfiguration.STANDARD_INTELLIGENCE.merge(configuration);
        }

        String additionalInstructions = StringUtils.defaultIfBlank(translationParameters.additionalInstructions, "");
        if (StringUtils.isNotBlank(autoTranslateCaConfig.additionalInstructions())) {
            additionalInstructions = additionalInstructions + "\n\n" + autoTranslateCaConfig.additionalInstructions().trim();
        }
        List<AutoTranslateRuleConfig> allRules = new ArrayList<>();
        if (translationParameters.rules != null) {
            allRules.addAll(translationParameters.rules);
        }

        if (autoTranslateCaConfig.rules() != null) {
            allRules.addAll(Arrays.asList(autoTranslateCaConfig.rules()));
        }

        allRules.addAll(collectTranslationTables(autoTranslateCaConfig, resource));

        // filter translation rules that apply
        List<PropertyToTranslate> allTranslateableProperties = new ArrayList<>();
        collectPropertiesToTranslate(resource, allTranslateableProperties, stats, translationParameters, true);
        String translationRules = collectApplicableTranslationRules(resource.getPath(), allTranslateableProperties, allRules);
        if (translationRules != null) {
            additionalInstructions = additionalInstructions + "\n\n" + translationRules.trim();
        }
        additionalInstructions = StringUtils.trimToNull(additionalInstructions);
        return GPTConfiguration.ofAdditionalInstructions(additionalInstructions).merge(configuration);
    }

    /**
     * Collects the values we need to translate.
     * If configured, we also insert texts that are already translated since they might guide the translation process.
     */
    protected List<PropertyToTranslate> reducePropertiesToTranslate(List<PropertyToTranslate> propertiesToTranslate, AutoTranslateCaConfig autoTranslateCaConfig) {
        boolean includeFullPageInRetranslation = configurationOrOverride(
                autoTranslateConfigService.includeFullPageInRetranslation(),
                autoTranslateCaConfig.includeFullPageInRetranslation(),
                !propertiesToTranslate.isEmpty() ? propertiesToTranslate.get(0).targetResource.getPath() : null
        );
        boolean[] includeIndizes = new boolean[propertiesToTranslate.size()];
        for (int i = 0; i < propertiesToTranslate.size(); i++) {
            includeIndizes[i] = includeFullPageInRetranslation || !propertiesToTranslate.get(i).isAlreadyCorrectlyTranslated;
        }

        expandSelection(includeIndizes, 2);

        List<PropertyToTranslate> reducedProps = new ArrayList<>();
        for (int i = 0; i < propertiesToTranslate.size(); i++) {
            if (includeIndizes[i]) {
                reducedProps.add(propertiesToTranslate.get(i));
            }
        }
        return reducedProps;
    }

    /**
     * Also include 2 items before those already set, and 2 items after those already set, to have some context.
     */
    protected static void expandSelection(boolean[] includeIndizes, int selectRange) {
        int lastSetIndex = Integer.MIN_VALUE;
        for (int i = 0; i < includeIndizes.length; i++) {
            if (includeIndizes[i]) {
                lastSetIndex = i;
            } else if (i <= lastSetIndex + selectRange) {
                includeIndizes[i] = true;
            }
        }
        lastSetIndex = Integer.MAX_VALUE;
        for (int i = includeIndizes.length - 1; i >= 0; i--) {
            if (includeIndizes[i]) {
                lastSetIndex = i;
            } else if (i >= lastSetIndex - selectRange) {
                includeIndizes[i] = true;
            }
        }
    }

    /**
     * If configured, we include the already translated parts of the page as example.
     */
    protected GPTConfiguration maybeIncludeAlreadyTranslatedTextAsExample(
            List<PropertyToTranslate> propertiesToTranslate,
            AutoTranslateCaConfig autoTranslateCaConfig, GPTConfiguration configuration) {
        boolean includeExistingTranslationsInRetranslation =
                configurationOrOverride(autoTranslateConfigService.includeExistingTranslationsInRetranslation(),
                        autoTranslateCaConfig.includeExistingTranslationsInRetranslation(),
                        !propertiesToTranslate.isEmpty() ? propertiesToTranslate.get(0).targetResource.getPath() : null
                );

        String alreadyTranslatedText = propertiesToTranslate.stream()
                .filter(p -> p.isAlreadyCorrectlyTranslated)
                .map(PropertyToTranslate::getTargetValue)
                .collect(Collectors.joining("\n"));

        if (includeExistingTranslationsInRetranslation && StringUtils.isNotBlank(alreadyTranslatedText)) {
            configuration = configuration.merge(GPTConfiguration.ofContext(
                    "Retrieve the result of a previous translation of parts of a previous version of the text. " +
                            "You don't need to translate this - this is just contextual information and " +
                            "you can draw on that for translation examples and context of the translation that is done later.",
                    // we have to follow the final format or that is confusing for the AI
                    MULTITRANSLATION_SEPARATOR_START + LASTID + MULTITRANSLATION_SEPARATOR_END +
                            alreadyTranslatedText +
                            MULTITRANSLATION_SEPARATOR_START + LASTID + MULTITRANSLATION_SEPARATOR_END
            ));
        }
        return configuration;
    }

    /**
     * Allows a boolean configuration to be overridden with an optional value from the context-aware configuration. If the override array has several values we just take the first one.
     */
    protected boolean configurationOrOverride(boolean defaultvalue, String override, String path) {
        if (override != null && !override.trim().isEmpty()) {
            if ("TRUE".equalsIgnoreCase(override.trim())) {
                return true;
            } else if ("FALSE".equalsIgnoreCase(override.trim())) {
                return false;
            } else {
                LOG.error("Invalid value for boolean configuration {} for path {}", override, path);
            }
        }
        return defaultvalue;
    }

    /**
     * Checks whether there are href="path" in the translatedValues where path is within blueprintPath
     * and replaces those with the according path in the live copy.
     */
    protected List<String> remapPaths(List<String> translatedValues, String blueprintPath, String livecopyPath) {
        return translatedValues.stream().map(val -> remapPaths(val, blueprintPath, livecopyPath)).collect(Collectors.toList());
    }

    /**
     * We find all href="path" patterns
     *
     * @see #remapPaths(List, String, String)
     */
    protected String remapPaths(String translatedValue, String blueprintPath, String livecopyPath) {
        if (translatedValue == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("href=\"" +
                Pattern.quote(blueprintPath) + "(/[^\"]*)\"");
        String result = pattern.matcher(translatedValue).replaceAll("href=\"" + livecopyPath + "$1\"");
        return result;
    }

    private String determineSourceLanguage(Resource resource) throws WCMException {
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship == null) {
            return null;
        }
        String sourceLanguage = SelectorUtils.findLanguage(resource.getResourceResolver().getResource(relationship.getSourcePath()));
        return sourceLanguage;
    }

    protected void markAsAiTranslated(Resource resource, LiveRelationship liveRelationship, AutoTranslateService.TranslationParameters parameters, GPTConfiguration configuration) throws WCMException {
        ModifiableValueMap valueMap = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(null, valueMap, null);
        String userID = parameters.userId != null ? parameters.userId : resource.getResourceResolver().getUserID();
        targetWrapper.setAiTranslatedBy(userID);
        targetWrapper.setAiTranslatedDate(Calendar.getInstance());
        if (configuration != null) {
            targetWrapper.setAiTranslatedModel(configuration.highIntelligenceNeededIsSet() ? "hi" : "standard");
        }
        if (liveRelationship != null) {
            liveRelationshipManager.cancelPropertyRelationship(resource.getResourceResolver(),
                    liveRelationship, targetWrapper.allGeneralKeys(), false);
        }
        valueMap.put(AI_TRANSLATION_ERRORMARKER, Boolean.FALSE); // reset error marker if there was one.
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
            boolean pathsChanged = migratePathsToLanguageCopy(child, language, stats);
            changed = pathsChanged || changed;
        }
        ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        if (mvm != null) {
            for (Map.Entry<String, Object> entry : new HashMap<>(mvm).entrySet()) {
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
    public void rollback(Resource resource) throws WCMException, PersistenceException {
        if (resource == null) {
            throw new IllegalArgumentException("Resource does not exist.");
        }
        LOG.info("Rolling back {}", resource.getPath());
        ModifiableValueMap mvm = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        LiveRelationship relationship = null;
        Set<String> resetPropertyExclusionKeys = new HashSet<>();
        for (String key : new ArrayList<>(mvm.keySet())) {
            if (AITranslatePropertyWrapper.isAiTranslateProperty(key)) {
                continue; // will be removed when working on the original key.
            }

            AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(null, mvm, key);
            boolean reenable = false;
            if (targetWrapper.hasSavedTranslation()) {
                targetWrapper.setCurrentValue(targetWrapper.getOriginalCopy());
                reenable = true;
            }
            if (isNotBlank(targetWrapper.getLcOriginal())) {
                targetWrapper.setCurrentValue(targetWrapper.getLcOriginal());
                reenable = true;
            }
            if (reenable) {
                if (relationship == null) { // on demand since expensive calculation
                    relationship = liveRelationshipManager.getLiveRelationship(resource, false);
                }
                reenableInheritance(resource, key, relationship);
            }

            String[] allKeys = targetWrapper.allKeys();
            Arrays.stream(allKeys).forEach(mvm::remove);
            resetPropertyExclusionKeys.addAll(Arrays.asList(allKeys));
            targetWrapper.setAiTranslatedBy(null);
            targetWrapper.setAiTranslatedDate(null);
            targetWrapper.setAiTranslatedModel(null);
            resetPropertyExclusionKeys.addAll(Arrays.asList(targetWrapper.allGeneralKeys()));
        }
        if (relationship != null) {
            liveRelationshipManager.reenablePropertyRelationship(resource.getResourceResolver(), relationship,
                    resetPropertyExclusionKeys.toArray(new String[0]), false);
            resource.getResourceResolver().commit();
        }
        for (Resource child : resource.getChildren()) {
            rollback(child);
        }
    }

    /**
     * Searches for properties we have to translate.
     *
     * @param propertiesToTranslate list to add the properties to translate to - output parameter
     * @param force                 all properties have to be retranslated
     * @return true if something was changed already
     */
    protected boolean collectPropertiesToTranslate(
            @Nonnull Resource resource, @Nonnull List<PropertyToTranslate> propertiesToTranslate, @Nonnull Stats stats,
            @Nonnull AutoTranslateService.TranslationParameters translationParameters, boolean force) throws WCMException {
        boolean changed = false;
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship == null) {
            LOG.warn("No live relationship for {}", resource.getPath());
            return false;
        }
        String sourcePath = relationship.getSourcePath();
        Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
        if (sourceResource == null || !autoTranslateConfigService.isTranslatableResource(sourceResource)) {
            if (sourceResource == null) {
                LOG.info("No source resource found - translation not touching {}", resource.getPath());
                // that can happen for new resources in the live copy. Unfortunately it's not really clear if we should
                // try to translate that or not - we'll probably learn about that in practice.
            }
            return false;
        }
        ValueMap sourceValueMap = sourceResource.getValueMap();
        ModifiableValueMap targetValueMap = requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        for (String key : autoTranslateConfigService.translateableAttributes(sourceResource)) {
            if (AITranslatePropertyWrapper.isAiTranslateProperty(key)) {
                continue;
            }
            stats.translateableProperties++;
            AITranslatePropertyWrapper targetWrapper = new AITranslatePropertyWrapper(sourceValueMap, targetValueMap, key);

            boolean isCancelled = isCancelled(resource, key, relationship);
            boolean isAlreadyCorrectlyTranslated = false;
            if (targetWrapper.isOriginalAsWhenLastTranslating() && !force && !isCancelled) {
                // shortcut: we have a recent translation already
                targetWrapper.setCurrentValue(targetWrapper.getTranslatedCopy());
                changed = changed || !StringUtils.equals(targetWrapper.getTranslatedCopy(), targetWrapper.getOriginalCopy());
                isAlreadyCorrectlyTranslated = true;
            }

            if (targetWrapper.hasSavedTranslation()) {
                stats.retranslatedProperties++;
            }

            LOG.trace("Translating {} in {}", key, resource.getPath());
            PropertyToTranslate propertyToTranslate = new PropertyToTranslate();
            propertyToTranslate.sourceResource = sourceResource;
            propertyToTranslate.targetResource = resource;
            propertyToTranslate.propertyName = key;
            propertyToTranslate.isAlreadyCorrectlyTranslated = isAlreadyCorrectlyTranslated;
            propertyToTranslate.isCancelled = isCancelled;
            propertiesToTranslate.add(propertyToTranslate);
            if (isCancelled) {
                LOG.debug("Skipping translation for {} in {} because it is cancelled", key, resource.getPath());
            }
        }
        for (Resource child : resource.getChildren()) {
            if (!PATTERN_IGNORED_SUBNODE_NAMES.matcher(child.getName()).matches()) {
                boolean childChanged = collectPropertiesToTranslate(child, propertiesToTranslate, stats, translationParameters, force);
                changed = childChanged || changed;
            }
        }
        return changed;
    }

    protected static boolean isCancelled(Resource resource, String key, LiveRelationship relationship) {
        if (relationship.getStatus() == null) {
            return false;
        }
        if (relationship.getStatus().isCancelled()) {
            return true;
        }
        if (relationship.getStatus().getCanceledProperties().contains(key)) {
            return true;
        }
        String[] cancelledProps = resource.getValueMap().get("cq:propertyInheritanceCancelled", String[].class);
        if (cancelledProps != null) {
            return Arrays.asList(cancelledProps).contains(key);
        }
        return false;
    }

    protected String collectApplicableTranslationRules(String path, List<PropertyToTranslate> allTranslateableProperties, @Nullable List<AutoTranslateRuleConfig> rules) {
        if (rules == null) {
            return null;
        }
        StringBuilder applicableRules = new StringBuilder();
        for (AutoTranslateRuleConfig rule : rules) {
            if (isApplicable(rule, path, allTranslateableProperties)) {
                applicableRules.append(rule.additionalInstructions()).append("\n");
            }
        }
        return applicableRules.length() > 0 ? applicableRules.toString() : null;
    }

    /**
     * Returns whether the rule is applicable. It is not applicable if there are no translateable properties, anyway,
     * if there is a {@link AutoTranslateRuleConfig#pathRegex()} that doesn't match the path, or
     * if there is a {@link AutoTranslateRuleConfig#contentPattern()} that doesn't match the content.
     */
    protected boolean isApplicable(@Nonnull AutoTranslateRuleConfig rule, @Nonnull String path, @Nonnull List<PropertyToTranslate> allTranslateableProperties) {
        if (allTranslateableProperties == null) {
            return false;
        }
        if (StringUtils.isNotBlank(rule.pathRegex()) && !path.matches(rule.pathRegex().trim())) {
            return false;
        }
        if (StringUtils.isBlank(rule.contentPattern())) {
            return true;
        }
        try {
            Pattern contentPattern = compileContentPattern(rule.contentPattern().trim());
            return allTranslateableProperties.stream()
                    .map(PropertyToTranslate::getSourceValue)
                    .anyMatch(value -> value != null && contentPattern.matcher(value).find());
        } catch (PatternSyntaxException e) {
            LOG.error("Error in pattern syntax for rule {} applicable to path {}", rule, path, e);
            return false;
        }
    }

    protected List<AutoTranslateRuleConfig> collectTranslationTables(
            AutoTranslateCaConfig autoTranslateCaConfig, @Nonnull Resource resource) {
        List<AutoTranslateRuleConfig> rules = new ArrayList<>();
        String translationInstructionTemplate = autoTranslateCaConfig.translationTableRuleText() != null
                && !autoTranslateCaConfig.translationTableRuleText().trim().isEmpty() ?
                autoTranslateCaConfig.translationTableRuleText() :
                DEFAULT_TRANSLATION_RULE_PATTERN;
        if (autoTranslateCaConfig != null && autoTranslateCaConfig.translationTables() != null) {
            for (AutoTranslateTranslationTableConfig tableConfig : autoTranslateCaConfig.translationTables()) {
                if (tableConfig.path() == null || tableConfig.path().isEmpty()) {
                    continue;
                }
                Map<String, String> rawRules;
                try {
                    rawRules = getRawRules(tableConfig, resource);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not read translation table " + tableConfig.path() +
                            " configured at " + resource.getPath() + " because of " + e, e);
                }
                for (Map.Entry<String, String> entry : rawRules.entrySet()) {
                    // we don't use MessageFormat because of complexities around ' : single quotes would have to be doubled.
                    String instructions = translationInstructionTemplate
                            .replace("{0}", entry.getKey())
                            .replace("{1}", entry.getValue());
                    AutoTranslateRuleConfig rule = new AutoTranslateRuleConfigContentRule(entry.getKey(), instructions);
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    protected Map<String, String> getRawRules(AutoTranslateTranslationTableConfig tableConfig, Resource resource) throws IOException {
        Resource tableResource = resource.getResourceResolver().getResource(tableConfig.path());
        if (tableResource == null) {
            throw new IllegalArgumentException("Translation table not found: " + tableConfig.path() +
                    " configured at " + resource.getPath());
        }
        Map<String, String> rules = new TranslationRuleExtractor().extractRules(tableResource,
                tableConfig.sheetIndex(), tableConfig.startRow(), tableConfig.keyColumn(), tableConfig.valueColumn());
        LOG.debug("Extracted rules from {} : {}", tableResource.getPath(), rules);
        return rules;
    }

    /**
     * The content match can be a word or phrase that must be present in the content of the page for the rule to match.
     * For example, 'Product' will match all pages that contain the word 'Product', case-insensitive.
     * Spaces will also match any whitespace.
     * If it contains any of the regex meta characters []()*+ it'll be treated as a regex.
     */
    @Nonnull
    protected static Pattern compileContentPattern(String contentMatch) {
        if (contentMatch.matches(".*[\\[(*+?].*")) {
            return Pattern.compile(contentMatch, Pattern.CASE_INSENSITIVE);
        }
        if (contentMatch.contains("*") || contentMatch.contains("?") || contentMatch.contains("[") ||
                contentMatch.contains("]") || contentMatch.contains("(") || contentMatch.contains(")")) {
            return Pattern.compile(contentMatch, Pattern.CASE_INSENSITIVE);
        }
        // whitespace in the pattern will be replaced with a regex that matches any whitespace
        return Pattern.compile(contentMatch.replaceAll("\\s+", "\\\\s+"), Pattern.CASE_INSENSITIVE);
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

        /**
         * True if the source value wasn't changed since the last translation - that is, the target value
         * still has a correct translation and should not be modified.
         */
        protected boolean isAlreadyCorrectlyTranslated;

        /**
         * Whether the inheritance is cancelled for that property. If it is it won't be changed, but might be used
         * as context for the translation and the properties for the merge tool are set.
         */
        protected boolean isCancelled;

        public String getSourceValue() {
            return sourceResource.getValueMap().get(propertyName, String.class);
        }

        public String getTargetValue() {
            return targetResource.getValueMap().get(propertyName, String.class);
        }

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
            if (isAlreadyCorrectlyTranslated) {
                sb.append(" (already translated)");
            }
            return sb.toString();
        }
    }


    /**
     * Simple implementation of this for content translation rules.
     */
    protected static class AutoTranslateRuleConfigContentRule implements AutoTranslateRuleConfig {
        private String contentPattern;
        private String additionalInstructions;

        public AutoTranslateRuleConfigContentRule(String contentPattern, String additionalInstructions) {
            this.contentPattern = contentPattern;
            this.additionalInstructions = additionalInstructions;
        }

        @Override
        public String pathRegex() {
            return null;
        }

        @Override
        public String contentPattern() {
            return contentPattern;
        }

        @Override
        public String additionalInstructions() {
            return additionalInstructions;
        }

        @Override
        public String comment() {
            return null;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return AutoTranslateRuleConfig.class;
        }

        @Override
        public String toString() {
            return "TranslationRule{" +
                    "contentPattern=\"" + contentPattern + '"' +
                    ", additionalInstructions=\"" + additionalInstructions + '"' +
                    '}';
        }
    }

}
