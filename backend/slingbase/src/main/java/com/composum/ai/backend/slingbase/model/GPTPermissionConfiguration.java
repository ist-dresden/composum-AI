package com.composum.ai.backend.slingbase.model;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.composum.ai.backend.slingbase.impl.OsgiAIConfigurationPluginImpl;

/**
 * Serves both as OSGI configuration for {@link OsgiAIConfigurationPluginImpl} as well as
 * Sling Context Aware Configuration, since they both do the same thing, OSGI serving as a fallback
 * or the single global configuration if SlingCAC is not used. In both cases multiple configurations are possible -
 * in the case of OSGI it is used as a configuration factory, in the case of SlingCAC it is used as a collection.
 */
@ObjectClassDefinition(name = "Composum AI Permission Configuration", description = "A configuration for allowed AI services. " +
        "There can be multiple configurations, and the allowed services are aggregated.\n" +
        "There is a fallback configuration that is used if no other configuration is found, " +
        "and a factory for multiple configurations which override the fallback configuration if present. " +
        "If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.")
@Configuration(label = "Composum AI Permission Configuration", description = "A configuration for allowed AI services. " +
        "There can be multiple configurations, and the allowed services are aggregated.",
        collection = true,
        property = {"category=Composum-AI"}
)

public @interface GPTPermissionConfiguration {

    @AttributeDefinition(name = "Services",
            description = "List of services to which this configuration applies. Possible values are: "
                    + GPTPermissionInfo.SERVICE_CATEGORIZE + ", " + GPTPermissionInfo.SERVICE_CREATE
                    + ", " + GPTPermissionInfo.SERVICE_SIDEPANEL + ", " + GPTPermissionInfo.SERVICE_TRANSLATE + " . For AEM only create and sidepanel are supported.")
    @Property(order = 10, label = "Services", description = "List of services to which this configuration applies. Possible values are: "
            + GPTPermissionInfo.SERVICE_CATEGORIZE + ", " + GPTPermissionInfo.SERVICE_CREATE
            + ", " + GPTPermissionInfo.SERVICE_SIDEPANEL + ", " + GPTPermissionInfo.SERVICE_TRANSLATE + " . For AEM only create and sidepanel are supported.",
            property = {
                    "widgetType=dropdown",
                    "dropdownOptions=["
                            + "{'value':'" + GPTPermissionInfo.SERVICE_CREATE + "','description':'Content Creation Dialog'},"
                            + "{'value':'" + GPTPermissionInfo.SERVICE_SIDEPANEL + "','description':'Side Panel AI'}"
                            + "]"
            }
    )
    String[] services() default {
            GPTPermissionInfo.SERVICE_CATEGORIZE, GPTPermissionInfo.SERVICE_CREATE,
            GPTPermissionInfo.SERVICE_SIDEPANEL, GPTPermissionInfo.SERVICE_TRANSLATE
    };

    @AttributeDefinition(name = "Allowed Users",
            description = "Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration.")
    @Property(order = 20, label = "Allowed Users",
            description = "Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration.")
    String[] allowedUsers() default {".*"};

    @AttributeDefinition(name = "Denied Users",
            description = "Regular expressions for denied users or user groups. Takes precedence over allowed users.")
    @Property(order = 30, label = "Denied Users",
            description = "Regular expressions for denied users or user groups. Takes precedence over allowed users.")
    String[] deniedUsers() default {};

    @AttributeDefinition(name = "Allowed Paths",
            description = "Regular expressions for allowed content paths. If not present, no paths are allowed.")
    @Property(order = 40, label = "Allowed Paths",
            description = "Regular expressions for allowed content paths. If not present, no paths are allowed.")
    String[] allowedPaths() default {"/content/.*"};

    @AttributeDefinition(name = "Denied Paths",
            description = "Regular expressions for denied content paths. Takes precedence over allowed paths.")
    @Property(order = 50, label = "Denied Paths",
            description = "Regular expressions for denied content paths. Takes precedence over allowed paths.")
    // content fragments are not allowed by default since there has been trouble in the UI
    String[] deniedPaths() default {"/content/dam/.*"};

    @AttributeDefinition(name = "Allowed Views",
            description = "Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views.")
    @Property(order = 60, label = "Allowed Views",
            description = "Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views.")
    String[] allowedViews() default {".*"};

    @AttributeDefinition(name = "Denied Views",
            description = "Regular expressions for denied views. Takes precedence over allowed views.")
    @Property(order = 70, label = "Denied Views",
            description = "Regular expressions for denied views. Takes precedence over allowed views.")
    String[] deniedViews() default {};

    @AttributeDefinition(name = "Allowed Components", description = "Regular expressions for allowed resource types of components. If not present, no components are allowed.")
    @Property(order = 80, label = "Allowed Components",
            description = "Regular expressions for allowed resource types of components. If not present, no components are allowed.")
    String[] allowedComponents() default {".*"};

    @AttributeDefinition(name = "Denied Components", description = "Regular expressions for denied resource types of components. Takes precedence over allowed components.")
    @Property(order = 90, label = "Denied Components",
            description = "Regular expressions for denied resource types of components. Takes precedence over allowed components.")
    String[] deniedComponents() default {};

    // allowed and denied page templates . name: allowedPageTemplates, deniedPageTemplates . default: all allowed, none denied
    @AttributeDefinition(name = "Allowed Page Templates", description = "Regular expressions for allowed page templates. If not present, all page templates are allowed.")
    @Property(order = 100, label = "Allowed Page Templates",
            description = "Regular expressions for allowed page templates. If not present, all page templates are allowed.")
    String[] allowedPageTemplates() default {".*"};

    @AttributeDefinition(name = "Denied Page Templates", description = "Regular expressions for denied page templates. Takes precedence over allowed page templates.")
    @Property(order = 110, label = "Denied Page Templates",
            description = "Regular expressions for denied page templates. Takes precedence over allowed page templates.")
    String[] deniedPageTemplates() default {};

}
