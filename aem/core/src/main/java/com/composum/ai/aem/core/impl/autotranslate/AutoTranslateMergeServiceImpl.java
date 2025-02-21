package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.IterableUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    @Nonnull
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
                                    AITranslatePropertyWrapper.AI_ORIGINAL_SUFFIX, res);
                            if (propertyName != null) {
                                LOG.debug("Found property: {}", propertyName);
                                AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
                                list.add(new AutoTranslateProperty(res.getPath(), relationship.getTargetPath(), wrapper, getComponentName(res), getComponentTitle(res)));
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

    @Override
    public Map<String, String> saveTranslation(@Nonnull Resource resource, @Nonnull String propertyName, @Nonnull String content, @Nonnull boolean markAsMerged) throws WCMException {
        ModifiableValueMap properties = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(resource, false);
        if (relationship != null) {
            String sourcePath = relationship.getSourcePath();
            Resource sourceResource = resource.getResourceResolver().getResource(sourcePath);
            AITranslatePropertyWrapper wrapper = new AITranslatePropertyWrapper(sourceResource.getValueMap(), properties, propertyName);
            wrapper.setCurrentValue(content);
            if (markAsMerged) {
                if (wrapper.getNewOriginalCopy() == null || wrapper.getNewTranslatedCopy() == null) {
                    LOG.warn("Already merged? Property {} on resource {} has no original or translated copy", propertyName, resource.getPath());
                    wrapper.setOriginalCopy(wrapper.getNewOriginalCopy());
                    wrapper.setTranslatedCopy(wrapper.getNewTranslatedCopy());
                }
                wrapper.setNewOriginalCopy(null); // that's the "needs merge" marker
                wrapper.setNewTranslatedCopy(null);
            }
            return Collections.singletonMap("saved", wrapper.getCurrentValue());
        }
        return Collections.emptyMap();
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
        String resourceType = null;
        ResourceResolver resolver = resource.getResourceResolver();
        while (resourceType == null && resource != null) {
            resourceType = resource.getValueMap().get("sling:resourceType", String.class);
            resource = resource.getParent();
        }
        if (resourceType != null) {
            Resource componentResource = resolver.getResource(resourceType);
            if (componentResource != null) {
                return componentResource.getValueMap().get("jcr:title", String.class);
            }
        }
        return null;
    }

    /**
     * Determines the jcr:title , title or text of the component by searching upwards for such a property.
     */
    protected String getComponentTitle(@Nonnull Resource resource) {
        while (resource != null && resource.getValueMap().get("sling:resourceType") == null) {
            resource = resource.getParent();
        }
        if (resource != null) {
            String title = resource.getValueMap().get("jcr:title", String.class);
            if (title == null) {
                title = resource.getValueMap().get("title", String.class);
            }
            if (title == null) {
                title = resource.getValueMap().get("text", String.class);
            }
            if (title == null) {
                title = resource.getValueMap().get("jcr:description", String.class);
            }
            if (title != null) { // remove HTML tags
                title = title.replaceAll("</?[a-zA-Z][^>]*/?>", "");
            }
            return title;
        }
        return null;
    }

}
