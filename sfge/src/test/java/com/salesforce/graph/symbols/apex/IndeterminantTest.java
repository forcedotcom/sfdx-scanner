package com.salesforce.graph.symbols.apex;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class IndeterminantTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Method that can't be resolved to a user or standard type will have their type synthesized
     * based on the variable declaration.
     */
    @ValueSource(
            strings = {"Boolean", "Id", "Integer", "List<String>", "Map<String, String>", "String"})
    @ParameterizedTest(name = "{0}")
    public void testUnresolvedUserMethodReturnIsIndeterminant(String variableType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + variableType
                        + " v = MyOtherClass.someMethod();\n"
                        + "       System.debug(v);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        // It should be of a particular type, not the default ApexSingleValue
        MatcherAssert.assertThat(value, not(instanceOf(ApexSingleValue.class)));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        value.getTypeVertex().get().getCanonicalType().equals(variableType);
    }

    /** Method parameters have their type synthesized based on the variable declaration. */
    @ValueSource(
            strings = {"Boolean", "Id", "Integer", "List<String>", "Map<String, String>", "String"})
    @ParameterizedTest(name = "{0}")
    public void testUnresolvedUserMethodParameterIsIndeterminant(String variableType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething("
                        + variableType
                        + " v) {\n"
                        + "       System.debug(v);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        // It should be of a particular type, not the default ApexSingleValue
        MatcherAssert.assertThat(value, not(instanceOf(ApexSingleValue.class)));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        value.getTypeVertex().get().getCanonicalType().equals(variableType);
    }

    public static Stream<Arguments> systemMethodSource() {
        return Stream.of(
                Arguments.of("Boolean", "UserInfo.isMultiCurrencyOrganization()"),
                Arguments.of("Integer", "new Schema.DescribeIconResult().getHeight()"),
                Arguments.of("String", "UserInfo.getUserid()"));
    }

    /**
     * Methods that are resolved to a standard type will have their type synthesized based on the
     * return type of the method
     */
    @MethodSource("systemMethodSource")
    @ParameterizedTest(name = "variableType=({0}):method=({1})")
    public void testUnresolvedSystemMethodReturnAssignmentIsIndeterminant(
            String variableType, String method) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + variableType
                        + " v = "
                        + method
                        + ";\n"
                        + "       System.debug(v);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        // It should be of a particular type, not the default ApexSingleValue
        MatcherAssert.assertThat(value, not(instanceOf(ApexClassInstanceValue.class)));
        MatcherAssert.assertThat(value, not(instanceOf(ApexSingleValue.class)));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        value.getTypeVertex().get().getCanonicalType().equals(variableType);
    }

    /**
     * Methods that are resolved to a standard type will have their type synthesized based on the
     * return type of the method
     */
    @MethodSource("systemMethodSource")
    @ParameterizedTest(name = "variableType=({0}):method=({1})")
    public void testUnresolvedSystemMethodReturnPassAsParameterIsIndeterminant(
            String variableType, String method) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug("
                        + method
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        // It should be of a particular type, not the default ApexSingleValue
        MatcherAssert.assertThat(value, not(instanceOf(ApexSingleValue.class)));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        value.getTypeVertex().get().getCanonicalType().equals(variableType);
    }
}
