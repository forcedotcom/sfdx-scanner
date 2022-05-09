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
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug(MyClass.class.getName());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexStringValue.isIndeterminant(), equalTo(false));
    }

    /** Methods that take a string and return a boolean */
    public static Stream<Arguments> booleanReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_CONTAINS),
                Arguments.of(ApexStringValue.METHOD_CONTAINS_IGNORE_CASE),
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
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "('a'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "booleanReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_BooleanReturn_IndeterminantStringParameter(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    /** Methods that take a string and return a string */
    public static Stream<Arguments> stringReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_REMOVE_END),
                Arguments.of(ApexStringValue.METHOD_REMOVE_END_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_REMOVE_START),
                Arguments.of(ApexStringValue.METHOD_REMOVE_START_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_AFTER),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_BETWEEN),
                Arguments.of(ApexStringValue.METHOD_SUB_STRING_BEFORE));
    }

    @MethodSource(value = "stringReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantStringParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "('a'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantStringParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    /** Methods that take two string parameters and return a string */
    public static Stream<Arguments> stringReturnStringParameterStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_REPLACE),
                Arguments.of(ApexStringValue.METHOD_REPLACE_ALL));
    }

    @MethodSource(value = "stringReturnStringParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantStringParameters(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "('a', 'b'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnStringParameterStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantStringParameters(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a, String b) {\n"
                        + "       String s = 'Foo';\n"
                        +
                        // Test the combination of the parameters as determinant/indeterminant
                        "       System.debug(s."
                        + methodName
                        + "(a, 'x'));\n"
                        + "       System.debug(s."
                        + methodName
                        + "('x', a));\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a, b));\n"
                        + "    }\n"
                        + "}";

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

    /** Methods that take no parameters and return an integer */
    public static Stream<Arguments> integerReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_LENGTH),
                Arguments.of(ApexStringValue.METHOD_HASH_CODE));
    }

    @MethodSource(value = "integerReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_NoParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    /** Methods that take a string parmeter and return an integer */
    public static Stream<Arguments> integerReturnStringParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_INDEX_OF),
                Arguments.of(ApexStringValue.METHOD_INDEX_OF_IGNORE_CASE),
                Arguments.of(ApexStringValue.METHOD_COUNT_MATCHES));
    }

    @MethodSource(value = "integerReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_IntegerReturn_DeterminantStringParameter(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "('a'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "integerReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_IntegerReturn_IndeterminantStringParameter(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    /** Methods that take a string and return a list of strings */
    public static Stream<Arguments> listReturnStringParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_SPLIT));
    }

    @MethodSource(value = "listReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_ListReturn_DeterminantStringParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       List<String> l = s."
                        + methodName
                        + "('a');\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "       System.debug(l[1]);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value;
        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "listReturnStringParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_ListReturn_IndeterminantStringParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       List<String> l = s."
                        + methodName
                        + "(a);\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l[0]);\n"
                        + "       System.debug(l[1]);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), Matchers.equalTo(true));

        ApexStringValue value;
        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    /** Methods that take two integers and return a string */
    public static Stream<Arguments> stringReturnIntegerParameterIntegerParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_SUB_STRING));
    }

    @MethodSource(value = "stringReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantIntegerParameters(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "(0, 2));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameterIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantIntegerParameters(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer a, Integer b) {\n"
                        + "       String s = 'Hello';\n"
                        +
                        // Test the combination of the parameters as determinant/indeterminant
                        "       System.debug(s."
                        + methodName
                        + "(a, 2));\n"
                        + "       System.debug(s."
                        + methodName
                        + "(2, b));\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a, b));\n"
                        + "    }\n"
                        + "}";

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

    /** Methods that take an integer and return a string */
    public static Stream<Arguments> stringReturnIntegerParameter() {
        return Stream.of(Arguments.of(ApexStringValue.METHOD_ABBREVIATE));
    }

    @MethodSource(value = "stringReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_DeterminantIntegerParameter(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "(10));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @MethodSource(value = "stringReturnIntegerParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDeterminantString_StringReturn_IndeterminantIntegerParameter(
            String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer a) {\n"
                        + "       String s = 'Foo';\n"
                        + "       System.debug(s."
                        + methodName
                        + "(a));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    public static Stream<Arguments> stringReturnNoParameter() {
        return Stream.of(
                Arguments.of(ApexStringValue.METHOD_ESCAPE_CSV),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_HTML_3),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_ECMA_SCRIPT),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_HTML_4),
                Arguments.of(ApexStringValue.METHOD_ESCAPE_JAVA),
                Arguments.of(ApexStringValue.METHOD_NORMALIZE_SPACE),
                Arguments.of(ApexStringValue.METHOD_TO_LOWER_CASE),
                Arguments.of(ApexStringValue.METHOD_TO_UPPER_CASE),
                Arguments.of(ApexStringValue.METHOD_TRIM),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_CSV),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_HTML_3),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_ECMA_SCRIPT),
                Arguments.of(ApexStringValue.METHOD_UNESCAPE_HTML_4));
    }

    @MethodSource(value = "stringReturnNoParameter")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantString_StringReturn_NoParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(s."
                        + methodName
                        + "());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
    }

    @Test
    public void testCountMatches() {
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

    // No match returns the original string
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
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s."
                        + method
                        + "('"
                        + parameter
                        + "'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expectedValue));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s."
                        + method
                        + "('"
                        + parameter
                        + "'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expectedValue));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = 'Goodbye';\n"
                        + "       System.debug(s."
                        + method
                        + "("
                        + length
                        + "));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expectedResult));
    }

    @ValueSource(strings = {"10", "10.0"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValueOf(String value) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug(String.valueOf("
                        + value
                        + "));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(value));
    }

    @ValueSource(strings = {"Boolean", "Double", "Decimal", "Integer"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValueOfIndeterminant(String variableType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething("
                        + variableType
                        + " x) {\n"
                        + "       System.debug(String.valueOf(x));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + variableDeclaration
                        + "\n"
                        + "		String s = String.join(l, '.');\n"
                        + "   	System.debug(s);\n"
                        + "   }\n"
                        + "}";

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
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(List<String> l) {\n"
                        + variableAssignement
                        + "\n"
                        + "		String s = String.join(l, "
                        + separator
                        + ");\n"
                        + "   	System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testAbbreviate() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'abcdefghijklmno';\n"
                        + "   	System.debug(s.abbreviate(5));\n"
                        + "   	System.debug(s.abbreviate(10, 5));\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("ab...", "...fghi..."));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'Hello Goodbye';\n"
                        + "   	System.debug(s."
                        + method
                        + "('"
                        + parameter
                        + "'));\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = 'Hello Goodbye';\n"
                        + "   	System.debug(s."
                        + method
                        + "('"
                        + parameter
                        + "'));\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }

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
                        "Company: \\\"Salesforce.com\\\""));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: method=({0})-parameter=({1})-expected=({2})")
    public void testEscape(String method, String parameter, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = '"
                        + parameter
                        + "';\n"
                        + "   	System.debug(s."
                        + method
                        + "());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo(expected));
    }

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
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "		String s = '"
                        + parameter
                        + "';\n"
                        + "   	System.debug(s."
                        + method
                        + "());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo(expected));
    }

    // TODO: Tests on determinant data
}
