package com.composum.ai.backend.slingbase.impl;

import java.io.PrintWriter;
import java.util.Calendar;

import javax.annotation.Nonnull;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;

/**
 * Implements a cache for markdown of pages.
 * We replicate the page resource tree below cacheRootPath (just with sling:Folder) and store the markdown
 * in a property.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Constants.SERVICE_VENDOR + "=IST Gmbh Dresden" +
                        Constants.SERVICE_DESCRIPTION + "=Composum AI: Caching for approximate markdown of pages.",
        })
@Designate(ocd = MarkdownSlingCacheImpl.Config.class)
public class MarkdownSlingCacheImpl implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownSlingCacheImpl.class);

    /**
     * The property in the cache tree where the markdown is stored.
     */
    public static final String PROPERTY_MARKDOWN = "ai_approximateMarkdown";

    /**
     * The page modification date when {@link #PROPERTY_MARKDOWN} was generated. If the actual modification date is different,
     * the cache is removed.
     */
    public static final String PROPERTY_MARKDOWN_PAGE_MODIFICATION_DATE = "ai_approximateMarkdownPageModificationDate";

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    private Config config;

    @Activate
    @Modified
    public void activate(Config config) {
        this.config = config;
    }

    @Deactivate
    public void deactivate() {
        this.config = null;
    }

    protected boolean isEnabled() {
        return config != null && config.cacheRootPath() != null && !config.disabled();
    }

    @Nonnull
    @Override
    public PluginResult maybeHandle(@Nonnull Resource resource, @Nonnull PrintWriter out,
                                    @Nonnull ApproximateMarkdownService service, @Nonnull SlingHttpServletRequest request,
                                    @Nonnull SlingHttpServletResponse response) {
        if (!isEnabled() || resource == null || !resource.getName().equals(JcrConstants.JCR_CONTENT)) {
            return PluginResult.NOT_HANDLED;
        }
        try (ResourceResolver serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            Resource cacheResource = getOrCreateCacheResource(serviceResourceResolver, resource.getPath(), false);
            if (cacheResource != null) {
                ValueMap cacheVM = cacheResource.getValueMap();
                boolean isModified = isModified(resource, cacheVM);
                if (!isModified) {
                    String markdown = cacheVM.get(PROPERTY_MARKDOWN, String.class);
                    if (markdown != null) {
                        out.print(markdown);
                        return PluginResult.HANDLED_ALL;
                    }
                }
            }
        } catch (PersistenceException | RuntimeException | LoginException e) {
            LOG.warn("Could not read cache for {} because of {}", resource.getPath(), e.toString(), e);
        }
        return PluginResult.NOT_HANDLED;
    }

    protected static boolean isModified(@NotNull Resource resource, ValueMap cacheVM) {
        Calendar lastModified = getLastModified(resource);
        Calendar cacheLastModified = cacheVM.get(PROPERTY_MARKDOWN_PAGE_MODIFICATION_DATE, Calendar.class);
        boolean isModified = lastModified != null && cacheLastModified != null && !lastModified.equals(cacheLastModified);
        return isModified;
    }

    @Override
    public void cacheMarkdown(@Nonnull Resource resource, @Nonnull String markdown) {
        if (!isEnabled() || resource == null || markdown == null || markdown.isEmpty() ||
                !resource.getName().equals(JcrConstants.JCR_CONTENT)) {
            return;
        }
        try (ResourceResolver serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            Resource cacheResource = getOrCreateCacheResource(serviceResourceResolver, resource.getPath(), true);
            if (cacheResource != null && (
                    isModified(resource, cacheResource.getValueMap()) ||
                            !markdown.equals(cacheResource.getValueMap().get(PROPERTY_MARKDOWN, String.class)))) {
                Calendar lastModified = getLastModified(resource);
                if (lastModified != null) {
                    ModifiableValueMap mvm = cacheResource.adaptTo(ModifiableValueMap.class);
                    mvm.put(PROPERTY_MARKDOWN_PAGE_MODIFICATION_DATE, lastModified);
                    mvm.put(PROPERTY_MARKDOWN, markdown);
                    cacheResource.getResourceResolver().commit();
                    if (isModified(resource, cacheResource.getValueMap())) {
                        LOG.error("BUG: sanity check failed - shouldn't be in a modified state anymore", resource.getPath());
                    }
                }
            }
        } catch (PersistenceException | LoginException | RuntimeException e) {
            LOG.warn("Could not cache {} because of {}", resource.getPath(), e.toString(), e);
        }
    }

    protected static @Nullable Calendar getLastModified(@NotNull Resource resource) {
        ValueMap origPageVM = resource.getValueMap();
        Calendar lastModified = origPageVM.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        if (lastModified == null) {
            lastModified = origPageVM.get(JcrConstants.JCR_CREATED, Calendar.class);
        }
        return lastModified;
    }

    protected Resource getOrCreateCacheResource(@Nonnull ResourceResolver serviceResolver, @Nonnull String path, boolean createIfNotPresent) throws PersistenceException {
        Resource cacheRoot = serviceResolver.getResource(config.cacheRootPath());
        if (cacheRoot != null) {
            Resource cacheResource = ResourceUtil.getOrCreateResource(serviceResolver, cacheRoot.getPath() + path, "sling:Folder", "sling:Folder", true);
            // add mix:lastModified to mixins to make it easier to track changes, and if that isn't already there
            if (createIfNotPresent && cacheResource != null) {
                ModifiableValueMap mvm = cacheResource.adaptTo(ModifiableValueMap.class);
                if (!cacheResource.isResourceType("mix:lastModified")) {
                    mvm.put("jcr:mixinTypes", new String[]{"mix:lastModified"});
                }
            }
            return cacheResource;
        } else {
            LOG.warn("Configuration error: could not find cache root path {}", config.cacheRootPath());
            return null;
        }
    }

    @ObjectClassDefinition(name = "Composum AI Approximate Markdown Cache Service Configuration",
            description = "If configured, caches the calculated approximate markdown of pages." +
                    "CAUTION: the page content must be independent of the user, or you might leak one users data to another!")
    public @interface Config {

        @AttributeDefinition(name = "Disable", description = "Disable the service", defaultValue = "false")
        boolean disabled() default false;

        @AttributeDefinition(name = "Cache Root Path",
                description = "The JCR root path where the markdown is stored. If not set, no caching is done. Suggestion: /var/composum/ai-markdown-cache. " +
                        "To set this up you'll need to create this path in the repository, " +
                        "add a service user for this bundles name (composum-ai-integration-backend-slingbase) " +
                        "and make the path writeable for this user.",
                required = true
        )
        String cacheRootPath();
    }

}
