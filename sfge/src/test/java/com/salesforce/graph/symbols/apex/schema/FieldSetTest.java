package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexFieldSetListValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FieldSetTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(
            strings = {"fs.getDescription()", "fs.getLabel()", "fs.getName()", "fs.getNamespace()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndeterminantStringMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       FieldSet fs = SObjectType.Account.fieldSets.getMap().get('theName');\n"
                        + "       System.debug(fs);\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        FieldSet fieldSet = visitor.getResult(0);
        MatcherAssert.assertThat(fieldSet.isIndeterminant(), equalTo(false));

        ApexStringValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testFieldSetsDotGetMap() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String obj, String fieldSetName) {\n"
                        + "       Schema.SObjectType objType = Schema.getGlobalDescribe().get(obj);\n"
                        + "       Schema.DescribeSObjectResult dr = objType.getDescribe();\n"
                        + "       Schema.FieldSet fs = dr.fieldSets.getMap().get(fieldSetName);\n"
                        + "       System.debug(fs);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        FieldSet value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("Schema.FieldSet"));
        SObjectType sObjectType = value.getSObjectType().get();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        ApexValue<?> type = sObjectType.getType().get();
        MatcherAssert.assertThat(type.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testGetFields() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       FieldSet fs = SObjectType.Account.fieldSets.getMap().get('theName');\n"
                        + "       System.debug(fs.getFields());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexFieldSetListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("List<Schema.FieldSetMember>"));
    }

    @CsvSource({
        "getFields,com.salesforce.graph.symbols.apex.ApexFieldSetListValue",
        "getSObjectType,com.salesforce.graph.symbols.apex.schema.SObjectType"
    })
    @ParameterizedTest
    public void testSecondaryInvocationInForLoop(String methodName, String apexValueType)
            throws ClassNotFoundException {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void doSomething() {\n"
                        + "       List<FieldSet> myFieldSets = new List<FieldSet>{SObjectType.Account.fieldSets.getMap().get('theName')};\n"
                        + "       for (FieldSet myFieldSet: myFieldSets) {\n"
                        + "           System.debug(myFieldSet."
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
