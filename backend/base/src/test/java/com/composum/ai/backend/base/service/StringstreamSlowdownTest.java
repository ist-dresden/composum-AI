package com.composum.ai.backend.base.service;


import java.util.function.Consumer;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class StringstreamSlowdownTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    private long time = 0;

    private StringBuilder result = new StringBuilder();
    private Consumer<String> target = (str) -> {
        ec.checkThat(str, CoreMatchers.notNullValue());
        ec.checkThat(str.length(), Matchers.greaterThan(1));
        result.append(str);
    };

    private StringstreamSlowdown slowdown = new StringstreamSlowdown(target, 250) {
        @Override
        protected long getTime() {
            return time;
        }
    };

    @Test
    public void testUsage() {
        slowdown.accept("Hel");
        ec.checkThat(result.toString(), Matchers.isEmptyString());
        slowdown.accept("lo there!");
        ec.checkThat(result.toString(), Matchers.isEmptyString());
        time += 500;
        slowdown.accept(" Let's go!");
        ec.checkThat(result.toString(), Matchers.is("Hello there! Let's go!"));
        slowdown.accept(" And again!");
        ec.checkThat(result.toString(), Matchers.is("Hello there! Let's go!"));
        time += 500;
        slowdown.accept(" Now again!");
        ec.checkThat(result.toString(), Matchers.is("Hello there! Let's go! And again! Now again!"));
        slowdown.accept(" The rest.");
        ec.checkThat(result.toString(), Matchers.is("Hello there! Let's go! And again! Now again!"));
        slowdown.flush();
        ec.checkThat(result.toString(), Matchers.is("Hello there! Let's go! And again! Now again! The rest."));
    }

}
