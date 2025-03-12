package com.composum.ai.aem.core.impl.autotranslate;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

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

    /**
     * Parameter to show properties wrt. cancellation status.
     */
    protected static final String PARAM_PROPERTY_FILTER = "propertyfilter";

    /**
     * Parameter to show properties wrt. needed actions.
     */
    protected static final String PARAM_SCOPE = "scope";

    /**
     * Parameter that limits output to just that component - for specific reloading.
     */
    protected static final String PARAM_COMPONENTPATH = "componentpath";

    /**
     * Parameter that limits output to just that property - for specific reloading.
     *
     * @see #PARAM_COMPONENTPATH
     */
    protected static final String PARAM_PROPERTYNAME = "propertyname";

    protected PropertyFilter getPropertyFilter(SlingHttpServletRequest request) {
        return PropertyFilter.fromValue(request.getParameter(PARAM_PROPERTY_FILTER));
    }

    protected Scope getScope(SlingHttpServletRequest request) {
        return Scope.fromValue(request.getParameter(PARAM_SCOPE));
    }

    public boolean isDisabled() {
        return autoTranslateService == null || !autoTranslateService.isEnabled();
    }

    public List<AutoTranslateMergeService.AutoTranslateProperty> getProperties() {
        List<AutoTranslateMergeService.AutoTranslateProperty> properties = autoTranslateMergeService.getProperties(getPageResource());
        PropertyFilter propertyFilter = getPropertyFilter(request);
        Scope scope = getScope(request);
        switch (propertyFilter) {
            case INHERITANCE_CANCELLED:
                properties.removeIf(property -> !property.isCancelled());
                break;
            case INHERITANCE_ENABLED:
                properties.removeIf(AutoTranslateMergeService.AutoTranslateProperty::isCancelled);
                break;
            case ALL_PROPERTIES:
                break;
        }
        switch (scope) {
            case UNFINISHED_PROPERTIES:
                properties.removeIf(p -> !p.isProcessingNeeded());
                break;
            case ALL_PROPERTIES:
                break;
        }
        String componentPath = request.getParameter(PARAM_COMPONENTPATH);
        if (StringUtils.isNotBlank(componentPath)) {
            properties.removeIf(p -> !p.getComponentPath().equals(componentPath));
        }
        String propertyname = request.getParameter(PARAM_PROPERTYNAME);
        if (StringUtils.isNotBlank(propertyname)) {
            properties.removeIf(p -> !p.getWrapper().getPropertyName().equals(propertyname));
        }
        return properties;
    }

    public List<AutoTranslateComponent> getPageComponents() {
        ArrayList<AutoTranslateComponent> pageComponents = new ArrayList<>();
        Map<String, AutoTranslateComponent> components = new LinkedHashMap<>();
        List<AutoTranslateMergeService.AutoTranslateProperty> properties = getProperties();
        for (AutoTranslateMergeService.AutoTranslateProperty property : properties) {
            AutoTranslateComponent component = components.get(property.getComponentPath());
            if (this.pageResource.getPath().equals(property.getComponentPath())) {
                // all page properties are individually cancellable -> save them individually
                component = new AutoTranslateComponent(property.getComponentPath());
                pageComponents.add(component);
            } else if (component == null) {
                component = new AutoTranslateComponent(property.getComponentPath());
                components.put(property.getComponentPath(), component);
                pageComponents.add(component);
            }
            component.getCheckableProperties().add(property);
        }
        return pageComponents;
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

        public String getComponentPath() {
            return componentPath;
        }

        /**
         * For pages properties are cancelled individually. If it's a page we return the property name here, else nothing.
         */
        public String getCancelPropertyName() {
            if (componentPath.endsWith("/jcr:content")) {
                return properties.get(0).getWrapper().getPropertyName();
            }
            return null;
        }

        public String getComponentPathInPage() {
            return substringAfter(componentPath, "/jcr:content/");
        }

        public String getLinkToComponent() {
            if (componentPath.endsWith("/jcr:content")) {
                return "/mnt/overlay/wcm/core/content/sites/properties.html?item=" +
                        substringBefore(componentPath, "/jcr:content");
            }
            return "/editor.html" + substringBefore(componentPath, "/jcr:content") +
                    ".html#scrolltocomponent-" + substringAfter(componentPath, "/jcr:content/");
        }

        public List<AutoTranslateMergeService.AutoTranslateProperty> getCheckableProperties() {
            return properties;
        }

        public boolean isCancelled() {
            return properties.get(0).isCancelled();
        }

        public String cancelledClass() {
            return properties.get(0).cancelledClass();
        }

        /**
         * Size of {@link #getCheckableProperties()} times 3 + 1 since HTL cannot calculate :-(
         */
        public int getCalculatedComponentRowspan() {
            return properties.size() * 3 + 1;
        }

        @Override
        public String toString() {
            return "AutoTranslateComponent(" + componentPath + ')';
        }
    }


    public enum PropertyFilter {
        ALL_PROPERTIES("allstati"),
        INHERITANCE_CANCELLED("cancelled"),
        INHERITANCE_ENABLED("inheritanceenabled");

        private final String value;

        PropertyFilter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static PropertyFilter fromValue(String value) {
            if (value == null) {
                return PropertyFilter.ALL_PROPERTIES;
            }
            for (PropertyFilter filter : values()) {
                if (filter.value.equals(value)) {
                    return filter;
                }
            }
            return null;
        }
    }

    public enum Scope {
        UNFINISHED_PROPERTIES("unfinished"),
        ALL_PROPERTIES("allprops");

        private final String value;

        Scope(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Scope fromValue(String value) {
            if (value == null) {
                return UNFINISHED_PROPERTIES;
            }
            for (Scope scope : values()) {
                if (scope.value.equals(value)) {
                    return scope;
                }
            }
            return null;
        }
    }

}
