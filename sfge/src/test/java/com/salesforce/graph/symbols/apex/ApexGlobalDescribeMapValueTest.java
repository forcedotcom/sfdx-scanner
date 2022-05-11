package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexGlobalDescribeMapValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithInlineParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       System.debug(gd);\n"
                        + "       System.debug(gd."
                        + methodName
                        + "('Account'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexGlobalDescribeMapValue gd = visitor.getResult(0);
        MatcherAssert.assertThat(gd.getCanonicalType(), equalTo("Map<String,Schema.SObjectType>"));

        SObjectType sObjectType = visitor.getResult(1);
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithVariableParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       String s = 'Account';\n"
                        + "       System.debug(gd."
                        + methodName
                        + "(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectType value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithUnresolvedMethodParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       System.debug(gd."
                        + methodName
                        + "(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectType value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getType().get().isIndeterminant(), equalTo(true));
    }

    /**
     * All of these methods should return an indeterminant boolean since we don't know how the
     * server would respond
     */
    @ValueSource(strings = {"gd.containsKey('Account')", "gd.isEmpty()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetGlobalDescribeBooleanMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));
    }

    /**
     * All of these methods should return an indeterminant integer since we don't know how the
     * server would respond
     */
    @ValueSource(strings = {"gd.size()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetGlobalDescribeIntegerMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));
    }

    /**
     * All of these methods should return an indeterminant set since we don't know how the server
     * would respond
     */
    @ValueSource(strings = {"gd.keySet()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetGlobalDescribeSetMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                        + "       Set<String> keySet = "
                        + methodName
                        + ";\n"
                        + "       System.debug(keySet);\n"
                        + "       System.debug(keySet.size());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // keySet
        ApexSetValue keySet = visitor.getResult(0);
        MatcherAssert.assertThat(keySet.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(keySet.getValues(), hasSize(equalTo(0)));

        // keySet.size
        ApexIntegerValue size = visitor.getResult(1);
        MatcherAssert.assertThat(size.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(size.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testClone() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe().clone();\n"
                        + "       System.debug(gd);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // The cloned and returnedFrom objects should look the same
        ApexGlobalDescribeMapValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                value.getCanonicalType(), equalTo("Map<String,Schema.SObjectType>"));

        ApexGlobalDescribeMapValue returnedFrom =
                (ApexGlobalDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(returnedFrom.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                returnedFrom.getCanonicalType(), equalTo("Map<String,Schema.SObjectType>"));
    }
}
