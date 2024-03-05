package com.composum.ai.aem.core.impl.autotranslate.rollout;


import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveAction;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.commons.BaseAction;
import com.day.cq.wcm.msm.commons.BaseActionFactory;

public class AutoTranslateLiveActionImpl extends BaseAction implements AutoTranslateLiveAction {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateLiveActionImpl.class);

    public static final String NAME = "composumAiAutoTranslate";

    protected AutoTranslateLiveActionImpl(ValueMap config, BaseActionFactory<? extends LiveAction> liveActionFactory) {
        super(config, liveActionFactory);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean handles(Resource source, Resource target, LiveRelationship relation, boolean isResetRollout)
            throws RepositoryException, WCMException {
        LOG.info("AutoTranslateLiveActionImpl.handles({}, {})", source.getPath(), target.getPath());
        return true;
    }

    @Override
    protected void doExecute(Resource resource, Resource resource1, LiveRelationship liveRelationship, boolean autoSave)
            throws RepositoryException, WCMException {
        LOG.error("AutoTranslateLiveActionImpl.doExecute({}, {})", resource.getPath(), resource1.getPath());
    }

}
