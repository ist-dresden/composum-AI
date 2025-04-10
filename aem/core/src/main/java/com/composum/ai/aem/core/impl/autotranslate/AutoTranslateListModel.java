package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateServiceImpl.MARKER_DEBUG_ADDITIONAL_INSTRUCTIONS;
import static com.composum.ai.backend.base.service.chat.GPTChatCompletionService.MARKER_DEBUG_PRINT_REQUEST;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTBackendsService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateListModel {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateListModel.class);

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @OSGiService
    private AIConfigurationService configurationService;

    @OSGiService
    private AutoTranslateConfigService autoTranslateConfigService;

    @OSGiService
    private LiveRelationshipManager liveRelationshipManager;

    @OSGiService
    private GPTBackendsService backendsService;

    @Self
    private SlingHttpServletRequest request;

    private AutoTranslateService.TranslationRun run;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled() || configurationService == null
                || autoTranslateConfigService == null || !autoTranslateConfigService.isPocUiEnabled();
    }

    @Nonnull
    public List<AutoTranslateService.TranslationRun> getTranslationRuns() {
        return autoTranslateService != null ? autoTranslateService.getTranslationRuns() : Collections.emptyList();
    }

    public boolean inProgress() {
        List<AutoTranslateService.TranslationRun> runs = getTranslationRuns();
        return runs.stream().filter(run -> run.isInProgress()).findAny().isPresent();
    }

    public AutoTranslateService.TranslationRun createRun() throws LoginException, PersistenceException, WCMException {
        if (run == null) {
            String path = request.getParameter("path");
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("path parameter is required");
            }
            path = path.replaceAll("_jcr_content", "jcr:content").replaceAll("\\.html$", "").trim();
            boolean recursive = request.getParameter("recursive") != null;
            boolean copyOriginalPage = request.getParameter("copyOriginalPage") != null;
            String additionalInstructions = request.getParameter("additionalInstructions");
            boolean debugaddinstructions = request.getParameter("debugaddinstructions") != null;
            boolean debugprintrequest = request.getParameter("debugprintrequest") != null;
            if (debugaddinstructions) {
                additionalInstructions = StringUtils.trim(
                        StringUtils.defaultString(additionalInstructions) + "\n\n" +
                                MARKER_DEBUG_ADDITIONAL_INSTRUCTIONS
                );
            }
            if (debugprintrequest) {
                additionalInstructions = StringUtils.trim(
                        StringUtils.defaultString(additionalInstructions) + "\n\n" + MARKER_DEBUG_PRINT_REQUEST);
            }
            AutoTranslateService.TranslationParameters parms = new AutoTranslateService.TranslationParameters();
            String translationmodel = request.getParameter("translationmodel");
            String otherModel = request.getParameter("otherModel");
            if ("otherModel".equals(translationmodel)) {
                translationmodel = otherModel;
            } else if ("default".equals(translationmodel)) {
                translationmodel = null;
            }
            parms.model = defaultIfBlank(translationmodel, null);
            String maxdepth = request.getParameter("maxdepth");
            if (StringUtils.isNotBlank(maxdepth)) {
                parms.maxDepth = Integer.parseInt(maxdepth);
            }
            parms.recursive = recursive;
            parms.additionalInstructions = additionalInstructions;
            if (copyOriginalPage && !debugaddinstructions) {
                copyOriginalPage(request, path);
            }
            run = autoTranslateService.startTranslation(request.getResourceResolver(), path, parms);
        }
        return run;
    }

    /**
     * If parameter copyOriginalPage is set, we create a copy of the original page with this suffix
     * before doing the translation.
     */
    public static final String SUFFIX_TRANSLATECOPY = "_aitranslate_bak";

    /**
     * Make a copy of the original page for comparison purposes.
     */
    protected void copyOriginalPage(SlingHttpServletRequest request, String path) throws WCMException, PersistenceException {
        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page originalPage = pageManager.getContainingPage(path);
        path = originalPage.getPath();
        if (originalPage != null) {
            String newPath = path + SUFFIX_TRANSLATECOPY;
            if (resolver.getResource(newPath) != null) {
                resolver.delete(resolver.getResource(newPath));
            }
            Page copy = pageManager.copy(originalPage, newPath, null, true, true, false);
            if (copy != null) {
                liveRelationshipManager.endRelationship(copy.getContentResource(), true);
                liveRelationshipManager.detach(copy.getContentResource(), true); // end doesn't seem to work
                LOG.info("Created copy of {} at {}", originalPage.getPath(), newPath);
                resolver.commit();
            } else {
                LOG.error("Failed to create copy of {} at {}", originalPage.getPath(), newPath);
                throw new IllegalArgumentException("Failed to create copy of " + originalPage.getPageTitle() + " at " + newPath);
            }
        } else {
            throw new IllegalArgumentException("No page exists at " + path);
        }
    }

    public String rollback() throws WCMException, PersistenceException {
        String path = request.getParameter("path");
        try {
            if (isDisabled()) {
                throw new IllegalStateException("Rollback only allowed in experimental mode.");
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

    public List<String> getAvailableModels() {
        return backendsService.getAllModels();
    }

}
