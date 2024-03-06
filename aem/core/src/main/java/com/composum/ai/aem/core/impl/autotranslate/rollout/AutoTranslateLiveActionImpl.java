package com.composum.ai.aem.core.impl.autotranslate.rollout;


import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateService;
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

    protected AutoTranslateLiveActionImpl(ValueMap config, BaseActionFactory<? extends LiveAction> liveActionFactory,
                                          AutoPageTranslateService autoPageTranslateService, AIConfigurationService configurationService) {
        super(config, liveActionFactory);
        this.autoPageTranslateService = autoPageTranslateService;
        this.configurationService = configurationService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean handles(Resource source, Resource target, LiveRelationship relation, boolean isResetRollout)
            throws RepositoryException, WCMException {
        return target != null && JcrConstants.JCR_CONTENT.equals(target.getName());
    }

    @Override
    protected void doExecute(Resource source, Resource target, LiveRelationship liveRelationship, boolean autoSave)
            throws RepositoryException, WCMException {
        GPTConfiguration config = configurationService.getGPTConfiguration(target.getResourceResolver(), target.getPath());
        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        parms.recursive = false;
        parms.autoSave = autoSave;
        parms.breakInheritance = false;
        // parms.additionalInstructions
        // parms.translateWhenChanged
        try {
            autoPageTranslateService.translateLiveCopy(target, config, parms);
        } catch (PersistenceException e) {
            throw new WCMException("Error translating " + source.getPath(), e);
        }
    }

}
