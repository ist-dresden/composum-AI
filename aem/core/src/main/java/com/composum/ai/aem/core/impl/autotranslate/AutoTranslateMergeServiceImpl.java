package com.composum.ai.aem.core.impl.autotranslate;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.ComponentCancellationHelper;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatMessagesTemplate;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationService;
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

    /**
     * Name of template for merging translations.
     */
    private static final String TEMPLATE_AITRANSLATIONMERGE = "translationmerge";

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Reference
    private GPTChatCompletionService chatCompletionService;

    @Reference
    protected AIConfigurationService configurationService;

    public boolean isProcessingNeeded(Resource pageResource) {
        if (pageResource == null) {
            return false;
        }
        List<AutoTranslateProperty> props = getProperties(pageResource);
        return props.stream().anyMatch(AutoTranslateProperty::isProcessingNeeded);
    }

    @Override
    public boolean isAutomaticallyTranslated(Resource resource) {
        try {
            Resource contentResource = resource;
            if (!contentResource.getName().equals("jcr:content")) {
                while (contentResource != null && !contentResource.getName().equals("jcr:content")) {
                    contentResource = contentResource.getParent();
                }
            }
            LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(contentResource, true);
            if (relationship == null) {
                return false;
            }
            return contentResource.getValueMap().get(AITranslatePropertyWrapper.PROPERTY_AI_TRANSLATED_DATE) != null;
        } catch (WCMException e) {
            LOG.error("Could not determine relationships of " + resource.getPath(), e);
            return false;
        }
    }

    @Override
    @Nonnull
    public List<AutoTranslateProperty> getProperties(Resource pageResource) {
        List<AutoTranslateProperty> list = new ArrayList<>();
        descendantsStream(pageResource).forEach(res -> {
            ModifiableValueMap properties = res.adaptTo(ModifiableValueMap.class);
            try {
                LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(res, true);
                if (relationship != null) {
                    String sourcePath = relationship.getSourcePath();
                    Resource sourceResource = res.getResourceResolver().getResource(sourcePath);
                    if (sourceResource != null) {
                        Resource component = ComponentCancellationHelper.findNextHigherCancellableComponent(res);
                        if (component == null) {
                            LOG.warn("No component found for resource {}", res.getPath());
                            return;
                        }

                        for (String key : properties.keySet()) {
                            String propertyName = AITranslatePropertyWrapper.decodePropertyName(
                                    AITranslatePropertyWrapper.AI_PREFIX, key,
                                    AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, res.getValueMap());
                            if (propertyName != null) {
                                LOG.debug("Found property: {}", propertyName);
                                AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                                boolean processingNeeded;
                                if (relationship.getStatus().isCancelled() || relationship.getStatus().getCanceledProperties().contains(propertyName)) {
                                    processingNeeded = isNotBlank(wrapper.getNewOriginalCopy()) &&
                                            isNotBlank(wrapper.getNewTranslatedCopy());
                                } else { // not cancelled - processing needed if the current value is not the accepted translation
                                    processingNeeded = isBlank(wrapper.getAcceptedTranslation()) ||
                                            !StringUtils.equals(wrapper.getAcceptedTranslation(), wrapper.getCurrentValue());
                                }
                                list.add(new AutoTranslateProperty(res.getPath(), component.getPath(), wrapper, getComponentName(res), getComponentTitle(res), relationship, processingNeeded));
                            }
                        }
                    }
                }
            } catch (WCMException | RuntimeException e) {
                LOG.error("Exception for " + res.getPath(), e);
            }
        });
        return list;
    }

    @Override
    public Map<String, String> saveTranslation(@Nonnull Resource resource, @Nonnull String propertyName, @Nonnull String content, @Nonnull boolean markAsMerged) throws WCMException {
        ModifiableValueMap properties = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, true);
        if (relationship != null) {
            String sourcePath = relationship.getSourcePath();
            Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
            AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
            wrapper.setCurrentValue(content);
            wrapper.setTranslatedCopy(content);
            if (markAsMerged) {
                if (StringUtils.isNotBlank(wrapper.getNewOriginalCopy())) {
                    wrapper.setOriginalCopy(wrapper.getNewOriginalCopy());
                }
                wrapper.setNewOriginalCopy(null); // that resets the "needs merge" marker
                wrapper.setNewTranslatedCopy(null);
            }
            return Collections.singletonMap("saved", wrapper.getCurrentValue());
        }
        return Collections.emptyMap();
    }

    @Override
    public void approveTranslation(Resource resource, String propertyName) throws WCMException {
        ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, true);
        if (relationship != null) {
            String sourcePath = relationship.getSourcePath();
            Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
            AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
            wrapper.setAcceptedSource(wrapper.getOriginalCopy());
            wrapper.setAcceptedTranslation(wrapper.getCurrentValue());
        }
    }

    @Override
    public void changeInheritance(Resource resource, String propertyName, CancelOrReenable kind) throws WCMException {
        Resource componentResource = ComponentCancellationHelper.findNextHigherCancellableComponent(resource);
        if (componentResource == null) {
            LOG.warn("Bug: no component found for resource {}", resource.getPath());
            return;
        }
        ModifiableValueMap properties = componentResource.adaptTo(ModifiableValueMap.class);
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(componentResource, true);
        if (relationship != null) {
            String sourcePath = relationship.getSourcePath();
            Resource sourceResource = componentResource.getResourceResolver().getResource(sourcePath);
            switch (kind) {
                // also reset values that have no meaning anymore.
                case CANCEL:
                    if (StringUtils.isBlank(propertyName)) {
                        boolean deep = !ComponentCancellationHelper.isContainer(componentResource);
                        liveRelationshipManager.cancelRelationship(componentResource.getResourceResolver(), relationship, deep, true);
                    } else {
                        liveRelationshipManager.cancelPropertyRelationship(componentResource.getResourceResolver(), relationship, new String[]{propertyName}, true);
                        AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                        wrapper.adjustForReenableInheritance();
                    }
                    break;
                case REENABLE:
                    if (StringUtils.isBlank(propertyName)) {
                        liveRelationshipManager.reenableRelationship(componentResource.getResourceResolver(), relationship, true);
                        ComponentCancellationHelper.adjustPropertiesReenableInheritance(liveRelationshipManager, componentResource);
                    } else {
                        liveRelationshipManager.reenablePropertyRelationship(componentResource.getResourceResolver(), relationship, new String[]{propertyName}, true);
                        AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                        wrapper.adjustForReenableInheritance();
                    }
                    break;
            }
        }
    }

    @Override
    public String intelligentMerge(String language, @Nonnull Resource resource, @Nonnull String originalSource,
                                   @Nonnull String newSource, @Nonnull String newTranslation,
                                   @Nonnull String currentText) {
        GPTChatMessagesTemplate template = chatCompletionService.getTemplate(TEMPLATE_AITRANSLATIONMERGE);
        GPTConfiguration config = configurationService.getGPTConfiguration(resource.getResourceResolver(), resource.getPath());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("targetlanguage", language);
        parameters.put("originalSource", originalSource);
        parameters.put("currentText", currentText);
        parameters.put("newSource", newSource);
        parameters.put("newTranslation", newTranslation);
        parameters.put("addition", ""); // perhaps use translation instructions later, but those are implicitly in the new translation
        List<GPTChatMessage> messages = template.getMessages(parameters);

        GPTChatRequest request = new GPTChatRequest(messages);
        request.setConfiguration(config);
        int maxTokens = 3 * Math.max(chatCompletionService.countTokens(currentText), chatCompletionService.countTokens(newTranslation)) + 100;
        if (maxTokens < 4096) { // setting more than that could lead to trouble with weaker models
            request.setMaxTokens(maxTokens);
        }
        String result = chatCompletionService.getSingleChatCompletion(request);
        LOG.debug("Merging {} \n->\n {}", messages, result);
        return result;
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

    /**
     * Determines the jcr:title of the current component, as found by sling:resourceType
     */
    protected String getComponentName(Resource resource) {
        ResourceResolver resolver = resource.getResourceResolver();
        Resource componentResource = ComponentCancellationHelper.findNextHigherCancellableComponent(resource);
        String resourceType = componentResource.getValueMap().get("sling:resourceType", String.class);
        if (resourceType != null) {
            Resource componentTypeResource = resolver.getResource(resourceType);
            if (componentTypeResource != null) {
                return componentTypeResource.getValueMap().get("jcr:title", String.class);
            }
        }
        return null;
    }

    /**
     * Determines the jcr:title , title or text of the component by searching upwards for such a property.
     */
    protected String getComponentTitle(@Nonnull Resource resource) {
        Resource componentResource = ComponentCancellationHelper.findNextHigherCancellableComponent(resource);
        if (componentResource != null) {
            String title = componentResource.getValueMap().get("jcr:title", String.class);
            if (title == null) {
                title = componentResource.getValueMap().get("title", String.class);
            }
            if (title == null) {
                title = componentResource.getValueMap().get("text", String.class);
            }
            if (title == null) {
                title = componentResource.getValueMap().get("jcr:description", String.class);
            }
            if (title != null) { // remove HTML tags
                title = title.replaceAll("</?[a-zA-Z][^>]*/?>", "");
            }
            return title;
        }
        return null;
    }

}
