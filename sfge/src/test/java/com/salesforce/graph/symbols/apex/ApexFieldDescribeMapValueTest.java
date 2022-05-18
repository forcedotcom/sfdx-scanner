package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.schema.SObjectField;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexFieldDescribeMapValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(
            strings = {
                "SObjectType.Account.fields.getMap()",
                "Schema.SObjectType.Account.fields.getMap()"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectWithResolvedInitializer(String initializer) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug("
                        + initializer
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(1)));

        ApexFieldDescribeMapValue value =
                (ApexFieldDescribeMapValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                value.getCanonicalType(), equalTo("Map<String,Schema.SObjectField>"));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithInlineParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       System.debug(fieldMap."
                        + methodName
                        + "('Phone'));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectField value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getFieldname()), equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getAssociatedObjectType().get()),
                equalTo("Account"));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithVariableParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       String s = 'Phone';\n"
                        + "       System.debug(fieldMap."
                        + methodName
                        + "(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectField value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getFieldname()), equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getAssociatedObjectType().get()),
                equalTo("Account"));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveWithUnresolvedMethodParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       System.debug(fieldMap."
                        + methodName
                        + "(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectField value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getFieldname().get().isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getAssociatedObjectType().get()),
                equalTo("Account"));
    }

    @ValueSource(strings = {"fieldMap.containsKey('Phone')", "fieldMap.isEmpty()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testBooleanMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       System.debug(fieldMap);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexFieldDescribeMapValue describeMapValue = visitor.getResult(0);
        MatcherAssert.assertThat(describeMapValue.isIndeterminant(), equalTo(false));

        ApexBooleanValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
        ApexFieldDescribeMapValue fieldMap =
                (ApexFieldDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(fieldMap.getAssociatedObjectType()),
                Matchers.equalTo("Account"));
    }

    @ValueSource(strings = {"fieldMap.size()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIntegerMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
        ApexFieldDescribeMapValue fieldMap =
                (ApexFieldDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(fieldMap.getAssociatedObjectType()),
                Matchers.equalTo("Account"));
    }

    @ValueSource(strings = {"fieldMap.keySet()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSetMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap();\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSetValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        ApexFieldDescribeMapValue fieldMap =
                (ApexFieldDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(fieldMap.getAssociatedObjectType()),
                Matchers.equalTo("Account"));
    }

    @Test
    public void testClone() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Map<String, Schema.SObjectField> fieldMap = Schema.SObjectType.Account.fields.getMap().clone();\n"
                        + "       System.debug(fieldMap);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // The cloned and returnedFrom objects should look the same
        ApexFieldDescribeMapValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getAssociatedObjectType()),
                Matchers.equalTo("Account"));

        ApexFieldDescribeMapValue returnedFrom =
                (ApexFieldDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(returnedFrom.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(returnedFrom.getAssociatedObjectType()),
                Matchers.equalTo("Account"));
    }
}
