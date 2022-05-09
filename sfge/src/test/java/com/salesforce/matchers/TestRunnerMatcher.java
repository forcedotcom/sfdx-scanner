package com.salesforce.matchers;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matcher for matching strings provided by {@link SystemDebugAccumulator}
 *
 * <p>Match a single value
 *
 * <pre>
 * TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
 * MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
 * </pre>
 *
 * Match multiple values including null
 *
 * <pre>
 * TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
 * MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("Hello", "Goodbye", null));
 * </pre>
 */
public final class TestRunnerMatcher
        extends TypeSafeMatcher<TestRunner.Result<SystemDebugAccumulator>> {
    private final List<String> expectedValues;
    private final List<String> actualValues;

    private TestRunnerMatcher(String expectedValue) {
        this.expectedValues = Arrays.asList(expectedValue);
        this.actualValues = new ArrayList<>();
    }

    private TestRunnerMatcher(String... expectedValues) {
        this.expectedValues = Arrays.asList(expectedValues);
        this.actualValues = new ArrayList<>();
    }

    @Override
    protected boolean matchesSafely(TestRunner.Result<SystemDebugAccumulator> item) {
        List<Optional<ApexValue<?>>> allResults = item.getVisitor().getAllResults();

        for (Optional<ApexValue<?>> result : allResults) {
            ApexValue<?> value = result.get();
            if (value.isNull()) {
                actualValues.add(null);
            } else {
                actualValues.add(TestUtil.apexValueToString(value));
            }
        }

        return expectedValues.equals(actualValues);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" strings equal to " + expectedValues);
    }

    @Override
    protected void describeMismatchSafely(
            TestRunner.Result<SystemDebugAccumulator> item, Description description) {
        description.appendText("was " + actualValues);
    }

    /** Assert that the accumulator contains a single value */
    public static Matcher<TestRunner.Result<SystemDebugAccumulator>> hasValue(
            String expectedValue) {
        return new TestRunnerMatcher(expectedValue);
    }

    /** Assert that the accumulator contains multiple values in a specific order */
    public static Matcher<TestRunner.Result<SystemDebugAccumulator>> hasValues(
            String... expectedValues) {
        return new TestRunnerMatcher(expectedValues);
    }
}
