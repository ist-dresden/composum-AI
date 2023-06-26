package com.composum.ai.backend.base.service;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * For a stream of String segments (like the ChatGPT tokens that trickle in from ChatGPT when streaming) this collects
 * the parts and forwards the collected part every 500ms to a given consumer. This decreases the overhead of displaying
 * the changed content in the browser.
 */
public class StringstreamSlowdown implements Consumer<String>, AutoCloseable {

    @Nonnull
    private final Consumer<String> target;
    private final long minimumDelayMillis;
    private long lastForwardTime = getTime();

    private final StringBuffer collected = new StringBuffer();

    /**
     * Sets the target where we forward the collected strings.
     *
     * @param target             the destination
     * @param minimumDelayMillis the minimum delay in milliseconds for which we wait before forwarding anything.
     */
    public StringstreamSlowdown(@Nonnull Consumer<String> target, long minimumDelayMillis) {
        this.target = target;
        this.minimumDelayMillis = minimumDelayMillis;
    }

    protected long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    public synchronized void accept(String s) {
        collected.append(s);
        long now = getTime();
        if (now - lastForwardTime >= minimumDelayMillis && collected.length() > 0) {
            String forwardFragment = retrieveForwardFragment();
            if (forwardFragment != null && !forwardFragment.isEmpty()) {
                target.accept(forwardFragment);
                lastForwardTime = now;
            }
        }
    }

    public synchronized void flush() {
        target.accept(collected.toString());
        collected.setLength(0);
    }

    private static final Pattern PATTERN_FRAGMENT = Pattern.compile("(.*[\\s\\p{Punct}]).*");

    /**
     * Returns the longest fragment of {@link #collected} ending in a whitespace or punctuation character.
     * This fragment is removed from {@link #collected}. If the fragment is longer than 80 characters but doesn't match
     * this rule, it's forwarded, anyway.
     */
    protected String retrieveForwardFragment() {
        Matcher matcher = PATTERN_FRAGMENT.matcher(collected);
        if (matcher.matches()) {
            String fragment = matcher.group(1);
            collected.delete(0, fragment.length());
            return fragment;
        } else if (collected.length() > 80) {
            String result = collected.toString();
            collected.setLength(0);
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        flush();
    }

}
