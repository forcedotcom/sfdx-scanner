package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.schema.FieldSetMember;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexFieldSetListValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(strings = {"l.Contains(x)", "l.equals(x)", "l.isEmpty()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantBooleanMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults().size(), equalTo(2));

        ApexFieldSetListValue list = visitor.getResult(0);
        MatcherAssert.assertThat(list.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(list.getCanonicalType(), equalTo("List<Schema.FieldSetMember>"));

        ApexBooleanValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @ValueSource(strings = {"l.size()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantIntegerMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults().size(), equalTo(2));

        ApexFieldSetListValue list = visitor.getResult(0);
        MatcherAssert.assertThat(list.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(list.getCanonicalType(), equalTo("List<Schema.FieldSetMember>"));

        ApexIntegerValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.FieldSetMember> l = SObjectType.Account.fieldSets.getMap().get('theName').getFields();\n"
                        + "       System.debug(l);\n"
                        + "       System.debug(l.get(0));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults().size(), equalTo(2));

        ApexFieldSetListValue list = visitor.getResult(0);
        MatcherAssert.assertThat(list.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(list.getCanonicalType(), equalTo("List<Schema.FieldSetMember>"));

        FieldSetMember value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
    }
}
