package com.composum.ai.backend.slingbase.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.composum.ai.backend.slingbase.impl.AllowDenyMatcherUtil;

/**
 * Permission information that can be used to determine whether a page or component permits services as the side panel
 * AI or the content creation assistant. Is used as a JSON return object - thus we are mutable and have getters / setters.
 */
public class GPTPermissionInfo {

    /**
     * Service name for Content Creation Dialog
     */
    public static final String SERVICE_CREATE = "create";
    /**
     * Service name for Side Panel AI
     */
    public static final String SERVICE_SIDEPANEL = "sidepanel";
    /**
     * Only for Composum: service name for translation.
     */
    public static final String SERVICE_TRANSLATE = "translate";
    /**
     * Only for composum: service name for categorization.
     */
    public static final String SERVICE_CATEGORIZE = "categorize";

    private List<GPTPermissionInfoItem> servicePermissions = new ArrayList<>();

    /**
     * List of service permissions.
     */
    public List<GPTPermissionInfoItem> getServicePermissions() {
        return servicePermissions;
    }

    public void setServicePermissions(List<GPTPermissionInfoItem> servicePermissions) {
        this.servicePermissions = servicePermissions;
    }

    /**
     * Checks whether this allows the given service for the given resourceType
     */
    public boolean allows(@Nullable String service, @Nullable String resourceType) {
        if (service == null || resourceType == null || servicePermissions == null || servicePermissions.isEmpty()) {
            return false;
        }
        for (GPTPermissionInfoItem servicePermission : servicePermissions) {
            if (servicePermission != null && servicePermission.allows(service, resourceType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Joins the permission informations additively.
     */
    @Nullable
    public static GPTPermissionInfo mergeAdditively(@Nullable GPTPermissionInfo info1, @Nullable GPTPermissionInfo info2) {
        if (info1 == null) {
            return info2;
        } else if (info2 == null) {
            return info1;
        }
        GPTPermissionInfo result = new GPTPermissionInfo();
        result.getServicePermissions().addAll(info1.getServicePermissions());
        result.getServicePermissions().addAll(info2.getServicePermissions());
        return result;
    }

    /**
     * One set of permissions.
     */
    public static class GPTPermissionInfoItem {

        private List<String> services = Collections.emptyList();

        private List<String> allowedComponents = Collections.emptyList();

        private List<String> deniedComponents = Collections.emptyList();

        /**
         * The name of the service this applies to - one of the SERVICE_ constants.
         */
        public List<String> getServices() {
            return services;
        }

        public void setService(List<String> service) {
            this.services = service;
        }

        /**
         * Regular expressions for allowed components. If not present, no components are allowed.
         */
        public List<String> getAllowedComponents() {
            return allowedComponents;
        }

        public void setAllowedComponents(List<String> allowedComponents) {
            this.allowedComponents = allowedComponents;
        }

        /**
         * Regular expressions for denied components. Takes precedence over allowed components.
         */
        public List<String> getDeniedComponents() {
            return deniedComponents;
        }

        public void setDeniedComponents(List<String> deniedComponents) {
            this.deniedComponents = deniedComponents;
        }

        @Override
        public String toString() {
            return "GPTPermissionInfoItem{" +
                    "services=" + services +
                    ", allowedComponents=" + allowedComponents +
                    ", deniedComponents=" + deniedComponents +
                    '}';
        }

        public boolean allows(String service, String resourceType) {
            if (services == null || !services.contains(service)) {
                return false;
            }
            boolean allowed = AllowDenyMatcherUtil.matchesAny(resourceType, getAllowedComponents());
            boolean denied = allowed && AllowDenyMatcherUtil.matchesAny(resourceType, getDeniedComponents());
            return allowed && !denied;
        }

        /**
         * Reads out the services and component regex information, the other stuff has to be checked elsewhere.
         */
        @Nonnull
        public static GPTPermissionInfoItem from(@Nonnull GPTPermissionConfiguration config) {
            if (config == null) {
                return null;
            }
            GPTPermissionInfoItem result = new GPTPermissionInfoItem();
            result.setService(Arrays.asList(config.services()));
            result.setAllowedComponents(
                    config.allowedComponents() != null ?
                            Arrays.asList(config.allowedComponents()) : Collections.emptyList());
            result.setDeniedComponents(
                    config.deniedComponents() != null ?
                            Arrays.asList(config.deniedComponents()) : Collections.emptyList());
            return result;
        }

    }

    @Override
    public String toString() {
        return "GPTPermissionInfo{" +
                "servicePermissions=" + servicePermissions +
                '}';
    }

    @Nullable
    public static GPTPermissionInfo from(@Nullable GPTPermissionConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        GPTPermissionInfo result = new GPTPermissionInfo();
        result.setServicePermissions(Arrays.asList(GPTPermissionInfoItem.from(configuration)));
        return result;
    }

}
