package com.salesforce;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.provider.Arguments;

/** Utilities for working with junit parameterized tests */
public final class ArgumentsUtil {

    /**
     * @return a stream of all Arguments from <code>originalArguments</code> plus a version of each
     *     of the Arguments with the value at <code>index</code> converted to upper case. Null
     *     values at <code>index</code> do not have a new row added.
     */
    public static Stream<Arguments> permuteArgumentsWithUpperCase(
            List<Arguments> originalArguments, int index) {
        return permuteStringArguments(originalArguments, index, String::toUpperCase);
    }

    /**
     * @param function the function that will be executed on the argument found at <code>index
     *     </code>
     * @return a stream of all Arguments from <code>originalArguments</code> plus a version of each
     *     of the Arguments with the value at <code>index</code> converted using <code>function
     *     </code>. Null values at <code>index</code> do not have a new row added.
     */
    public static Stream<Arguments> permuteStringArguments(
            List<Arguments> originalArguments, int index, Function<String, String> function) {
        final List<Arguments> arguments = new ArrayList<>(originalArguments);

        for (Arguments originalArgument : originalArguments) {
            final String value = (String) originalArgument.get()[index];
            if (StringUtils.isNotEmpty(value)) {
                // Loop through values and invoke the function on it
                final Object[] originalValues = originalArgument.get();
                final List<Object> newValues = new ArrayList<>();
                for (int i = 0; i < originalValues.length; i++) {
                    if (i == index) {
                        newValues.add(function.apply(value));
                    } else {
                        newValues.add(originalValues[i]);
                    }
                }
                // Create a new Arguments object with the new permutation
                arguments.add(Arguments.of(newValues.toArray(new Object[] {})));
            }
        }

        return arguments.stream();
    }

    private ArgumentsUtil() {}
}
