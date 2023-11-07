package com.composum.ai.backend.slingbase.impl;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Utilities for matching allow / deny String[] pattern collections.
 */
public class AllowDenyMatcherUtil {

    /**
     * Makes a pattern that matches whenever one of the patterns matches the input.
     * If no patterns are given, we return null.
     */
    @Nullable
    public static Pattern joinPatternsIntoAnyMatcher(@Nullable String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            return null;
        }
        String regex = Stream.of(patterns)
                .filter(p -> p != null && !p.isEmpty())
                .map(p -> "(" + p + ")")
                .collect(Collectors.joining("|"));
        return regex.isEmpty() ? null : Pattern.compile(regex);
    }

    /**
     * Check whether a value matches the allowPattern but not the denyPattern. Thouse could be created with {@link #joinPatternsIntoAnyMatcher(String[])}.
     * If allowPattern is null it will never match. If denyPattern is null it is ignored, just the allowPattern matters.
     *
     * @see #joinPatternsIntoAnyMatcher(String[])
     */
    public static boolean allowDenyCheck(@Nullable String value, @Nullable Pattern allowPattern, @Nullable Pattern denyPattern) {
        if (allowPattern == null) {
            return false;
        }
        if (denyPattern != null && denyPattern.matcher(value).matches()) {
            return false;
        }
        return allowPattern.matcher(value).matches();
    }


    public static boolean matchesAny(String value, String[] patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (defaultString(value).matches(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean matchesAny(String value, List<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (pattern != null && defaultString(value).matches(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collects the username and all groups the user belongs to (either explicitly or inherited).
     */
    public static List<String> userAndGroupsOfUser(SlingHttpServletRequest request) throws RepositoryException {
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
}
