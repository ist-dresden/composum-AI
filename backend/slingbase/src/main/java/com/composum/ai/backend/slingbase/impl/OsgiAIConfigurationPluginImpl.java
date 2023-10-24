package com.composum.ai.backend.slingbase.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
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
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=2000"
)
@Designate(ocd = OsgiAIConfiguration.class, factory = true)
public class OsgiAIConfigurationPluginImpl implements AIConfigurationPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiAIConfigurationPluginImpl.class);

    private OsgiAIConfiguration config;

    @Activate
    @Modified
    protected void activate(OsgiAIConfiguration configuration) {
        this.config = configuration;
        LOG.info("Activated with configuration {}", configuration);
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
        LOG.info("Deactivated.");
    }

    @Override
    @Nullable
    public Set<String> allowedServices(SlingHttpServletRequest request, String contentPath, String editorUrl) {
        Set<String> allowedServices = new HashSet<>();
        try {
            List<String> userAndGroups = userAndGroupsOfUser(request);
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
            if (userAllowed && !userDenied && pathAllowed && viewAllowed) {
                allowedServices.addAll(Arrays.asList(config.services()));
            }
        } catch (RepositoryException | RuntimeException e) {
            LOG.error("Error determining allowed services for {} {} {}", request.getRemoteUser(), contentPath, editorUrl, e);
        }
        return allowedServices;
    }

    /**
     * Not implemented here.
     */
    @Nullable
    @Override
    public GPTConfiguration getGPTConfiguration(@NotNull SlingHttpServletRequest request, @NotNull String contentPath) throws IllegalArgumentException {
        return null;
    }

    protected List<String> userAndGroupsOfUser(SlingHttpServletRequest request) throws RepositoryException {
        List<String> authorizableNames = new ArrayList<>();
        UserManager userManager = request.getResourceResolver().adaptTo(UserManager.class);
        if (userManager == null) { // fallback for plain Apache Sling
            JackrabbitSession session = ((JackrabbitSession) request.getResourceResolver().adaptTo(Session.class));
            userManager = Objects.requireNonNull(session.getUserManager());
        }
        Principal userPrincipal = request.getUserPrincipal();
        authorizableNames.add(userPrincipal.getName());
        Authorizable user = userManager.getAuthorizable(userPrincipal);
        if (user instanceof User) {
            User userInstance = (User) user;
            Iterator<Group> groups = userInstance.memberOf();
            while (groups.hasNext()) {
                authorizableNames.add(groups.next().getID());
            }
        }
        return authorizableNames;
    }

    protected boolean matchesAny(String value, String[] patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (value.matches(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

}
