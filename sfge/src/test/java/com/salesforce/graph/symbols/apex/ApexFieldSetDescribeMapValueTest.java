package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.schema.FieldSet;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexFieldSetDescribeMapValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(
            strings = {
                "SObjectType.Account.fieldSets.getMap()",
                "Schema.SObjectType.Account.fieldSets.getMap()"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectWithResolvedInitializer(String initializer) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       System.debug("
                        + initializer
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(1)));

        ApexFieldSetDescribeMapValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("Map<String,Schema.FieldSet>"));
    }

    /** Get and remove have the same behavior */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveFieldSet(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       System.debug(SObjectType.Account.fieldSets.getMap()."
                        + methodName
                        + "('theName'));\n"
                        + "       System.debug(SObjectType.Account.fieldSets.getMap()."
                        + methodName
                        + "(x));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        FieldSet fieldSet;
        ApexStringValue fieldSetName;

        // Fieldset name = 'theName'
        fieldSet = visitor.getResult(0);
        MatcherAssert.assertThat(fieldSet.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                fieldSet.getCanonicalType(),
                equalTo(ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(fieldSet.getSObjectType()), equalTo("Account"));
        fieldSetName = (ApexStringValue) fieldSet.getFieldSetName().get();
        MatcherAssert.assertThat(fieldSetName.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(TestUtil.apexValueToString(fieldSetName), equalTo("theName"));

        // Fieldset name = unresolved x
        fieldSet = visitor.getResult(1);
        MatcherAssert.assertThat(fieldSet.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                fieldSet.getCanonicalType(),
                equalTo(ApexStandardLibraryUtil.Type.SCHEMA_FIELD_SET));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(fieldSet.getSObjectType()), equalTo("Account"));
        fieldSetName = (ApexStringValue) fieldSet.getFieldSetName().get();
        MatcherAssert.assertThat(fieldSetName.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testClone() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       System.debug(SObjectType.Account.fieldSets.getMap().clone());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(1)));

        // The cloned and returnedFrom objects should look the same
        ApexFieldSetDescribeMapValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("Map<String,Schema.FieldSet>"));

        ApexFieldSetDescribeMapValue returnedFrom =
                (ApexFieldSetDescribeMapValue) value.getReturnedFrom().get();
        MatcherAssert.assertThat(returnedFrom.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                returnedFrom.getCanonicalType(), equalTo("Map<String,Schema.FieldSet>"));
    }
}
