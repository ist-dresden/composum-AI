package com.composum.ai.backend.slingbase;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

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

}
