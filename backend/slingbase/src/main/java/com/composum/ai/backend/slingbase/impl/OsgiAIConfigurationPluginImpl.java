package com.composum.ai.backend.slingbase.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.AIConfigurationPlugin;

/**
 * The {@code OsgiAIConfigurationPluginImpl} class is the default implementation of the {@link AIConfigurationPlugin} interface.
 * This implementation sources its configurations from the OSGI environment, specifically from instances of {@link OsgiAIConfiguration}.
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
 * @see OsgiAIConfiguration
 */

public class OsgiAIConfigurationPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIConfigurationPluginImpl.class);

    private volatile List<OsgiAIConfiguration> configurations;

    @Activate
    @Modified
    protected void activate(List<OsgiAIConfiguration> configurations) {
        this.configurations = configurations;
        LOG.info("Activated with {} configurations.", configurations.size());
    }

    @Deactivate
    protected void deactivate() {
        this.configurations = null;
        LOG.info("Deactivated.");
    }

    @Override
    @Nonnull
    public Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl) {
        Set<String> allowedServices = new HashSet<>();
        try {
            List<String> userAndgroups = groupsOfUser(request);
            userAndgroups.add(request.getUserPrincipal().getName());
            for (OsgiAIConfiguration config : configurations) {
                // A user is allowed if his username or any of the groups he is in matches the allowedUsers regexes and
                // none of them matches the deniedUsers regexes.
                boolean userAllowed = true;
                boolean userDenied = false;
                for (String userOrGroup : userAndgroups) {
                    userAllowed = userAllowed || matchesAny(userOrGroup, config.allowedUsers());
                    userDenied = userDenied || matchesAny(userOrGroup, config.deniedUsers());
                }
                boolean pathAllowed = matchesAny(contentPath, config.allowedPaths()) && !matchesAny(contentPath, config.deniedPaths());
                boolean viewAllowed = matchesAny(editorUrl, config.allowedViews()) && !matchesAny(editorUrl, config.deniedViews());
                if (userAllowed && !userDenied && pathAllowed && viewAllowed) {
                    allowedServices.addAll(Arrays.asList(config.services()));
                }
            }
        } catch (RepositoryException | RuntimeException e) {
            LOG.error("Error determining allowed services for {} {} {}", request.getRemoteUser(), contentPath, editorUrl, e);
        }
        return allowedServices;
    }

    protected List<String> groupsOfUser(SlingHttpServletRequest request) throws RepositoryException {
        List<String> groupnames = new ArrayList<>();
        UserManager userManager = request.getResourceResolver().adaptTo(UserManager.class);
        Authorizable user = userManager.getAuthorizable(request.getUserPrincipal());
        if (user instanceof User) {
            User userInstance = (User) user;
            Iterator<Group> groups = userInstance.memberOf();
            while (groups.hasNext()) {
                groupnames.add(groups.next().getID());
            }
        }
        return groupnames;
    }

    protected boolean matchesAny(String value, String[] patterns) {
        for (String pattern : patterns) {
            if (value.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

}
