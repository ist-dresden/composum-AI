package com.composum.ai.backend.slingbase;

import java.io.PrintWriter;

import javax.annotation.Nonnull;

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
    PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out, @Nonnull ApproximateMarkdownService service);

}
