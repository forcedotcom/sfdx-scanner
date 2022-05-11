package com.salesforce.graph.ops.directive;

import static org.hamcrest.Matchers.equalTo;

import com.salesforce.ArgumentsUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EngineDirectiveCommandTest {
    public static Stream<Arguments> testDirectiveTokenFromString() {
        List<Arguments> arguments =
                Arrays.asList(
                        Arguments.of(null, null),
                        Arguments.of("", null),
                        Arguments.of("sfge-disable", EngineDirectiveCommand.DISABLE),
                        Arguments.of(
                                "sfge-disable-next-line", EngineDirectiveCommand.DISABLE_NEXT_LINE),
                        Arguments.of("sfge-disable-stack", EngineDirectiveCommand.DISABLE_STACK));

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expected({1})")
    public void testDirectiveTokenFromString(
            String value, EngineDirectiveCommand engineDirectiveCommand) {
        MatcherAssert.assertThat(
                EngineDirectiveCommand.fromString(value),
                equalTo(Optional.ofNullable(engineDirectiveCommand)));
    }

    public static Stream<Arguments> testDirectiveCode() {
        return Stream.of(
                Arguments.of(EngineDirectiveCommand.DISABLE, "sfge-disable"),
                Arguments.of(EngineDirectiveCommand.DISABLE_NEXT_LINE, "sfge-disable-next-line"),
                Arguments.of(EngineDirectiveCommand.DISABLE_STACK, "sfge-disable-stack"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expected({1})")
    public void testDirectiveCode(EngineDirectiveCommand value, String expected) {
        MatcherAssert.assertThat(value.getToken(), equalTo(expected));
        MatcherAssert.assertThat(value.toString(), equalTo(expected));
    }
}
