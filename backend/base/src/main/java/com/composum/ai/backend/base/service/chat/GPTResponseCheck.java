package com.composum.ai.backend.base.service.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An additional check that verifies whether the translation was carried out right - such as paths in HTML weren't translated.
 */
public interface GPTResponseCheck {

    /**
     * Performs a check, and if the check fails it returns additional instructions with which the request is retried.
     *
     * @return null if the response is fine, otherwise the additional instructions to fix the response (possibly an empty string).
     */
    @Nullable
    String responseProblem(@Nonnull String source, @Nonnull String translation);

    /**
     * Finds problems of translation wrt. source and any of the checks.
     */
    static String collectResponseProblems(@Nullable List<GPTResponseCheck> checks, @Nullable String source, @Nullable String translation) {
        if (checks == null || source == null || translation == null) {
            // source / translation null is a doubful thing here, but the problem is not in the response check.
            return null;
        }
        StringBuilder problems = new StringBuilder();
        for (GPTResponseCheck check : checks) {
            String problem = check.responseProblem(source, translation);
            if (problem != null) {
                problems.append(problem.trim()).append("\n\n");
            }
        }
        return problems.length() > 0 ? "\n\n" + problems.toString() : null;
    }

    /**
     * A check that all href attributes in richtext appear in the translation.
     * Sometimes the GPT translates paths containing recognizable words, so we check that the set of HREFs in the
     * original is the same as the set of HREFs in the translation.
     */
    static GPTResponseCheck KEEP_HREF_TRANSLATION_CHECK = new GPTResponseCheck() {

        private final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\" ]*)\"");

        @Nullable
        @Override
        public String responseProblem(@Nonnull String source, @Nonnull String translation) {
            Set<String> sourceHrefs = findHrefs(source);
            if (sourceHrefs.isEmpty()) { // not applicable
                return null;
            }
            Set<String> translationHrefs = findHrefs(translation);
            if (!sourceHrefs.equals(translationHrefs)) {
                Set<String> missingHrefs = new HashSet<>(sourceHrefs);
                missingHrefs.removeAll(translationHrefs);
                List<String> examples = new ArrayList<>();
                if (!missingHrefs.isEmpty()) {
                    missingHrefs.stream().limit(3).sorted().forEach(examples::add);
                } else { // strange, but we add some random examples from sourceHrefs
                    sourceHrefs.stream().limit(3).sorted().forEach(examples::add);
                }
                return "CAUTION: Do not translate or change absolute or relative URLs in href attributes in HTML links, such as " +
                        examples.stream().collect(Collectors.joining(", "))
                        + " .";
            }
            return null;
        }

        private Set<String> findHrefs(String text) {
            Set<String> hrefs = new HashSet<>();
            Matcher matcher = HREF_PATTERN.matcher(text);
            while (matcher.find() && matcher.group().length() < 200) { // if too long then probably something is broken.
                hrefs.add(matcher.group());
            }
            return hrefs;
        }
    };

}
