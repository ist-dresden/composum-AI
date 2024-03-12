package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateListModel {

    @OSGiService(optional = true)
    private AutoTranslateService autoTranslateService;

    @OSGiService(optional = true)
    private AIConfigurationService configurationService;

    @OSGiService(optional = true)
    private AutoTranslateConfigService autoTranslateConfigService;

    @Self
    private SlingHttpServletRequest request;

    private AutoTranslateService.TranslationRun run;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled() || configurationService == null
                || autoTranslateConfigService == null || !autoTranslateConfigService.isPocUiEnabled();
    }

    public List<AutoTranslateService.TranslationRun> getTranslationRuns() {
        return autoTranslateService.getTranslationRuns();
    }

    public AutoTranslateService.TranslationRun createRun() throws LoginException, PersistenceException {
        if (run == null) {
            String path = request.getParameter("path");
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("path parameter is required");
            }
            path = path.replaceAll("_jcr_content", "jcr:content").trim();
            boolean recursive = request.getParameter("recursive") != null;
            boolean changed = request.getParameter("translateWhenChanged") != null;
            String additionalInstructions = request.getParameter("additionalInstructions");
            boolean breakInheritance = request.getParameter("breakInheritance") != null;
            GPTConfiguration configuration = configurationService.getGPTConfiguration(request.getResourceResolver(), path);
            AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
            parms.recursive = recursive;
            parms.translateWhenChanged = changed;
            parms.additionalInstructions = additionalInstructions;
            parms.breakInheritance = breakInheritance;
            run = autoTranslateService.startTranslation(request.getResourceResolver(), path, parms, configuration);
        }
        return run;
    }

    public String rollback() throws WCMException, PersistenceException {
        String path = request.getParameter("path");
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        path = path.replaceAll("_jcr_content", "jcr:content").trim();
        Resource resource = request.getResourceResolver().getResource(path);
        autoTranslateService.rollback(resource);
        request.getResourceResolver().commit();
        return "rolled back for " + path;
    }

}
