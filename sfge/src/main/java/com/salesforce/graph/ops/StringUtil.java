package com.salesforce.graph.ops;

import java.util.regex.Pattern;

/**
 * String utilities not provided by. Please check {@link org.apache.commons.lang3.StringUtils} for
 * functionality before adding any new methods to this class
 */
public final class StringUtil {
    /** Finds characters that are any type of space including tabs */
    private static final Pattern PATTERN_ALL_SPACES = Pattern.compile("\\s");

    private static final String EMPTY_STRING = "";

    /** Removes all spaces from {@code value} */
    public static final String stripAllSpaces(String value) {
        return PATTERN_ALL_SPACES.matcher(value).replaceAll(EMPTY_STRING);
    }

    private StringUtil() {}
}
