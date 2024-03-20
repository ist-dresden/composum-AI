package com.composum.ai.aem.core.impl.autotranslate.workflow;

import static com.adobe.granite.workflow.PayloadMap.TYPE_JCR_PATH;

import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
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
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateCaConfig;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Autotranslate workflow.
 */
@Component(service = WorkflowProcess.class,
        property = {"process.label=Composum AI Autotranslate page tree",
                Constants.SERVICE_VENDOR + "=IST Gmbh Dresden" +
                        Constants.SERVICE_DESCRIPTION + "=Automatic translation of the page tree from it's blueprint.",
        })
public class AutoTranslateWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateWorkflowProcess.class);

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    protected AutoPageTranslateService autoPageTranslateService;

    @Reference
    protected AutoTranslateConfigService autoTranslateConfigService;

    @Reference
    protected AIConfigurationService configurationService;

    protected final Gson gson = new Gson();

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
            PageManager pageManager = Objects.requireNonNull(resourceResolver.adaptTo(PageManager.class), "No Pagemanager.");
            WorkflowData workflowData = workItem.getWorkflowData();
            if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
                String path = workflowData.getPayload().toString();
                Resource resource = path != null ? resourceResolver.getResource(path) : null;
                Page page = resource != null ? pageManager.getPage(path) : null;
                if (page != null) {
                    LOG.info("Autotranslate workflow started for page: {}", page.getPath());
                    translate(page, processArguments);
                } else {
                    LOG.error("Autotranslate workflow started with wrong payload path - no page found: {}", path);
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

    protected void translate(Page page, String processArguments) throws PersistenceException, WCMException, WorkflowException {
        if (page.getContentResource() == null) {
            LOG.info("No need to translate page without content: {}", page.getPath());
            return;
        }

        TranslationParameters parms = getTranslationParameters(processArguments);
        ConfigurationBuilder confBuilder = Objects.requireNonNull(page.getContentResource().adaptTo(ConfigurationBuilder.class));
        AutoTranslateCaConfig autoTranslateCaConfig = confBuilder.as(AutoTranslateCaConfig.class);
        if (autoTranslateCaConfig != null && autoTranslateCaConfig.additionalInstructions() != null) {
            parms.additionalInstructions =
                    (StringUtils.defaultString(parms.additionalInstructions) + "\n\n" +
                    autoTranslateCaConfig.additionalInstructions()).trim();
        }

        try {
            Resource contentResource = page.getContentResource();
            if (contentResource != null) {
                GPTConfiguration config = configurationService.getGPTConfiguration(contentResource.getResourceResolver(), contentResource.getPath());
                if (parms.additionalInstructions != null) {
                    config = GPTConfiguration.merge(config,
                            new GPTConfiguration(null, null, null, parms.additionalInstructions));
                }
                autoPageTranslateService.translateLiveCopy(contentResource, config, parms);
            }
        } catch (PersistenceException | WCMException | RuntimeException e) { // make sure we log the actual path
            LOG.error("Failed to translate page: {}", page.getPath(), e);
            throw e;
        }

        if (parms.recursive) {
            for (Iterator<Page> it = page.listChildren(); it.hasNext(); ) {
                translate(it.next(), processArguments);
            }
        }
    }

}
