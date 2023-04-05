package com.composum.chatgpt.base.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link RateLimiter}.
 */
public class RateLimiterTest {

    protected long time = 0; // System.currentTimeMillis();
    protected long startTime = time;

    protected RateLimiter limiter;

    protected class RateLimiterWithTestSetup extends RateLimiter {
        public RateLimiterWithTestSetup(RateLimiter parent, int limit, int period, TimeUnit timeUnit) {
            super(parent, limit, period, timeUnit);
        }

        @Override
        protected long getCurrentTimeMillis() {
            return time;
        }

        @Override
        protected void sleep(long delay) {
            time += delay;
        }
    }


    @Before
    public void setUp() {
        limiter = new RateLimiterWithTestSetup(null, 10, 100, TimeUnit.SECONDS);
    }

    protected void waitFor(int seconds) {
        time += TimeUnit.SECONDS.toMillis(seconds);
    }

    @Test
    public void testRateLimiting() {
        MatcherAssert.assertThat(time, is(startTime + 0L));
        // the first five requests should not be limited
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            assertThat("On request " + i, time, is(startTime + 0L));
        }
        // the rest should spread out over the remaining two minutes.
        limiter.waitForLimit();
        assertThat(time, is(startTime + 20000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 40000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 60000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 80000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 100000L));
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            assertThat("On request " + i, time, is(startTime + 100000L));
        }
        limiter.waitForLimit();
        assertThat(time, is(startTime + 120000L));
    }

    @Test
    public void testRateLimitingWithWait() {
        MatcherAssert.assertThat(time, is(startTime + 0L));
        // the first five requests should not be limited
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            assertThat("On request " + i, time, is(startTime + 0L));
        }
        waitFor(90);
        assertThat(time, is(startTime + 90000L));
        // the rest should spread out over the remaining 10 seconds - limited, but faster limits
        limiter.waitForLimit();
        assertThat(time, is(startTime + 92000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 94000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 96000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 98000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 100000L));
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            assertThat("On request " + i, time, is(startTime + 100000L));
        }
        limiter.waitForLimit();
        assertThat(time, is(startTime + 120000L));
    }


    @Test
    public void testWithParent() {
        limiter = new RateLimiterWithTestSetup(limiter, 20, 1, TimeUnit.HOURS);
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            assertThat("On request " + i, time, is(startTime + 0L));
        }
        // the rest should spread out over the remaining two minutes, as the parent limiter says
        limiter.waitForLimit();
        assertThat(time, is(startTime + 20000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 40000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 60000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 80000L));
        limiter.waitForLimit();
        assertThat(time, is(startTime + 100000L));
        // now our limiter kicks in.
        limiter.waitForLimit();
        assertThat(time, is(startTime + 450000L));
    }

}
