package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateRunModel {

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @Self
    private SlingHttpServletRequest request;

    public AutoTranslateService.TranslationRun getModel() {
        String id = StringUtils.removeStart(request.getRequestPathInfo().getSuffix(), "/");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id suffix is required");
        }
        return autoTranslateService.getTranslationRuns().stream()
                .filter(run -> id.equals(run.id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such run: " + id));
    }

    public String cancel() {
        AutoTranslateService.TranslationRun run = getModel();
        run.cancel();
        return "cancelled";
    }

}
