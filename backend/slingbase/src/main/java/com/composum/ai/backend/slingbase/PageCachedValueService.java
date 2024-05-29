package com.composum.ai.backend.slingbase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;

/**
 * Service to cache some values that depend on a page content but are computationally intensive to calculate,
 * like the markdown representation or embeddings or a machine generated summary.
 * The values are not stored in the page itself but in a separate tree at a location that is configurable.
 */
public interface PageCachedValueService {

    /**
     * Writes a value that should be cached with that property name to the cache.
     */
    void putPageCachedValue(@Nonnull String propertyName, @Nonnull Resource resource, @Nonnull String value);

    /**
     * Reads a value that was cached with that property name from the cache.
     */
    @Nullable
    String getPageCachedValue(@Nonnull String propertyName, @Nonnull Resource resource);

}
