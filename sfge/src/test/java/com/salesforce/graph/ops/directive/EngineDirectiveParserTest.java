package com.salesforce.graph.ops.directive;

import static org.hamcrest.Matchers.equalTo;

import com.salesforce.ArgumentsUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EngineDirectiveParserTest {
    public static final String COMMENT = "this is a comment";
    public static EngineDirective DISABLE_NO_RULE =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE).build();
    public static EngineDirective DISABLE_STACK_NO_RULE =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE_STACK).build();
    public static EngineDirective DISABLE_NO_RULE_WITH_COMMENT =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                    .withComment(COMMENT)
                    .build();

    public static Stream<Arguments> testGetEngineDirective() {
        EngineDirective DISABLE_1 =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                        .withRuleName("Rule1")
                        .build();
        EngineDirective DISABLE_1_2 =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                        .withRuleNames(CollectionUtil.newTreeSetOf("Rule1", "Rule2"))
                        .build();
        EngineDirective DISABLE_STACK_1 =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE_STACK)
                        .withRuleName("Rule1")
                        .build();
        EngineDirective DISABLE_STACK_1_2 =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE_STACK)
                        .withRuleNames(CollectionUtil.newTreeSetOf("Rule1", "Rule2"))
                        .build();
        EngineDirective DISABLE_1_WITH_COMMENT =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                        .withRuleName("Rule1")
                        .withComment(COMMENT)
                        .build();
        EngineDirective DISABLE_1_2_WITH_COMMENT =
                EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                        .withRuleNames(CollectionUtil.newTreeSetOf("Rule1", "Rule2"))
                        .withComment(COMMENT)
                        .build();

        List<Arguments> arguments =
                Arrays.asList(
                        Arguments.of(",", NodeType.METHOD, null),
                        Arguments.of("", NodeType.METHOD, null),
                        Arguments.of("sfge-unrelated", NodeType.EXPRESSION_STATEMENT, null),
                        Arguments.of("sfge-disable", NodeType.USER_CLASS, DISABLE_NO_RULE),
                        Arguments.of(
                                "sfge-disable -- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_NO_RULE_WITH_COMMENT),
                        Arguments.of(
                                "sfge-disable --- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_NO_RULE_WITH_COMMENT),
                        Arguments.of(
                                "sfge-disable Rule1 -- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_1_WITH_COMMENT),
                        Arguments.of(
                                "sfge-disable Rule1 --- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_1_WITH_COMMENT),
                        Arguments.of(
                                "sfge-disable Rule1, Rule2 -- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_1_2_WITH_COMMENT),
                        Arguments.of(
                                "sfge-disable Rule1, Rule2 --- this is a comment",
                                NodeType.USER_CLASS,
                                DISABLE_1_2_WITH_COMMENT),
                        Arguments.of("sfge-disable-stack", NodeType.METHOD, DISABLE_STACK_NO_RULE),
                        Arguments.of("sfge-disable Rule1", NodeType.USER_CLASS, DISABLE_1),
                        Arguments.of("sfge-disable-stack Rule1", NodeType.METHOD, DISABLE_STACK_1),
                        Arguments.of("sfge-disable Rule1, Rule2", NodeType.USER_CLASS, DISABLE_1_2),
                        Arguments.of(
                                "sfge-disable-stack Rule1, Rule2",
                                NodeType.METHOD,
                                DISABLE_STACK_1_2),
                        Arguments.of(
                                "sfge-disable-stack Rule1 ,Rule2 ",
                                NodeType.METHOD_CALL_EXPRESSION,
                                DISABLE_STACK_1_2));

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-label=({1})-expected({2})")
    public void testGetEngineDirective(String value, String label, EngineDirective expected) {
        EngineDirective engineDirective =
                EngineDirectiveParser.getEngineDirective(value, label).orElse(null);
        MatcherAssert.assertThat(engineDirective, equalTo(expected));
    }

    public static Stream<Arguments> testAnnotationParameterVertexNormalizeValue() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("   ", ""),
                Arguments.of(" \t \t\t  ", ""),
                Arguments.of("  Hello ", "Hello"),
                Arguments.of("  Hello  \t   \t\t   Goodbye ", "Hello Goodbye"),
                Arguments.of("sfge-disable-stack Rule1, Rule2", "sfge-disable-stack Rule1,Rule2"),
                Arguments.of(
                        "sfge-disable-stack Rule1 , Rule2\t ,Rule3",
                        "sfge-disable-stack Rule1,Rule2,Rule3"),
                Arguments.of(
                        "sfge-disable-stack Rule1, Rule2 ,Rule3",
                        "sfge-disable-stack Rule1,Rule2,Rule3"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expected({1})")
    public void testAnnotationParameterVertexNormalizeValue(String value, String expected) {
        MatcherAssert.assertThat(
                EngineDirectiveParser.normalizeValue(value), Matchers.equalTo(expected));
    }
}
