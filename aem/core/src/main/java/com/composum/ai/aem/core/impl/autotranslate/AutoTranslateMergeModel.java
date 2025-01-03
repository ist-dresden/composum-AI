package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.PageManager;

@Model(adaptables = SlingHttpServletRequest.class)
public class AutoTranslateMergeModel {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateMergeModel.class);

    @OSGiService
    private AutoTranslateService autoTranslateService;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private AutoTranslateMergeService autoTranslateMergeService;


    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled();
    }

    public List<AITranslatePropertyWrapper> getProperties() {
        Resource resource = getPageResource();
        return autoTranslateMergeService.getProperties(resource);
    }

    /**
     * Finds the content resource for the page that is in the request suffix.
     */
    protected Resource getPageResource() {
        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);

        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();
        if (suffix != null) {
            return pageManager.getContainingPage(suffix).getContentResource();
        }
        return null;
    }
}
