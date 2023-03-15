package com.salesforce.graph.symbols.apex;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class IndeterminantTest {
    private static final Logger LOGGER = LogManager.getLogger(IndeterminantTest.class);

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

    // spotless:off
    @CsvSource({
        // TODO: ApexPathWalker should also place null constraints
        // "IF Conditional equals Null,str == null,true,false",
        "IF Conditional NOT equals Null,str != null,false,true"
    })
    // spotless:on
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullConstraintOnIndeterminant_IfBranch(
            String name, String conditional, boolean isNull, boolean isIndeterminant) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(String str) {\n"
                        + "       if (" + conditional + ") {\n"
                        + "           System.debug(str);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";
        // spotless:on

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, Matchers.hasSize(2));
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            LOGGER.info("Test result size = " + visitor.resultSize());
            if (visitor.resultSize() > 0) {
                ApexStringValue stringValue = visitor.getSingletonResult();
                LOGGER.info("negative constraints: " + stringValue.negativeConstraints);
                LOGGER.info("positive constraints: " + stringValue.positiveConstraints);
                MatcherAssert.assertThat(stringValue.isNull(), equalTo(isNull));
                MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(isIndeterminant));
            }
        }
    }

    // spotless:off
    @CsvSource({
        // TODO: ApexPathWalker should also place null constraints
        "Else part of IF Conditional equals Null,str == null,false,true",
        // "Else part of IF Conditional NOT equals Null,str != null,true,false"
    })
    // spotless:on
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullConstraintOnIndeterminant_ElseBranch(
            String name, String conditional, boolean isNull, boolean isIndeterminant) {
        // spotless: off
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(String str) {\n"
                        + "       if ("
                        + conditional
                        + ") {\n"
                        + "           //do nothing \n"
                        + "       } else {\n"
                        + "           System.debug(str);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";
        // spotless: on

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, Matchers.hasSize(2));
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            if (visitor.resultSize() > 0) {
                ApexStringValue stringValue = visitor.getSingletonResult();
                MatcherAssert.assertThat(stringValue.isNull(), equalTo(isNull));
                MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(isIndeterminant));
            }
        }
    }
}
