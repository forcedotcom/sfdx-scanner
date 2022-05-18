package com.salesforce.matchers;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matcher for matching strings provided by {@link SystemDebugAccumulator} when there are
 * multiple paths and the order of the results isn't important
 *
 * <p>Match a single value
 *
 * <pre>
 * List<TestRunner.Result<SystemDebugAccumulator>> results = TestRunner.walkPaths(g, sourceCode);
 * MatcherAssert.assertThat(result, TestRunnerListMatcher.hasValuesAnyOrder("Hello", "Goodbye"));
 * </pre>
 */
public class TestRunnerListMatcher
        extends TypeSafeMatcher<List<TestRunner.Result<SystemDebugAccumulator>>> {
    private final List<String> expectedValues;
    private final List<String> actualValues;

    private TestRunnerListMatcher(String... expectedValues) {
        this.expectedValues = Arrays.asList(expectedValues);
        this.actualValues = new ArrayList<>();
    }

    @Override
    protected boolean matchesSafely(List<TestRunner.Result<SystemDebugAccumulator>> item) {
        List<SystemDebugAccumulator> visitors =
                item.stream().map(r -> r.getVisitor()).collect(Collectors.toList());

        List<Optional<ApexValue<?>>> allResults = new ArrayList<>();
        for (SystemDebugAccumulator visitor : visitors) {
            allResults.addAll(visitor.getAllResults());
        }

        for (Optional<ApexValue<?>> result : allResults) {
            ApexValue<?> value = result.get();
            if (value.isNull()) {
                actualValues.add(null);
            } else {
                actualValues.add(TestUtil.apexValueToString(value));
            }
        }

        // Sort the values since the path ordering is non deterministic
        expectedValues.sort(Comparator.nullsFirst(String::compareTo));
        actualValues.sort(Comparator.nullsFirst(String::compareTo));

        return expectedValues.equals(actualValues);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" strings " + expectedValues);
    }

    @Override
    protected void describeMismatchSafely(
            List<TestRunner.Result<SystemDebugAccumulator>> item, Description description) {
        description.appendText("was " + actualValues);
    }

    /** Assert that the accumulator contains multiple values in an order */
    public static Matcher<List<TestRunner.Result<SystemDebugAccumulator>>> hasValuesAnyOrder(
            String... expectedValues) {
        return new TestRunnerListMatcher(expectedValues);
    }
}
