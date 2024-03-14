package com.composum.ai.aem.core.impl.autotranslate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.wcm.api.WCMException;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateRunModel {

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @OSGiService
    private AutoTranslateConfigService autoTranslateConfigService;

    @Self
    private SlingHttpServletRequest request;


    public void checkDisabled() {
        if (autoTranslateService == null || !autoTranslateService.isEnabled()
                || autoTranslateConfigService == null || !autoTranslateConfigService.isPocUiEnabled()) {
            throw new IllegalStateException("AutoTranslateService is not available");
        }
    }

    @Nullable
    public AutoTranslateService.TranslationRun getModel() {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();
        String id = StringUtils.removeStart(suffix, "/");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id suffix is required");
        }
        return autoTranslateService.getTranslationRuns().stream()
                .filter(run -> id.equals(run.id))
                .findFirst()
                .orElse(null);
    }

    public String cancel() {
        AutoTranslateService.TranslationRun run = getModel();
        run.cancel();
        return "cancelled";
    }

    public String rollback() throws PersistenceException, WCMException {
        AutoTranslateService.TranslationRun run = getModel();
        run.rollback(AutoTranslateRunModel.this.request.getResourceResolver());
        return "rolled back";
    }

}
