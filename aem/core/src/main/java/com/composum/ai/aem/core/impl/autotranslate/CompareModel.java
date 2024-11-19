package com.composum.ai.aem.core.impl.autotranslate;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * Model for comparing a page with its live relationship source.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class CompareModel {

    /**
     * Parameter with path to a page / page resource for which we compare live relationship source and the containing page.
     */
    public static final String PARAM_PATH = "path";

    @OSGiService
    private LiveRelationshipManager liveRelationshipManager;

    @Self
    private SlingHttpServletRequest request;

    private String url1;
    private String url2;

    @PostConstruct
    public void init() throws WCMException {
        String path = request.getParameter(PARAM_PATH);
        if (path == null || path.isEmpty()) {
            path = request.getRequestPathInfo().getSuffix();
        }
        if (path != null && !path.isEmpty()) {
            ResourceResolver resolver = request.getResourceResolver();
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            Page page = pageManager.getContainingPage(path);
            if (page != null) {
                url1 = page.getPath() + ".html";
                LiveRelationship relationship = liveRelationshipManager.getLiveRelationship(page.getContentResource(), false);
                if (relationship != null) {
                    Page target = pageManager.getContainingPage(relationship.getSourcePath());
                    if (target != null) {
                        url2 = target.getPath() + ".html";
                    }
                }
            }
        }
    }

    /**
     * If the request contained a valid path as {@value PARAM_PATH} or suffix, this is the URL of the page.
     */
    public String getUrl1() {
        return url1;
    }

    /**
     * If the request contained a valid path as {@value PARAM_PATH} or suffix, this is the URL of the page that is the source of the live relationship.
     */
    public String getUrl2() {
        return url2;
    }

}
