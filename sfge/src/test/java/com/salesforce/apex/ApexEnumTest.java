package com.salesforce.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.exception.UnexpectedException;
import java.util.Arrays;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexEnumTest {
    private final ApexEnum apexEnum;

    public ApexEnumTest() {
        this.apexEnum = new ApexEnum("DisplayType", Arrays.asList("ADDRESS", "PICKLIST", "PHONE"));
    }

    @Test
    public void testSimple() {
        MatcherAssert.assertThat(apexEnum.getName(), equalTo("DisplayType"));
        MatcherAssert.assertThat(apexEnum.getValues(), hasSize(equalTo(3)));

        // Test invalid values
        MatcherAssert.assertThat(apexEnum.isValid("INVALID"), equalTo(false));
        UnexpectedException ex =
                Assertions.assertThrows(
                        UnexpectedException.class, () -> apexEnum.getValue("INVALID"));
        MatcherAssert.assertThat(ex.getMessage(), equalTo("Invalid value. value=" + "INVALID"));
    }

    public static Stream<Arguments> testMapping() {
        return Stream.of(
                Arguments.of("ADDRESS", 0), Arguments.of("PICKLIST", 1), Arguments.of("PHONE", 2));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-ordinal({1})")
    public void testMapping(String value, int ordinal) {
        ApexEnum.Value enumValue = apexEnum.getValues().get(ordinal);
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo(value));
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(ordinal));
        MatcherAssert.assertThat(apexEnum.isValid(value), equalTo(true));
        MatcherAssert.assertThat(apexEnum.getValue(value), equalTo(enumValue));
        MatcherAssert.assertThat(apexEnum.isValid(value.toLowerCase()), equalTo(true));
        MatcherAssert.assertThat(apexEnum.getValue(value.toLowerCase()), equalTo(enumValue));
    }

    @Test
    public void testInvalid() {
        MatcherAssert.assertThat(apexEnum.isValid("INVALID"), equalTo(false));
        UnexpectedException ex =
                Assertions.assertThrows(
                        UnexpectedException.class, () -> apexEnum.getValue("INVALID"));
        MatcherAssert.assertThat(ex.getMessage(), equalTo("Invalid value. value=" + "INVALID"));
    }
}
