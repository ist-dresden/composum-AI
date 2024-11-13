package com.composum.ai.aem.core.impl.autotranslate.rollout;


import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_TRANSLATION_ERRORMARKER;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateService;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveAction;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.commons.BaseAction;
import com.day.cq.wcm.msm.commons.BaseActionFactory;

/** Implementation for the rollout configuration.  */
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
            throws RepositoryException {
        boolean isContentNode = source != null && target != null && BaseAction.isPage(source.adaptTo(Node.class))
                && target.adaptTo(Node.class) != null;
        // target != null && JcrConstants.JCR_CONTENT.equals(target.getName()) && !source.isResourceType(NT_RESOURCE);
        if (isContentNode) {
            LOG.debug("handles({}, {}, {})", relation.getSourcePath(), relation.getTargetPath(), isResetRollout);
        } else {
            LOG.trace("handles({}, {}, {})", relation.getSourcePath(), relation.getTargetPath(), isResetRollout);
        }
        return isContentNode;
    }

    @Override
    protected void doExecute(Resource source, Resource target, LiveRelationship liveRelationship, boolean autoSave) throws WCMException {
        String id = (Math.abs(Math.random()) + "").substring(2, 8);
        LOG.info(">>>{} doExecute({}, {}, {})", id, liveRelationship.getSourcePath(), liveRelationship.getTargetPath(), autoSave);
        AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
        parms.recursive = false;
        parms.autoSave = autoSave;
        parms.breakInheritance = false;
        // parms.translateWhenChanged probably only makes sense when differential translation is integrated.
        try {
            boolean duringLiveCopyCreation = liveRelationship.getStatus() == null || liveRelationship.getStatus().getLastRolledOut() == null;
            // that works only for the root page. For the rest we use this hack to determine whether the user is waiting or this is an asynchronous rollout.
            duringLiveCopyCreation = duringLiveCopyCreation || (target.getResourceResolver().getAttribute("sling.authType") != null);
            if (duringLiveCopyCreation) {
                LOG.info("duringLiveCopyCreation: {}", duringLiveCopyCreation);
            }
            if (duringLiveCopyCreation) {
                // do that afterward since we cannot access all live relationships from the manager
                // and cancel properties since they aren't saved yet, or the user is waiting for us.
                autoTranslateService.startTranslation(target.getResourceResolver(), target.getPath(), parms);
            } else {
                autoPageTranslateService.translateLiveCopy(target, parms);
            }
       } catch (Exception e) {
            // Throwing an exception here will abort a whole recursive rollout, which is a problem.
            // So we rather mark failures (which should be infrequent) so that they are easy to find.
            LOG.error("Error translating " + source.getPath(), e);
            markAsError(target);
        } finally {
            LOG.info("<<<{} doExecute({}, {}, {})", id, liveRelationship.getSourcePath(), liveRelationship.getTargetPath(), autoSave);
        }
    }

    /** Make it easy to find pages with errors by marking page with AI_TRANSLATION_ERRORMARKER. */
    protected static void markAsError(Resource target) {
        Calendar date = Calendar.getInstance();
        try (ResourceResolver nestedResolver = target.getResourceResolver().clone(null)) {
            // use a copy of the resolver since that somehow doesn't work with the original resolver
            Resource targetNested = nestedResolver.getResource(target.getPath());
            targetNested.adaptTo(ModifiableValueMap.class).put(AI_TRANSLATION_ERRORMARKER, date);
            nestedResolver.commit();
            target.getResourceResolver().refresh();
        } catch (Exception e) {
            LOG.warn("Error marking " + target.getPath() + " as error", e);
        }
    }

}
