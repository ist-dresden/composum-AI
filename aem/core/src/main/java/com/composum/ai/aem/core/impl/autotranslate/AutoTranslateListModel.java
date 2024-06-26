package com.composum.ai.aem.core.impl.autotranslate;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateListModel {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateListModel.class);

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @OSGiService
    private AIConfigurationService configurationService;

    @OSGiService
    private AutoTranslateConfigService autoTranslateConfigService;

    @Self
    private SlingHttpServletRequest request;

    private AutoTranslateService.TranslationRun run;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled() || configurationService == null
                || autoTranslateConfigService == null || !autoTranslateConfigService.isPocUiEnabled();
    }

    public List<AutoTranslateService.TranslationRun> getTranslationRuns() {
        return autoTranslateService != null ? autoTranslateService.getTranslationRuns() : Collections.emptyList();
    }

    public AutoTranslateService.TranslationRun createRun() throws LoginException, PersistenceException {
        if (isDisabled()) {
            throw new IllegalStateException("AutoTranslateService is not available");
        }
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
            AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
            String translationmodel = request.getParameter("translationmodel");
            if ("standard".equals(translationmodel)) {
                parms.preferStandardModel = true;
            } else if ("highintelligence".equals(translationmodel)) {
                parms.preferHighIntelligenceModel = true;
            } else {
                parms.preferHighIntelligenceModel = autoTranslateConfigService.isUseHighIntelligenceModel();
            }
            String maxdepth = request.getParameter("maxdepth");
            if (StringUtils.isNotBlank(maxdepth)) {
                parms.maxDepth = Integer.parseInt(maxdepth);
            }
            parms.recursive = recursive;
            parms.translateWhenChanged = changed;
            parms.additionalInstructions = additionalInstructions;
            parms.breakInheritance = breakInheritance;
            run = autoTranslateService.startTranslation(request.getResourceResolver(), path, parms);
        }
        return run;
    }

    public String rollback() throws WCMException, PersistenceException {
        String path = request.getParameter("path");
        try {
            if (isDisabled()) {
                throw new IllegalStateException("AutoTranslateService is not available");
            }
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("path parameter is required");
            }
            path = path.replaceAll("_jcr_content", "jcr:content").trim();
            Resource resource = request.getResourceResolver().getResource(path);
            if (resource == null) {
                return "CAUTION: resource not found: " + path;
            }
            autoTranslateService.rollback(resource);
            request.getResourceResolver().commit();
            return "rolled back for " + path;
        } catch (Exception e) {
            LOG.error("rollback failed for " + path, e);
            return "CAUTION: rollback failed for " + path + ": " + e.getMessage();
        }
    }

}
