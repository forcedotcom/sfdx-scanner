package com.salesforce.matchers;

import com.salesforce.graph.symbols.apex.ApexValue;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/** Matchers for ApexValues */
public final class ApexValueMatchers {
    /**
     * Matches the result of {@link ApexValue#getTypeVertex().get().getCanonicalType()} to the
     * provided string.
     *
     * <p>Example
     *
     * <pre>
     *     MatcherAssert.assertThat(apexSingleValue, ApexValueMatcher.typeEqualTo("Account"));
     * </pre>
     */
    public static final class TypeEqualTo extends TypeSafeMatcher<ApexValue<?>> {
        public static final String TYPEABLE_NOT_PRESENT = "ApexValue#getTypeVertex returned empty";

        private final String expectedValue;
        private String errorMessage;

        private TypeEqualTo(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        protected boolean matchesSafely(ApexValue<?> item) {
            com.salesforce.graph.vertex.Typeable typeable = item.getTypeVertex().orElse(null);
            if (typeable == null) {
                errorMessage = TYPEABLE_NOT_PRESENT;
                return false;
            }

            final String canonicalType = typeable.getCanonicalType();
            if (!canonicalType.equalsIgnoreCase(expectedValue)) {
                errorMessage = "was " + canonicalType;
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedValue);
        }

        @Override
        protected void describeMismatchSafely(ApexValue<?> item, Description mismatchDescription) {
            mismatchDescription.appendText(errorMessage);
        }
    }

    /** Creates a matcher that asserts the type of an ApexValue. {@link TypeEqualTo} */
    public static TypeEqualTo typeEqualTo(String expectedValue) {
        return new TypeEqualTo(expectedValue);
    }

    private ApexValueMatchers() {}
}
