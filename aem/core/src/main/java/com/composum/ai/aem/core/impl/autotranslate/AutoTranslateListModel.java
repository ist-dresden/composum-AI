package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateListModel {

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @Self
    private SlingHttpServletRequest request;

    public List<AutoTranslateService.TranslationRun> getTranslationRuns() {
        return autoTranslateService.getTranslationRuns();
    }

    public AutoTranslateService.TranslationRun createRun() throws LoginException, PersistenceException {
        String path = request.getParameter("path");
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        boolean recursive = request.getParameter("recursive") != null;
        return autoTranslateService.startTranslation(request.getResourceResolver(), path, recursive);
    }

}
