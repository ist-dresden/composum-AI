package com.composum.ai.backend.slingbase.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.composum.ai.backend.slingbase.AIConfigurationServlet;

@ObjectClassDefinition(name = "Composum AI Configuration", description = "Configuration for allowed AI services based on OSGI settings.")
public @interface OsgiAIConfiguration {

    @AttributeDefinition(name = "Services",
            description = "List of services to which this configuration applies. Possible values are: "
                    + AIConfigurationServlet.SERVICE_CATEGORIZE + ", " + AIConfigurationServlet.SERVICE_CREATE
                    + ", " + AIConfigurationServlet.SERVICE_SIDEPANEL + ", " + AIConfigurationServlet.SERVICE_TRANSLATE + " .")
    String[] services() default {};

    @AttributeDefinition(name = "Allowed Users",
            description = "Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration.")
    String[] allowedUsers() default {};

    @AttributeDefinition(name = "Denied Users",
            description = "Regular expressions for denied users or user groups. Takes precedence over allowed users.")
    String[] deniedUsers() default {};

    @AttributeDefinition(name = "Allowed Paths",
            description = "Regular expressions for allowed content paths. If not present, no paths are allowed.")
    String[] allowedPaths() default {};

    @AttributeDefinition(name = "Denied Paths",
            description = "Regular expressions for denied content paths. Takes precedence over allowed paths.")
    String[] deniedPaths() default {};

    @AttributeDefinition(name = "Allowed Views",
            description = "Regular expressions for allowed views. If not present, no views are allowed.")
    String[] allowedViews() default {};

    @AttributeDefinition(name = "Denied Views",
            description = "Regular expressions for denied views. Takes precedence over allowed views.")
    String[] deniedViews() default {};

}
