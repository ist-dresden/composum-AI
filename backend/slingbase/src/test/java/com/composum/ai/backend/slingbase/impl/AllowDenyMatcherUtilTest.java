package com.composum.ai.backend.slingbase.impl;

import static org.hamcrest.CoreMatchers.is;

import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.ai.backend.slingbase.impl.AllowDenyMatcherUtil;

public class AllowDenyMatcherUtilTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void testJoinPatternsIntoAnyMatcher() {
        // Test with non-null patterns
        Pattern pattern = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(new String[]{"abc", "def", "ghi", "", null});
        ec.checkThat(pattern.matcher("abc").matches(), is(true));
        ec.checkThat(pattern.matcher("def").matches(), is(true));
        ec.checkThat(pattern.matcher("ghi").matches(), is(true));
        ec.checkThat(pattern.matcher("jkl").matches(), is(false));
        ec.checkThat(pattern.matcher("").matches(), is(false));

        // Test with null patterns
        pattern = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(null);
        ec.checkThat(pattern, is((Pattern) null));

        pattern = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(new String[0]);
        ec.checkThat(pattern, is((Pattern) null));
    }

    @Test
    public void testAllowDenyCheck() {
        // Test case 1: value matches allowPattern but not denyPattern
        Pattern allowPattern = Pattern.compile("abc");
        Pattern denyPattern = Pattern.compile("def");
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("abc", allowPattern, denyPattern), is(true));

        // Test case 2: value matches both allowPattern and denyPattern
        denyPattern = Pattern.compile("abc");
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("abc", allowPattern, denyPattern), is(false));

        // Test case 3: value does not match allowPattern
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("xyz", allowPattern, denyPattern), is(false));

        // Test case 4: allowPattern is null
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("abc", null, denyPattern), is(false));

        // Test case 5: denyPattern is null and value matches allowPattern
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("abc", allowPattern, null), is(true));

        // Test case 6: denyPattern is null and value does not match allowPattern
        ec.checkThat(AllowDenyMatcherUtil.allowDenyCheck("xyz", allowPattern, null), is(false));
    }


}
