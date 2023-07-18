package com.composum.ai.composum.bundle.model;

import static com.composum.ai.composum.bundle.model.LabelExtensionVisibilityKey.CREATE;
import static com.composum.ai.composum.bundle.model.LabelExtensionVisibilityKey.DEFAULTVISIBILITY;
import static com.composum.ai.composum.bundle.model.LabelExtensionVisibilityKey.TRANSLATE;
import static com.composum.ai.composum.bundle.model.LabelExtensionVisibilityKey.isVisible;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test for {@link LabelExtensionVisibilityKey}.
 */
public class LabelExtensionVisibilityKeyTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testIsVisible() {
        // Test when the value is null
        assertThat(isVisible(null, CREATE), is(DEFAULTVISIBILITY));

        // Test when the value is blank
        assertThat(isVisible("", CREATE), is(DEFAULTVISIBILITY));

        // Test when the value is "ALL" or "true"
        assertThat(isVisible("ALL", CREATE), is(true));
        assertThat(isVisible("true", CREATE), is(true));

        // Test when the value is "NONE" or "false"
        assertThat(isVisible("NONE", CREATE), is(false));
        assertThat(isVisible("false", CREATE), is(false));

        // Test when the value is equal to the assistant's name
        assertThat(isVisible("CREATE", CREATE), is(true));

        // Test when the value starts with "!" and is followed by the assistant's name
        assertThat(isVisible("!CREATE", CREATE), is(false));

        // Test when none of the conditions are met and the default visibility is returned
        assertThat(isVisible("SOME_RANDOM_VALUE", CREATE), is(DEFAULTVISIBILITY));

        // Test when the value is a combination of values
        assertThat(isVisible("create,none", CREATE), is(true));
        assertThat(isVisible("create,none", TRANSLATE), is(false));

        // Test when the value is '!translate,all'
        assertThat(isVisible("!translate,all", CREATE), is(true));
        assertThat(isVisible("!translate,all", TRANSLATE), is(false));

    }
}
