package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.*;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FieldSetMemberTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(strings = {"fsm.getDBRequired()", "fsm.getRequired()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantBooleanMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       Schema.FieldSetMember fsm = l.get(0);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @ValueSource(strings = {"fsm.getFieldPath()", "fsm.getLabel()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantStringMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       Schema.FieldSetMember fsm = l.get(0);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testGetType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       Schema.FieldSetMember fsm = l.get(0);\n"
                        + "       System.debug(fsm.getType());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexEnumValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testGetSObjectField() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       Schema.FieldSetMember fsm = l.get(0);\n"
                        + "       System.debug(fsm.getSObjectField());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectField value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getAssociatedObjectType()), equalTo("Account"));
    }

    @CsvSource({
        "getSObjectField,com.salesforce.graph.symbols.apex.schema.SObjectField"
    }) // Leaving this parameterized so that we can add future methods we support here.
    @ParameterizedTest
    public void testSecondaryInvocationInForLoop(String methodName, String apexValueType)
            throws ClassNotFoundException {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void doSomething() {\n"
                        + "       List<FieldSetMember> myFieldMembers = new List<SObjectField>{SObjectType.Account.fieldSets.getMap().get('theName').getFields().get(0)};\n"
                        + "       for (FieldSetMember myFieldMember: myFieldMembers) {\n"
                        + "           System.debug(myFieldMember."
                        + methodName
                        + "());\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue forLoopValue = visitor.getSingletonResult();
        ApexValue<?> value = forLoopValue.getForLoopValues().get(0);
        MatcherAssert.assertThat(value, Matchers.instanceOf(Class.forName(apexValueType)));
    }
}
