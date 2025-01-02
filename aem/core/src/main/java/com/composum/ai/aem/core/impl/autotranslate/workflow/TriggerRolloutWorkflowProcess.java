package com.composum.ai.aem.core.impl.autotranslate.workflow;

import static com.adobe.granite.workflow.PayloadMap.TYPE_JCR_PATH;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.ValidationException;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.msm.api.RolloutManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Translates the page that is given as payload from it's blueprint. The page has to be a live copy of the page it's translated from.
 * Configured as recursive: rolls out tree of pages.
 * This is conservative in that it only rolls out (again) pages that already have been rolled out, and where the source still exists. It does not delete old pages or create new pages, even when recursive.
 * <p>As process
 * arguments a JSON can be given that configures the translation process - a serialization of {@link TriggerRolloutParameters},
 * e.g. {"autoSave":false,"breakInheritance":false,"recursive":false} .</p>
 */
@Component(service = WorkflowProcess.class,
        property = {"process.label=Composum AI Rollout To Here",
                Constants.SERVICE_VENDOR + "=IST Gmbh Dresden" +
                        Constants.SERVICE_DESCRIPTION + "=Rolls out the current page (optionally recursively) from it's blueprint, triggering automatic translation if that's configured as rollout configuration.",
        })
public class TriggerRolloutWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerRolloutWorkflowProcess.class);

    @Reference
    protected RolloutManager rolloutManager;

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Reference
    protected AutoTranslateConfigService autoTranslateConfigService;

    protected final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        if (!autoTranslateConfigService.isEnabled()) {
            throw new IllegalStateException("AutoTranslate is not enabled");
        }
        TriggerRolloutParameters processArguments = getParameters(workItem, metaDataMap);
        String path = null;

        try (ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class)) {
            WorkflowData workflowData = workItem.getWorkflowData();
            if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
                path = workflowData.getPayload().toString();
                if (path == null || !path.startsWith("/content/")) {
                    LOG.error("Workflow started with wrong payload path {}", path);
                    throw new IllegalArgumentException("Workflow started with wrong payload path: " + path);
                }
                Resource resource = resourceResolver.getResource(path);
                if (resource != null) {
                    LOG.debug("TriggerRollout workflow started for: {}", resource.getPath());
                    performRollouts(resource, processArguments);
                } else {
                    LOG.error("Autotranslate workflow started with wrong payload path - no resource found: {}", path);
                }
            } else {
                LOG.error("Autotranslate workflow started with wrong payload type: {}", workflowData.getPayloadType());
            }
        } catch (PersistenceException | WCMException | RuntimeException e) {
            LOG.error("Aborting workflow step because translation failed for {} path {}", processArguments, path, e);
            throw new WorkflowException("Failed to translate", e);
        } finally {
            LOG.info("Autotranslate workflow finished for {} path {}", processArguments, path);
        }
    }

    protected TriggerRolloutParameters getParameters(WorkItem workItem, MetaDataMap metaDataMap) throws WorkflowException {
        TriggerRolloutParameters triggerRolloutParameters = new TriggerRolloutParameters();
        Object payload = workItem.getWorkflowData().getPayload();
        String processArguments = metaDataMap.get("PROCESS_ARGS", String.class); // e.g. {"recursive":false}
        LOG.info("TriggerRollout workflow receiver {} , args {}", payload, processArguments);
        if (StringUtils.isNotBlank(processArguments)) {
            try {
                triggerRolloutParameters = gson.fromJson(processArguments, TriggerRolloutParameters.class);
            } catch (JsonSyntaxException e) {
                LOG.error("Failed to parse process arguments: {} , ", processArguments, e);
                throw new ValidationException("Failed to parse process arguments " + processArguments, e);
            }
        }
        return triggerRolloutParameters;
    }

    /**
     * Triggers a rollout for the jcr:content cq:Page or all such subnodes if {@link TriggerRolloutParameters#recursive}.
     */
    protected void performRollouts(Resource resource, TriggerRolloutParameters parms) throws PersistenceException, WCMException {
        List<String> pathsToRollout = new ArrayList<>();
        collectPathsToRollout(resource, pathsToRollout, parms);
        ResourceResolver resourceResolver = resource.getResourceResolver();
        for (String path : pathsToRollout) {
            Resource contentResource = resourceResolver.getResource(path);
            if (contentResource != null && contentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals("cq:PageContent")) {
                LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(contentResource, false);
                if (liveRelationship != null) {
                    Resource source = resource.getResourceResolver().getResource(liveRelationship.getSourcePath());
                    if (source != null) {
                        LOG.info("Triggering rollout for {}", contentResource.getPath());
                        rolloutManager.rollout(resource.getResourceResolver(), liveRelationship, false, true);
                    } else {
                        LOG.info("Ignoring: no source found for {}", contentResource.getPath());
                    }
                } else {
                    LOG.info("Ignoring: no live relationship found for {}", contentResource.getPath());
                }
            }
        }
    }

    protected void collectPathsToRollout(Resource resource, List<String> pathsToRollout, TriggerRolloutParameters parms) {
        Resource contentResource = resource.getName().equals(JcrConstants.JCR_CONTENT) ? resource : resource.getChild("jcr:content");
        if (contentResource != null && contentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals("cq:PageContent")) {
            pathsToRollout.add(contentResource.getPath());
        }
        if (parms.recursive) {
            for (Resource child : resource.getChildren()) {
                if (!child.getName().equals(JcrConstants.JCR_CONTENT)) { // jcr:content was already done
                    collectPathsToRollout(child, pathsToRollout, parms);
                }
            }
        }
    }

    protected void performRolloutForPage(Resource contentResource, TriggerRolloutParameters parms)
            throws PersistenceException, WCMException {
        if (contentResource != null && contentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals("cq:PageContent")) {
            LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(contentResource, false);
            if (liveRelationship != null) {
                Resource source = contentResource.getResourceResolver().getResource(liveRelationship.getSourcePath());
                if (source != null) {
                    LOG.info("Triggering rollout for {}", contentResource.getPath());
                    rolloutManager.rollout(contentResource.getResourceResolver(), liveRelationship, false, true);
                } else {
                    LOG.info("Ignoring: no source found for {}", contentResource.getPath());
                }
            } else {
                LOG.info("Ignoring: no live relationship found for {}", contentResource.getPath());
            }
        }
    }


    public class TriggerRolloutParameters {
        public boolean recursive = false;

        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(this);
            if (Boolean.TRUE.equals(recursive)) {
                builder.append("recursive", recursive);
            }
            return builder.toString();
        }
    }

}
