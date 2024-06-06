package com.composum.ai.aem.core.impl.autotranslate.rollout;


import java.util.Objects;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateService;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateCaConfig;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateService;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveAction;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.commons.BaseAction;
import com.day.cq.wcm.msm.commons.BaseActionFactory;

public class AutoTranslateLiveActionImpl extends BaseAction implements AutoTranslateLiveAction {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateLiveActionImpl.class);

    public static final String NAME = "composumAiAutoTranslate";

    protected final AutoPageTranslateService autoPageTranslateService;

    protected final AIConfigurationService configurationService;

    protected final AutoTranslateService autoTranslateService;

    protected AutoTranslateLiveActionImpl(ValueMap config, BaseActionFactory<? extends LiveAction> liveActionFactory,
                                          AutoPageTranslateService autoPageTranslateService, AIConfigurationService configurationService, AutoTranslateService autoTranslateService) {
        super(config, liveActionFactory);
        this.autoPageTranslateService = autoPageTranslateService;
        this.configurationService = configurationService;
        this.autoTranslateService = autoTranslateService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean handles(Resource source, Resource target, LiveRelationship relation, boolean isResetRollout)
            throws RepositoryException, WCMException {
        boolean isContentNode = target != null && JcrConstants.JCR_CONTENT.equals(target.getName());
        if (isContentNode) {
            LOG.debug("handles({}, {}, {})", relation.getSourcePath(), relation.getTargetPath(), isResetRollout);
        } else {
            LOG.trace("handles({}, {}, {})", relation.getSourcePath(), relation.getTargetPath(), isResetRollout);
        }
        return isContentNode;
    }

    @Override
    protected void doExecute(Resource source, Resource target, LiveRelationship liveRelationship, boolean autoSave)
            throws RepositoryException, WCMException {
        LOG.debug("doExecute({}, {}, {})", liveRelationship.getSourcePath(), liveRelationship.getTargetPath(), autoSave);
        GPTConfiguration config = configurationService.getGPTConfiguration(target.getResourceResolver(), target.getPath());
        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        parms.recursive = false;
        parms.autoSave = autoSave;
        parms.breakInheritance = false;
        ConfigurationBuilder confBuilder = Objects.requireNonNull(target.adaptTo(ConfigurationBuilder.class));
        AutoTranslateCaConfig autoTranslateCaConfig = confBuilder.as(AutoTranslateCaConfig.class);
        parms.additionalInstructions = autoTranslateCaConfig.additionalInstructions();
        if (autoTranslateCaConfig != null && autoTranslateCaConfig.preferHighIntelligenceModel()) {
            config = GPTConfiguration.HIGH_INTELLIGENCE.merge(config, true);
        } else if (autoTranslateCaConfig != null && autoTranslateCaConfig.preferStandardModel()) {
            config = GPTConfiguration.STANDARD_INTELLIGENCE.merge(config, true);
        }
        // parms.translateWhenChanged probably only makes sense when differential translation is integrated.
        try {
            boolean duringLiveCopyCreation = liveRelationship.getStatus() == null || liveRelationship.getStatus().getLastRolledOut() == null;
            // that works only for the root page. For the rest we use this hack to determine whether the user is waiting or this is an asynchronous rollout.
            duringLiveCopyCreation = duringLiveCopyCreation || (target.getResourceResolver().getAttribute("sling.authType") != null);
            LOG.debug("duringLiveCopyCreation: {}", duringLiveCopyCreation);
            if (duringLiveCopyCreation) {
                // do that afterward since we cannot access all live relationships from the manager
                // and cancel properties since they aren't saved yet, or the user is waiting for us.
                autoTranslateService.startTranslation(target.getResourceResolver(), target.getPath(), parms, config);
            } else {
                autoPageTranslateService.translateLiveCopy(target, config, parms);
            }
//        } catch (PersistenceException | LoginException e) {
//            throw new WCMException("Error translating " + source.getPath(), e);
        } catch (Exception e) { // rather log exception for now since a demo is coming...
            LOG.error("Error translating " + source.getPath(), e);
        }
    }

}
