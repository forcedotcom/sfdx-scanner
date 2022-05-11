package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexSimpleValueFactoryTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    public static Stream<Arguments> apexTypeToJavaClass() {
        return Stream.of(
                Arguments.of("Boolean", ApexBooleanValue.class),
                Arguments.of("Decimal", ApexDecimalValue.class),
                Arguments.of("Double", ApexDoubleValue.class),
                Arguments.of("Integer", ApexIntegerValue.class),
                Arguments.of("Long", ApexLongValue.class));
    }

    /** Verify the correct type is returned and it is indeterminant */
    @MethodSource(value = "apexTypeToJavaClass")
    @ParameterizedTest(name = "{displayName}: type=({0}):clazz=({1})")
    public void testIndeterminantParameter(String type, Class<? extends ApexValue<?>> clazz) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug("
                        + type
                        + ".valueOf(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, instanceOf(clazz));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    /**
     * This is a special case for instance variables, they may appear null to the engine, but the
     * engine isn't certain, in this case the value should be indeterminant. See {@link
     * ApexValue#checkForUseAsNullParameter(MethodCallExpressionVertex)}
     */
    @MethodSource(value = "apexTypeToJavaClass")
    @ParameterizedTest(name = "{displayName}: type=({0}):clazz=({1})")
    public void testNullInstanceVariableReturnsIndeterminant(
            String type, Class<? extends ApexValue<?>> clazz) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public String s { get; set; }\n"
                        + "    public void doSomething() {\n"
                        + "       System.debug("
                        + type
                        + ".valueOf(this.s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, instanceOf(clazz));
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    public static Stream<Arguments> nonNullParameters() {
        return Stream.of(
                Arguments.of("Boolean", "true", Boolean.TRUE),
                Arguments.of("Decimal", "10.0", new BigDecimal("10.0")),
                Arguments.of("Double", "11.0", Double.parseDouble("11.0")),
                Arguments.of("Integer", "12", Integer.parseInt("12")),
                Arguments.of("Long", "13", Long.parseLong("13")));
    }

    /**
     * Ensure that paths where "s" is null are excluded. This code typically has 2 paths. However,
     * {@link ApexSimpleValueFactory} invokes {@link
     * ApexValue#checkForUseAsNullParameter(MethodCallExpressionVertex)} to ensure that only
     * non-null values are passed to #valueOf. This has the effect of excluding the path with the
     * implicit else.
     */
    @MethodSource(value = "nonNullParameters")
    @ParameterizedTest(name = "{displayName}: type=({0}):clazz=({1})")
    public void testNullLocalVariablePathIsExcluded(
            String type, String initializer, Object expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(Boolean setS) {\n"
                        + "       String s;\n"
                        + "       if (setS) {\n"
                        + "       	s = '"
                        + initializer
                        + "';\n"
                        + "       }\n"
                        + "       System.debug("
                        + type
                        + ".valueOf(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSimpleValue<?, ?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isValuePresent(), equalTo(true));
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }

    @MethodSource(value = "nonNullParameters")
    @ParameterizedTest(name = "{displayName}: type=({0}):value=({1})):expected=({2}))")
    public void testDeterminantParameter(String type, String initializer, Object expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = '"
                        + initializer
                        + "';\n"
                        + "       System.debug("
                        + type
                        + ".valueOf(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSimpleValue<?, ?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isValuePresent(), equalTo(true));
        MatcherAssert.assertThat(value.getValue().get(), equalTo(expected));
    }
}
