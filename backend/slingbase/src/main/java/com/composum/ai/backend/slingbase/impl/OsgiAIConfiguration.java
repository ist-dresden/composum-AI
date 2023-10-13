package com.composum.ai.backend.slingbase.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.composum.ai.backend.slingbase.AIConfigurationServlet;

@ObjectClassDefinition(name = "Composum AI Configuration", description = "A configuration for allowed AI services. " +
        "There can be multiple configurations, and the allowed services are aggregated.")
public @interface OsgiAIConfiguration {

    @AttributeDefinition(name = "Services",
            description = "List of services to which this configuration applies. Possible values are: "
                    + AIConfigurationServlet.SERVICE_CATEGORIZE + ", " + AIConfigurationServlet.SERVICE_CREATE
                    + ", " + AIConfigurationServlet.SERVICE_SIDEPANEL + ", " + AIConfigurationServlet.SERVICE_TRANSLATE + " . For AEM only create and sidepanel are supported.")
    String[] services() default {
            AIConfigurationServlet.SERVICE_CATEGORIZE, AIConfigurationServlet.SERVICE_CREATE,
            AIConfigurationServlet.SERVICE_SIDEPANEL, AIConfigurationServlet.SERVICE_TRANSLATE
    };

    @AttributeDefinition(name = "Allowed Users",
            description = "Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration.")
    String[] allowedUsers() default {".*"};

    @AttributeDefinition(name = "Denied Users",
            description = "Regular expressions for denied users or user groups. Takes precedence over allowed users.")
    String[] deniedUsers() default {};

    @AttributeDefinition(name = "Allowed Paths",
            description = "Regular expressions for allowed content paths. If not present, no paths are allowed.")
    String[] allowedPaths() default {"/content/.*"};

    @AttributeDefinition(name = "Denied Paths",
            description = "Regular expressions for denied content paths. Takes precedence over allowed paths.")
    // content fragments are not allowed by default since there has been trouble in the UI
    String[] deniedPaths() default {"/content/dam/.*"};

    @AttributeDefinition(name = "Allowed Views",
            description = "Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views.")
    String[] allowedViews() default {".*"};

    @AttributeDefinition(name = "Denied Views",
            description = "Regular expressions for denied views. Takes precedence over allowed views.")
    String[] deniedViews() default {};

}
