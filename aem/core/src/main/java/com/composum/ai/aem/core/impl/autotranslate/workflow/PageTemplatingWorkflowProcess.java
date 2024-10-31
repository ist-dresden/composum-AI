package com.composum.ai.aem.core.impl.autotranslate.workflow;

import static com.adobe.granite.workflow.PayloadMap.TYPE_JCR_PATH;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import com.composum.ai.backend.slingbase.experimential.AITemplatingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Triggers a call of the {@link AITemplatingService} on the current page.
 * If parameter "reset" is given, it does a resetToPrompts instead.
 * As process arguments it can be given a json {"reset":true} to reset the page to prompts.
 * The URL as source needs to be in the page somewhere.
 *
 * @see "https://ai.composum.com/aiPageTemplating.html"
 */
public class PageTemplatingWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(PageTemplatingWorkflowProcess.class);

    @Reference
    protected AITemplatingService aiTemplatingService;

    protected final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
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
                    if (isReset(workItem, metaDataMap)) {
                        aiTemplatingService.resetToPrompts(resource);
                    } else {
                        aiTemplatingService.replacePromptsInResource(resource, null, null, null);
                    }
                } else {
                    LOG.error("Autotranslate workflow started with wrong payload path - no resource found: {}", path);
                }
            } else {
                LOG.error("Autotranslate workflow started with wrong payload type: {}", workflowData.getPayloadType());
            }

        } catch (Exception e) {
            LOG.error("Failed to process page templating for {}", path, e);
            throw new WorkflowException("Failed to process page templating for " + path, e);
        }
    }


    protected boolean isReset(WorkItem workItem, MetaDataMap metaDataMap) throws WorkflowException {
        Object payload = workItem.getWorkflowData().getPayload();
        String processArguments = metaDataMap.get("PROCESS_ARGS", String.class); // e.g. {"reset":true}
        LOG.info("TriggerRollout workflow receiver {} , args {}", payload, processArguments);
        if (StringUtils.isNotBlank(processArguments)) {
            try {
                Map parameters = gson.fromJson(processArguments, Map.class);
                return parameters.containsKey("reset") && (Boolean) parameters.get("reset");
            } catch (JsonSyntaxException | ClassCastException | IllegalArgumentException e) {
                LOG.error("Failed to parse process arguments: {} , ", processArguments, e);
                throw new ValidationException("Failed to parse process arguments " + processArguments, e);
            }
        }
        return false;
    }

}
