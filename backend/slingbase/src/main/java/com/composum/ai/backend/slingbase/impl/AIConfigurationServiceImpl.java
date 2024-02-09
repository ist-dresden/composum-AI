package com.composum.ai.backend.slingbase.impl;

import static com.composum.ai.backend.slingbase.impl.AllowDenyMatcherUtil.matchesAny;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationPlugin;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration;
import com.composum.ai.backend.slingbase.model.GPTPermissionInfo;
import com.composum.ai.backend.slingbase.model.GPTPromptLibrary;

/**
 * Collects the configurations from {@link AIConfigurationPlugin}s and aggregates them.
 *
 * <p>
 * The primary responsibility of this class is to determine which AI services are allowed based on various parameters such as:
 * <ul>
 *     <li>The user or user group making the request.</li>
 *     <li>The content path being accessed or edited.</li>
 *     <li>The URL of the editor in the browser.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The configurations are defined as OSGI configurations and can be dynamically modified at runtime. Each configuration specifies:
 * <ul>
 *     <li>Allowed and denied users or user groups.</li>
 *     <li>Allowed and denied content paths.</li>
 *     <li>Allowed and denied views (based on the URL).</li>
 *     <li>The specific AI services that the configuration applies to.</li>
 * </ul>
 * </p>
 *
 * <p>
 * When determining the allowed services, this implementation checks all the available configurations and aggregates the results.
 * A service is considered allowed if it matches any of the "allowed" regular expressions and does not match any of the "denied" regular expressions.
 * </p>
 *
 * @see AIConfigurationPlugin
 * @see GPTPermissionConfiguration
 */
@Component(service = AIConfigurationService.class)
public class AIConfigurationServiceImpl implements AIConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(AIConfigurationServiceImpl.class);

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected volatile List<AIConfigurationPlugin> plugins;

    @Reference
    protected GPTChatCompletionService chatCompletionService;

    /**
     * Union of the plugin's results.
     */
    @Override
    @Nullable
    public GPTPermissionInfo allowedServices(@Nonnull SlingHttpServletRequest request, @Nonnull String contentPath, @Nonnull String editorUrl) {
        GPTConfiguration gptConfiguration = getGPTConfiguration(request, contentPath);
        GPTPermissionInfo result = null;
        if (chatCompletionService.isEnabled(gptConfiguration)) {
            for (AIConfigurationPlugin plugin : plugins) {
                try {
                    List<GPTPermissionConfiguration> configs = plugin.allowedServices(request, contentPath);
                    if (configs != null) {
                        for (GPTPermissionConfiguration config : configs) {
                            if (basicCheck(config, request, contentPath, editorUrl)) {
                                GPTPermissionInfo permissionInfo = GPTPermissionInfo.from(config);
                                result = GPTPermissionInfo.mergeAdditively(result, permissionInfo);
                                if (configs != null) {
                                    LOG.info("Plugin {} allowed services {}", plugin.getClass(), permissionInfo);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error in AIConfigurationPlugin with {}", plugin.getClass(), e);
                }
            }
        }
        return result;
    }

    /**
     * Determines whether the configuration allows access wrt. user, page and view
     */
    protected boolean basicCheck(@Nonnull GPTPermissionConfiguration config, SlingHttpServletRequest request, @Nonnull String contentPath, @Nonnull String editorUrl) {
        boolean allowed = false;
        try {
            List<String> userAndGroups = AllowDenyMatcherUtil.userAndGroupsOfUser(request);
            // A user is allowed if his username or any of the groups he is in matches the allowedUsers regexes and
            // none of them matches the deniedUsers regexes.
            boolean userAllowed = false;
            boolean userDenied = false;
            for (String userOrGroup : userAndGroups) {
                userAllowed = userAllowed || matchesAny(userOrGroup, config.allowedUsers());
                userDenied = userDenied || matchesAny(userOrGroup, config.deniedUsers());
            }
            boolean pathAllowed = matchesAny(contentPath, config.allowedPaths()) && !matchesAny(contentPath, config.deniedPaths());
            boolean viewAllowed = matchesAny(editorUrl, config.allowedViews()) && !matchesAny(editorUrl, config.deniedViews());
            allowed = userAllowed && !userDenied && pathAllowed && viewAllowed;
            allowed = allowed && pageAllowed(request, contentPath, config);
        } catch (RepositoryException | RuntimeException e) {
            LOG.error("Error determining allowed services for {} {} {}", request.getRemoteUser(), contentPath, editorUrl, e);
        }
        return allowed;
    }

    protected boolean pageAllowed(SlingHttpServletRequest request, String contentPath, GPTPermissionConfiguration config) {
        Resource resource = request.getResourceResolver().getResource(contentPath);
        if (resource == null) {
            LOG.warn("Resource {} not found", contentPath);
            return false;
        }
        // go to next transitive parent jcr:content node - the page containing the component
        Resource page = resource;
        if (page.getChild(JcrConstants.JCR_CONTENT) != null) {
            page = page.getChild(JcrConstants.JCR_CONTENT);
        }
        while (page != null && !JcrConstants.JCR_CONTENT.equals(page.getName())) {
            page = page.getParent();
        }
        if (page == null) {
            LOG.warn("No page found for resource {}", resource.getPath());
            return false;
        }
        ValueMap valueMap = page.getValueMap();
        String template = valueMap.get("cq:template", String.class); // AEM
        if (template == null) {
            template = valueMap.get("template", String.class); // Composum
        }
        if (template == null) { // for content fragments we use the cq:model
            template = valueMap.get("data/cq:model", String.class);
        }
        return matchesAny(template, config.allowedPageTemplates()) && !matchesAny(template, config.deniedPageTemplates());
    }


    @Override
    public GPTConfiguration getGPTConfiguration(@NotNull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException {
        for (AIConfigurationPlugin plugin : plugins) {
            try {
                GPTConfiguration configuration = plugin.getGPTConfiguration(request, contentPath);
                if (configuration != null) {
                    LOG.info("Plugin {} returned configuration {}", plugin.getClass(), configuration);
                    return configuration;
                }
            } catch (Exception e) {
                LOG.error("Error in AIConfigurationPlugin with {}", plugin.getClass(), e);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public GPTPromptLibrary getGPTPromptLibraryPaths(@NotNull SlingHttpServletRequest request, @Nullable String contentPath) throws IllegalArgumentException {
        for (AIConfigurationPlugin plugin : plugins) {
            try {
                GPTPromptLibrary promptLibrary = plugin.getGPTPromptLibraryPaths(request, contentPath);
                if (promptLibrary != null) {
                    LOG.info("Plugin {} returned prompt library {}", plugin.getClass(), promptLibrary);
                    return promptLibrary;
                }
            } catch (Exception e) {
                LOG.error("Error in AIConfigurationPlugin with {}", plugin.getClass(), e);
            }
        }
        return null;
    }
}
