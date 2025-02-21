package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

    public List<AutoTranslateComponent> getPageComponents() {
        Map<String, AutoTranslateComponent> components = new LinkedHashMap<>();
        for (AutoTranslateMergeService.AutoTranslateProperty property : getProperties()) {
            AutoTranslateComponent component = components.get(property.getComponentPath());
            if (component == null) {
                component = new AutoTranslateComponent(property.getComponentPath());
                components.put(component.getComponentPathInPage(), component);
            }
            component.getCheckableProperties().add(property);
        }
        return new ArrayList<>(components.values());
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

    public static class AutoTranslateComponent {
        private final String componentPath;
        private final List<AutoTranslateMergeService.AutoTranslateProperty> properties = new ArrayList<>();

        public AutoTranslateComponent(String componentPath) {
            this.componentPath = componentPath;
        }

        public String getComponentName() {
            return properties.isEmpty() ? null : properties.get(0).getComponentName();
        }

        public String getComponentTitle() {
            return properties.isEmpty() ? null : properties.get(0).getComponentTitle();
        }

        public String getComponentPathInPage() {
            return StringUtils.substringAfter(componentPath, "/jcr:content/");
        }

        public List<AutoTranslateMergeService.AutoTranslateProperty> getCheckableProperties() {
            return properties;
        }

        /** Size of {@link #getCheckableProperties()} times 3 + 1 since HTL cannot calculate :-( */
        public int getCalculatedComponentRowspan() {
            return properties.size() * 3 + 1;
        }

    }

}
