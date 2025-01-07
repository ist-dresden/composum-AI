package com.composum.ai.aem.core.impl.autotranslate;

import java.util.List;
import java.util.Locale;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.SelectorUtils;
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

    private transient Resource pageResource;

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled();
    }

    public List<AutoTranslateMergeService.AutoTranslateProperty> getProperties() {
        return autoTranslateMergeService.getProperties(getPageResource());
    }

    public String getPageLanguage() {
        String language = SelectorUtils.findLanguage(getPageResource());
        return language != null ? SelectorUtils.getLanguageName(language, Locale.ENGLISH) : null;
    }

    /**
     * Finds the content resource for the page that is in the request suffix.
     */
    protected Resource getPageResource() {
        if (pageResource == null && !isDisabled()) {
            ResourceResolver resolver = request.getResourceResolver();
            PageManager pageManager = resolver.adaptTo(PageManager.class);

            RequestPathInfo requestPathInfo = request.getRequestPathInfo();
            String suffix = requestPathInfo.getSuffix();
            if (suffix != null) {
                pageResource = pageManager.getContainingPage(suffix).getContentResource();
            }
        }
        return pageResource;
    }

    public String getPagePath() {
        return getPageResource() != null ? getPageResource().getParent().getPath() : null;
    }

}
