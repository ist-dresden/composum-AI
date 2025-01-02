package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateMergeModel {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateMergeModel.class);

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @Self
    private SlingHttpServletRequest request;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled();
    }

    public String getHelloWorld() {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();
        return "Hello World! " + suffix;
    }

}
