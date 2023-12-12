package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexStringValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testClassGetName() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug(MyClass.class.getName());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue apexStringValue = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(apexStringValue.isIndeterminant(), equalTo(false));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take no parameters and return a boolean
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> booleanReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CONTAINS_WHITESPACE),
                Arguments.of(ApexStringValue.METHOD_IS_ALL_LOWER_CASE),
                Arguments.of(ApexStringValue.METHOD_IS_ALL_UPPER_CASE),
                Arguments.of(ApexStringValue.METHOD_IS_ALPHA),
                Arguments.of(ApexStringValue.METHOD_IS_ALPHA_SPACE),
                Arguments.of(ApexStringValue.METHOD_IS_ALPHANUMERIC),
                Arguments.of(ApexStringValue.METHOD_IS_ALPHANUMERIC_SPACE),
                Arguments.of(ApexStringValue.METHOD_IS_ASCII_PRINTABLE),
                Arguments.of(ApexStringValue.METHOD_IS_BLANK),
                Arguments.of(ApexStringValue.METHOD_IS_EMPTY),
                Arguments.of(ApexStringValue.METHOD_IS_NOT_BLANK),
                Arguments.of(ApexStringValue.METHOD_IS_NOT_EMPTY),
                Arguments.of(ApexStringValue.METHOD_IS_NUMERIC),
                Arguments.of(ApexStringValue.METHOD_IS_NUMERIC_SPACE),
                Arguments.of(ApexStringValue.METHOD_IS_WHITESPACE));
    }

    @MethodSource(value = "booleanReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_BooleanReturn(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "booleanReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_BooleanReturn(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string and return a boolean
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> booleanReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CONTAINS),
                Arguments.of(ApexStringValue.METHOD_CONTAINS_ANY),
                Arguments.of(ApexStringValue.METHOD_CONTAINS_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_CONTAINS_NONE),
                Arguments.of(ApexStringValue.METHOD_CONTAINS_ONLY),
                Arguments.of(ApexStringValue.METHOD_ENDS_WITH),
                Arguments.of(ApexStringValue.METHOD_ENDS_WITH_IGNORE_CASE),
                Arguments.of(SystemNames.METHOD_EQUALS),
                Arguments.of(ApexStringValue.METHOD_EQUALS_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_STARTS_WITH),
                Arguments.of(ApexStringValue.METHOD_STARTS_WITH_IGNORE_CASE));
    }

    @MethodSource(value = "booleanReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_BooleanReturn_DeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('a'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "booleanReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_BooleanReturn_IndeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "booleanReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_BooleanReturn_DeterminantStringParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "('Bar'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_DIFFERENCE),
                Arguments.of(ApexStringValue.METHOD_REMOVE_END),
                Arguments.of(ApexStringValue.METHOD_REMOVE_END_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_REMOVE_START),
                Arguments.of(ApexStringValue.METHOD_REMOVE_START_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_AFTER),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_AFTER_LAST),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_BETWEEN),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_BEFORE),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_BEFORE_LAST));
    }

    @MethodSource(value = "stringReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantStringParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('a'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantStringParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo Foo';\n"
                        + "       System.debug(s." + methodName + "(a));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantStringParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo foo';\n"
                        + "       System.debug(s." + methodName + "('oo'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take two string parameters and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnStringParameterIntegerParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_REPEAT));
    }

    @MethodSource(value = "stringReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('a', 2));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a, Integer b) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a, 2));\n"
                        + "       System.debug(s." + methodName + "('x', b));\n"
                        + "       System.debug(s." + methodName + "(a, b));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexStringValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "('a', 2));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take two string parameters and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnStringParameterStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_REPLACE),
                Arguments.of(ApexStringValue.METHOD_REPLACE_ALL));
    }

    @MethodSource(value = "stringReturnStringParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantStringParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('a', 'b'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantStringParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a, String b) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a, 'x'));\n"
                        + "       System.debug(s." + methodName + "('x', a));\n"
                        + "       System.debug(s." + methodName + "(a, b));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexStringValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantStringParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "('a', 'b'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take no parameters and return an integer
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_LENGTH),
                Arguments.of(ApexStringValue.METHOD_HASH_CODE));
    }

    @MethodSource(value = "integerReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take an integer parameter and return an integer
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerReturnIntegerParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CHAR_AT),
                Arguments.of(ApexStringValue.METHOD_CODE_POINT_AT),
                Arguments.of(ApexStringValue.METHOD_CODE_POINT_BEFORE),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_CHAR),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF_CHAR));
    }

    @MethodSource(value = "integerReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_IntegerParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "(1));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_IndeterminantIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer i) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(i));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_DeterminantIntegerParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(1));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take an integer parameter and return an integer
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerReturnIntegerParameterIntegerParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CODE_POINT_COUNT),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_CHAR),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF_CHAR),
                Arguments.of(ApexStringValue.METHOD_OFFSET_BY_CODE_POINTS));
    }

    @MethodSource(value = "integerReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_IntegerParameterIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "(1, 3));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_IndeterminateIntegerParameterIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer i) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(i, 3));\n"
                        + "       System.debug(s." + methodName + "(1, i));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_DeterminateIntegerParameterIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'FooBar';\n"
                        + "       System.debug(s." + methodName + "(1, 3));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string parameter and return an integer
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_COMPARE_TO),
                Arguments.of(ApexStringValue.METHOD_COUNT_MATCHES),
                Arguments.of(ApexStringValue.METHOD_GET_LEVENSHTEIN_DISTANCE),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_ANY),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_ANY_BUT),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_DIFFERENCE),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF_IGNORE_CASE));
    }

    @MethodSource(value = "integerReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_DeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('a'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_IndeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_DeterminantStringParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "('bar'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string parameter and an integer parameter and return an integer
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerReturnStringParameterIntegerParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_INDEX_OF),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF),
                Arguments.of(ApexStringValue.METHOD_LAST_INDEX_OF_IGNORE_CASE));
    }

    @MethodSource(value = "integerReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_DeterminantParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "('oo',3));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_IndeterminantIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a, Integer i) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a, 3));\n"
                        + "       System.debug(s." + methodName + "('oo', i));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_DeterminantIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "('oo', 3));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string and return a list of strings
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringListReturnStringParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_SPLIT));
    }

    @MethodSource(value = "stringListReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringListReturn_DeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       List<String> l = s." + methodName + "('a');\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringListReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringListReturn_IndeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       List<String> l = s." + methodName + "(a);\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringListReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringListReturn_DeterminantStringParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Test';\n"
                        + "       List<String> l = s." + methodName + "('es');\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(false));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take a string and return a list of strings
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringListReturnStringParameterIntegerParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_SPLIT));
    }

    @MethodSource(value = "stringListReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringListReturn_DeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       List<String> l = s." + methodName + "('a', 2);\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringListReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringListReturn_IndeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a, Integer i) {\n"
                        + "       String s = 'Foo';\n"
                        + "       List<String> l = s." + methodName + "(a, i);\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringListReturnStringParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringListReturn_DeterminantStringAndIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Test';\n"
                        + "       List<String> l = s." + methodName + "('es', 2);\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(false));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take an integer and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnIntegerParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_ABBREVIATE),
                Arguments.of(ApexStringValue.METHOD_CENTER),
                Arguments.of(ApexStringValue.METHOD_LEFT),
                Arguments.of(ApexStringValue.METHOD_LEFT_PAD),
                Arguments.of(ApexStringValue.METHOD_REPEAT),
                Arguments.of(ApexStringValue.METHOD_RIGHT),
                Arguments.of(ApexStringValue.METHOD_RIGHT_PAD),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING));
    }

    @MethodSource(value = "stringReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "(10));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantIntegerParameter(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "(a));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantIntegerParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'FooBar';\n"
                        + "       System.debug(s." + methodName + "(4));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take two integers and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnIntegerParameterIntegerParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_ABBREVIATE),
                Arguments.of(ApexStringValue.METHOD_MID),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING));
    }

    @MethodSource(value = "stringReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "(0, 2));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantIntegerParameters(
            String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer a, Integer b) {\n"
                        + "       String s = 'Hello';\n"
                        // Test the combination of the parameters as determinant/indeterminant
                        + "       System.debug(s." + methodName + "(a, 2));\n"
                        + "       System.debug(s." + methodName + "(2, b));\n"
                        + "       System.debug(s." + methodName + "(a, b));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexStringValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantIntegerParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Hello';\n"
                        + "       System.debug(s." + methodName + "(4, 5));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take an integer and a string and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnIntegerParameterStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_LEFT_PAD),
                Arguments.of(ApexStringValue.METHOD_RIGHT_PAD));
    }

    @MethodSource(value = "stringReturnIntegerParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "(0, 'bar'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer a, String b) {\n"
                        + "       String s = 'Hello';\n"
                        + "       System.debug(s." + methodName + "(a, 'bar'));\n"
                        + "       System.debug(s." + methodName + "(2, b));\n"
                        + "       System.debug(s." + methodName + "(a, b));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexStringValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_DeterminantParameters(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Hello';\n"
                        + "       System.debug(s." + methodName + "(4, 'bar'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take no parameter and return a string
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CAPITALIZE),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_CSV),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_HTML_3),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_ECMA_SCRIPT),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_HTML_4),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_JAVA),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_SINGLE_QUOTES),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_UNICODE),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_XML),
                Arguments.of(ApexStringValue.METHOD_NORMALIZE_SPACE),
                Arguments.of(ApexStringValue.METHOD_REVERSE),
                Arguments.of(ApexStringValue.METHOD_SWAP_CASE),
                Arguments.of(ApexStringValue.METHOD_TO_LOWER_CASE),
                Arguments.of(ApexStringValue.METHOD_TO_UPPER_CASE),
                Arguments.of(ApexStringValue.METHOD_TRIM),
                Arguments.of(ApexStringValue.METHOD_UNCAPITALIZE),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_CSV),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_ECMA_SCRIPT),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_HTML_3),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_HTML_4),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_UNICODE),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_XML));
    }

    @MethodSource(value = "stringReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s." + methodName + "());\n"
                        + "    }\n"
                        + "}";
        // spotless:on

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take no parameters and return an Integer list
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> stringListReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_SPLIT_BY_CHARACTER_TYPE),
                Arguments.of(ApexStringValue.METHOD_SPLIT_BY_CHARACTER_TYPE_CAMEL_CASE));
    }

    @MethodSource(value = "stringListReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringListReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       List<String> l = s." + methodName + "();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexListValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue firstIntValue = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(firstIntValue.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringListReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringListReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'FooBar';\n"
                        + "       List<String> l = s." + methodName + "();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexListValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));

        ApexStringValue firstIntValue = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(firstIntValue.getValue().isPresent(), Matchers.equalTo(true));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * ---- Test methods that take no parameters and return an Integer list
     * ---------------------------------------------------------------------------------------------
     */
    public static Stream<Arguments> integerListReturnNoParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_GET_CHARS));
    }

    @MethodSource(value = "integerListReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerListReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       List<Integer> l = s." + methodName + "();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexListValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));

        ApexIntegerValue firstIntValue = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(firstIntValue.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerListReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerListReturn_NoParameter(String methodName) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Foo';\n"
                        + "       List<Integer> l = s." + methodName + "();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexListValue value = result.getVisitor().getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));

        ApexIntegerValue firstIntValue = result.getVisitor().getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(firstIntValue.getValue().isPresent(), Matchers.equalTo(true));
    }

    /** ---- Test countMatches ---------------------------------------- */
    @Test
    public void testCountMatches() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Hello Goodbye Hello';\n"
                        + "       System.debug(s.countMatches('Hello'));\n"
                        + "       System.debug(s.countMatches('Goodbye'));\n"
                        + "       System.debug(s.countMatches('Hello Goodbye'));\n"
                        + "       System.debug(s.countMatches('Foo'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexIntegerValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.getValue().get(), equalTo(2));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.getValue().get(), equalTo(1));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.getValue().get(), equalTo(1));

        value = visitor.getResult(3);
        MatcherAssert.assertThat(value.getValue().get(), equalTo(0));
    }

    /** ---- Test remove* methods ---------------------------------------- */
    public static Stream<Arguments> testRemove() {
        return Stream.of(
                Arguments.of("removeStart", "Good", "bye"),
                Arguments.of("removeStart", "o", "Goodbye"),
                Arguments.of("removeStart", "GOOD", "Goodbye"),
                Arguments.of("removeStartIgnoreCase", "Good", "bye"),
                Arguments.of("removeStartIgnoreCase", "o", "Goodbye"),
                Arguments.of("removeStartIgnoreCase", "GOOD", "bye"),
                Arguments.of("removeEnd", "bye", "Good"),
                Arguments.of("removeEnd", "o", "Goodbye"),
                Arguments.of("removeEnd", "BYE", "Goodbye"),
                Arguments.of("removeEndIgnoreCase", "bye", "Good"),
                Arguments.of("removeEndIgnoreCase", "o", "Goodbye"),
                Arguments.of("removeEndIgnoreCase", "BYE", "Good"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testRemove(String method, String parameter, String expectedValue) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s." + method + "('" + parameter + "'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expectedValue));
    }

    /** ---- Test equals* methods ---------------------------------------- */
    public static Stream<Arguments> testEquality() {
        return Stream.of(
                Arguments.of("equals", "Goodbye", true),
                Arguments.of("equals", "o", false),
                Arguments.of("equals", "GOODBYE", false),
                Arguments.of("equalsIgnoreCase", "Goodbye", true),
                Arguments.of("equalsIgnoreCase", "o", false),
                Arguments.of("equalsIgnoreCase", "GOODBYE", true));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testEquality(String method, String parameter, boolean expectedValue) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s." + method + "('" + parameter + "'));\n"
                        + "    }\n"
                        + "}";
        // spotless:on

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expectedValue));
    }

    /** ---- Test left/right methods ---------------------------------------- */
    // Values larger than the string return the original string
    public static Stream<Arguments> testLeftRight() {
        return Stream.of(
                Arguments.of("left", 4, "Good"),
                Arguments.of("left", 10, "Goodbye"),
                Arguments.of("right", 3, "bye"),
                Arguments.of("right", 10, "Goodbye"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testLeftRight(String method, int length, String expectedResult) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s." + method + "(" + length + "));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expectedResult));
    }

    /** ---- Test valueOf static method ---------------------------------------- */
    @ValueSource(strings = {"10", "10.0"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValueOf(String value) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug(String.valueOf(" + value + "));\n"
                        + "    }\n"
                        + "}";
        // spotless:on

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(value));
    }

    @ValueSource(strings = {"Boolean", "Double", "Decimal", "Integer", "Datetime"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValueOfIndeterminant(String variableType) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(" + variableType + " x) {\n"
                        + "       System.debug(String.valueOf(x));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    /** ---- Test valueOf static method ---------------------------------------- */
    @Test
    public void testValueOfGmtIndeterminant() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(DateTime dt) {\n"
                        + "       System.debug(String.valueOfGmt(dt));\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    /** ---- Test join method ---------------------------------------- */
    public static Stream<Arguments> testStringJoin() {
        return Stream.of(
                Arguments.of("List<String> l = new List<String> {'a', 'b', 'c'};", "a.b.c"),
                Arguments.of("List<Integer> l = new List<Integer> {10, 20, 30};", "10.20.30"),
                Arguments.of(
                        "List<String> l = new List<String> {'a', 'b', c};",
                        "a.b." + ApexStringValueFactory.UNRESOLVED_ARGUMENT_PREFIX + 2));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStringJoin(String variableDeclaration, String expected) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n" + variableDeclaration + "\n"
                        + "		String s = String.join(l, '.');\n"
                        + "   	System.debug(s);\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    public static Stream<Arguments> testStringJoinIndeterminant() {
        return Stream.of(
                Arguments.of("l = new List<String> {'a', 'b', 'c'};", "unResolved"),
                Arguments.of("" /*Don't Initialize*/, "'.'"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStringJoinIndeterminant(String variableAssignement, String separator) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(List<String> l) {\n"
                        + variableAssignement + "\n"
                        + "		String s = String.join(l, " + separator + ");\n"
                        + "   	System.debug(s);\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    /** ---- Test abbreviate method ---------------------------------------- */
    @Test
    public void testAbbreviate() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'abcdefghijklmno';\n"
                        + "   	System.debug(s.abbreviate(5));\n"
                        + "   	System.debug(s.abbreviate(10, 5));\n"
                        + "   }\n"
                        + "}";
        // spotless:on

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("ab...", "...fghi..."));
    }

    /** ---- Test contains* methods ---------------------------------------- */
    public static Stream<Arguments> testContains() {
        return Stream.of(
                Arguments.of("contains", "Hello", true),
                Arguments.of("contains", "Foo", false),
                Arguments.of("contains", "HELLO", false),
                Arguments.of("containsIgnoreCase", "Hello", true),
                Arguments.of("containsIgnoreCase", "Foo", false),
                Arguments.of("containsIgnoreCase", "HELLO", true));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: method=({0})-parameter=({1})-expected=({2})")
    public void testContains(String method, String parameter, boolean expected) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'Hello Goodbye';\n"
                        + "   	System.debug(s." + method + "('" + parameter + "'));\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexBooleanValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }

    /** ---- Test indexOf* methods ---------------------------------------- */
    public static Stream<Arguments> testIndexOf() {
        return Stream.of(
                Arguments.of("indexOf", "Hello", 0),
                Arguments.of("indexOf", "Foo", -1),
                Arguments.of("indexOf", "HELLO", -1),
                Arguments.of("indexOfIgnoreCase", "Hello", 0),
                Arguments.of("indexOfIgnoreCase", "Foo", -1),
                Arguments.of("indexOfIgnoreCase", "HELLO", 0));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: method=({0})-parameter=({1})-expected=({2})")
    public void testIndexOf(String method, String parameter, int expected) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'Hello Goodbye';\n"
                        + "   	System.debug(s." + method + "('" + parameter + "'));\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexIntegerValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }

    /** ---- Test escape* methods ---------------------------------------- */
    public static Stream<Arguments> testEscape() {
        return Stream.of(
                Arguments.of("escapeCsv", "Max1, \"Max2\"", "\"Max1, \"\"Max2\"\"\""),
                Arguments.of("escapeEcmaScript", "\"grade\": 3.9/4.0", "\\\"grade\\\": 3.9\\/4.0"),
                Arguments.of(
                        "escapeHtml3", "\"<Black&White>\"", "&quot;&lt;Black&amp;White&gt;&quot;"),
                Arguments.of(
                        "escapeHtml4", "\"<Black&White>\"", "&quot;&lt;Black&amp;White&gt;&quot;"),
                Arguments.of(
                        "escapeJava",
                        "Company: \"Salesforce.com\"",
                        "Company: \\\"Salesforce.com\\\""),
                Arguments.of(
                        "escapeSingleQuotes",
                        "\\'Salesforce.com\\'", // During assignment in apex, this would fit in as
                        // String s = '\'Salesforce.com\'';
                        "\\'Salesforce.com\\'") // Actual value printed should've undergone
                // transformation such that it prints the escaped
                // value
                );
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: method=({0})-parameter=({1})-expected=({2})")
    public void testEscape(String method, String parameter, String expected) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = '" + parameter + "';\n"
                        + "   	System.debug(s." + method + "());\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo(expected));
    }

    /** ---- Test unescape* methods ---------------------------------------- */
    public static Stream<Arguments> testUnescape() {
        return Stream.of(
                Arguments.of("unescapeCsv", "\"Max1, \"\"Max2\"\"\"", "Max1, \"Max2\""),
                Arguments.of("unescapeEcmaScript", "\\\"3.8\\\",\\\"3.9\\\"", "\"3.8\",\"3.9\""),
                Arguments.of(
                        "unescapeHtml3",
                        "&quot;&lt;Black&amp;White&gt;&quot;",
                        "\"<Black&White>\""),
                Arguments.of(
                        "unescapeHtml4",
                        "&quot;&lt;Black&amp;White&gt;&quot;",
                        "\"<Black&White>\""),
                Arguments.of(
                        "unescapeJava",
                        "Company: \\\"Salesforce.com\\\"",
                        "Company: \"Salesforce.com\""));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: method=({0})-parameter=({1})-expected=({2})")
    public void testUnescape(String method, String parameter, String expected) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = '" + parameter + "';\n"
                        + "   	System.debug(s." + method + "());\n"
                        + "   }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo(expected));
    }

    /** ---- Test format method ---------------------------------------- */
    @Test
    public void testFormatStaticMethod() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String template = '{0} is the first param. {1} is the second';\n"
                        + "        List<Object> parameters = new List<Object> {'Foo', 2};\n"
                        + "        String formatted = String.format(template, parameters);\n"
                        + "        System.debug(formatted);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(
                value.getValue().get(), equalTo("Foo is the first param. 2 is the second"));
    }

    /** ---- Test fromCharArray method ---------------------------------------- */
    @Test
    public void testFromCharArrayStaticMethod() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        List<Integer> charArr= new Integer[]{72,105};\n"
                        + "        String convertedChar = String.fromCharArray(charArr);\n"
                        + "        System.debug(convertedChar);\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo("Hi"));
    }

    /** ---- Test stripHtmlTags method ---------------------------------------- */
    @Test
    public void testStripHtmlTagsIndeterminant() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "        System.debug(s.stripHtmlTags());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @Test
    public void testStripHtmlTagsDeterminant() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = '<html><body><div><p>hello</p></div></body></html>';\n"
                        + "        System.debug(s.stripHtmlTags());\n"
                        + "    }\n"
                        + "}";
        // spotless:on
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        // Note that the apex implementation of stripHtmlTags is inside of core, so we already
        // return indeterminant.
        // Until it becomes more easily accessibly, I don't think we want to reimplement it in our
        // code base.
        ApexStringValue value = result.getVisitor().getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }
}
