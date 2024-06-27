package com.composum.ai.aem.core.impl.autotranslate.workflow;

import static com.adobe.granite.workflow.PayloadMap.TYPE_JCR_PATH;

import java.util.Iterator;

import javax.annotation.Nonnull;

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
import com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateService;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateConfigService;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateService.TranslationParameters;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Autotranslate workflow. This triggers a automatic translation of a live copy from it's blueprint. As process
 * arguments a JSON can be given that configures the translation process - a serialization of {@link TranslationParameters},
 * e.g. {"autoSave":false,"breakInheritance":false,"translateWhenChanged":true,"recursive":false} .
 */
@Component(service = WorkflowProcess.class,
        property = {"process.label=Composum AI Autotranslate page tree",
                Constants.SERVICE_VENDOR + "=IST Gmbh Dresden" +
                        Constants.SERVICE_DESCRIPTION + "=Automatic translation of the page tree from it's blueprint.",
        })
public class AutoTranslateWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateWorkflowProcess.class);

    @Reference
    protected AutoPageTranslateService autoPageTranslateService;

    @Reference
    protected AutoTranslateConfigService autoTranslateConfigService;

    @Reference
    protected AIConfigurationService configurationService;

    protected final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        if (!autoTranslateConfigService.isEnabled()) {
            throw new IllegalStateException("AutoTranslate is not enabled");
        }

        Object payload = workItem.getWorkflowData().getPayload();
        // e.g. {"autoSave":false,"breakInheritance":false,"translateWhenChanged":true,"recursive":false}
        String processArguments = metaDataMap.get("PROCESS_ARGS", String.class);
        LOG.info("Autotranslate workflow started for {} , args {}", payload, processArguments);

        try {
            ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            WorkflowData workflowData = workItem.getWorkflowData();
            if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
                String path = workflowData.getPayload().toString();
                if (path == null || !path.startsWith("/content/")) {
                    LOG.error("Workflow started with wrong payload path {}", path);
                    throw new IllegalArgumentException("Workflow started with wrong payload path: " + path);
                }
                Resource resource = path != null ? resourceResolver.getResource(path) : null;
                if (resource != null) {
                    LOG.info("Autotranslate workflow started for: {}", resource.getPath());
                    translate(resource, processArguments);
                } else {
                    LOG.error("Autotranslate workflow started with wrong payload path - no resource found: {}", path);
                }
            } else {
                LOG.error("Autotranslate workflow started with wrong payload type: {}", workflowData.getPayloadType());
            }
        } catch (PersistenceException | WCMException | RuntimeException e) {
            LOG.error("Aborting workflow step because translation failed for {}", payload, e);
            throw new WorkflowException("Failed to translate", e);
        }
        LOG.info("Autotranslate workflow finished for {}", payload);
    }

    protected TranslationParameters getTranslationParameters(String processArguments) throws WorkflowException {
        TranslationParameters parameters;
        try {
            parameters = gson.fromJson(processArguments, TranslationParameters.class);
            if (parameters == null) {
                parameters = new TranslationParameters();
                parameters.autoSave = true;
                LOG.info("Using default parameters {}", parameters);
            } else {
                LOG.debug("Using parameters {}", parameters);
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Failed to parse process arguments: {}", processArguments, e);
            throw new WorkflowException("Failed to parse process arguments", e);
        }
        return parameters;
    }

    /**
     * We only translate jcr:content resources or resources that are within a jcr:content. If recursive is enabled, we search for the jcr:content recursively.
     */
    protected void translate(@Nonnull Resource resource, String processArguments)
            throws PersistenceException, WCMException, WorkflowException {
        TranslationParameters parms = getTranslationParameters(processArguments);
        translate(resource, parms, 0);
    }

    protected void translate(@Nonnull Resource resource, TranslationParameters parms, int depth)
            throws PersistenceException, WCMException, WorkflowException {
        if (parms.maxDepth != null && depth > parms.maxDepth) {
            LOG.info("Ignoring because max depth reached for resource: {}", resource.getPath());
            return;
        }

        Resource contentResource = null;
        if (resource.getPath().contains("/jcr:content")) {
            contentResource = resource;
        } else if (resource.getChild("jcr:content") != null) {
            contentResource = resource.getChild("jcr:content");
        }

        if (contentResource != null) {
            try {
                autoPageTranslateService.translateLiveCopy(contentResource, parms);
            } catch (PersistenceException | WCMException | RuntimeException e) { // make sure we log the actual path
                LOG.error("Failed to translate resource: {}", resource.getPath(), e);
                throw e;
            }
        } else {
            LOG.info("No need to translate resource without content: {}", resource.getPath());
        }

        if (parms.recursive) {
            // recursively looks for other jcr:content nodes
            Iterator<Resource> childIterator = resource.listChildren();
            while (childIterator.hasNext()) {
                Resource child = childIterator.next();
                // skip jcr:content node since that has been translated already
                if (!child.getPath().contains("/jcr:content")) {
                    translate(child, parms, depth + 1);
                }
            }
        }
    }

}
