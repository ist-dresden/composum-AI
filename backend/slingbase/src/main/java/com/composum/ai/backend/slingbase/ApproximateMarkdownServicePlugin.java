package com.composum.ai.backend.slingbase;

import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

/**
 * Implements special treatment for some resources, depending on the actual system.
 */
public interface ApproximateMarkdownServicePlugin {

    /**
     * Result of the plugin execution.
     */
    enum PluginResult {
        NOT_HANDLED,
        /**
         * The plugin handled the current resource and it's children - no need to continue.
         */
        HANDLED_ALL,
        /**
         * The plugin handled the attributes of the resource, but not it's children.
         */
        HANDLED_ATTRIBUTES
    }

    /**
     * Checks whether the resource should be handled by this plugin and if so, handles it by printing an appropriate markdown representation to the PrintWriter.
     *
     * @return what is already handled by this plugin. It is possible to write to the PrintWriter in any case.
     */
    @Nonnull
    PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out,
                             @Nonnull ApproximateMarkdownService service,
                             @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response);

    /**
     * Retrieves the imageURL in a way useable for ChatGPT - usually data:image/jpeg;base64,{base64_image}
     * If the plugin cannot handle this resource, it should return null.
     */
    @Nullable
    String getImageUrl(@Nullable Resource imageResource);

    /**
     * Returns true when the sling:resourceType or one of the sling:resourceSuperType of the sling:resourceType match the pattern.
     * Useable to check whether a resource is rendered with a derivation of a certain component.
     */
    default boolean resourceRendersAsComponentMatching(@Nonnull Resource resource, @Nonnull Pattern pattern) {
        String resourceType = resource.getResourceType();
        if (pattern.matcher(resourceType).matches()) {
            return true;
        }
        Resource component = resource.getResourceResolver().getResource(resourceType);
        while (component != null) {
            String supertype = component.getResourceSuperType();
            if (supertype != null && pattern.matcher(supertype).matches()) {
                return true;
            }
            component = component.getResourceResolver().getResource(supertype);
        }
        return false;
    }

}
