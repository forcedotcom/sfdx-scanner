package com.salesforce.matchers;

import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Wrapper that invokes {@link Optional#get()} on an Optional value before comparing it to the value
 * provided to {@link #optEqualTo(Object)}. Example
 *
 * <pre>
 *     Optional&lt;String&gt; optString = Optional.of("Hello");
 *     MatcherAssert.assertThat(optString, OptionalMatcher.optEqualTo("Hello");
 * </pre>
 */
public final class OptionalMatcher<T> extends TypeSafeMatcher<Optional<T>> {
    private final org.hamcrest.Matcher<T> matcher;

    private OptionalMatcher(T expectedValue) {
        this.matcher = equalTo(expectedValue);
    }

    @Override
    protected boolean matchesSafely(Optional<T> item) {
        return matcher.matches(item.get());
    }

    @Override
    public void describeTo(Description description) {
        matcher.describeTo(description);
    }

    @Override
    protected void describeMismatchSafely(Optional<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("was " + item.orElse(null));
    }

    public static <T> Matcher<T> optEqualTo(T expectedValue) {
        return new OptionalMatcher(expectedValue);
    }
}
