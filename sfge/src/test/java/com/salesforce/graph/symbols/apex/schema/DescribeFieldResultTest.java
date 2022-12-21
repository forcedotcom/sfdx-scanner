package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.*;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DescribeFieldResultTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    // TODO: More initializers
    @ValueSource(strings = {"Schema.SObjectType.Account.fields.Phone"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectWithResolvedInitializer(String initializer) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeFieldResult dfr = "
                        + initializer
                        + ";\n"
                        + "       System.debug(dfr);\n"
                        + "       System.debug(dfr.getName());\n"
                        + "       System.debug(dfr.getType());\n"
                        + "       System.debug(dfr.getSObjectField());\n"
                        +
                        // Testing ApexDisplayType#name() here, it is the only method on the class
                        "       System.debug(dfr.getType().name());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(5)));

        // dfr
        DescribeFieldResult dfr = visitor.getResult(0);
        MatcherAssert.assertThat(dfr.isIndeterminant(), Matchers.equalTo(false));
        DescribeSObjectResult dr = (DescribeSObjectResult) dfr.getReturnedFrom().get();
        MatcherAssert.assertThat(dr.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));

        // dfr.getName()
        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(TestUtil.apexValueToString(name), Matchers.equalTo("Phone"));

        // dfr.getType()
        ApexEnumValue displayType = visitor.getResult(2);
        MatcherAssert.assertThat(displayType.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(displayType.getValue().isPresent(), Matchers.equalTo(false));
        dfr = (DescribeFieldResult) displayType.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getFieldName()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));

        // dfr.getSObjectField()
        SObjectField sObjectField = visitor.getResult(3);
        MatcherAssert.assertThat(sObjectField.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectField.getFieldname()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectField.getAssociatedObjectType()),
                Matchers.equalTo("Account"));

        // dfr.getType().name()
        ApexStringValue typeName = visitor.getResult(4);
        MatcherAssert.assertThat(typeName.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(typeName.getValue().isPresent(), Matchers.equalTo(false));
        displayType = (ApexEnumValue) typeName.getReturnedFrom().get();
        dfr = (DescribeFieldResult) displayType.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getFieldName()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));
    }

    @ValueSource(strings = {"dfr.getLabel()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStringMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeFieldResult dfr = Schema.SObjectType.Account.fields.Phone;\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(value.getValue().isPresent(), Matchers.equalTo(false));
        DescribeFieldResult dfr = (DescribeFieldResult) value.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getFieldName()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));
    }

    @ValueSource(strings = {"dfr.getPicklistValues()", "dfr.getReferenceTo()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testListMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeFieldResult dfr = Schema.SObjectType.Account.fields.Phone;\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), Matchers.equalTo(false));
        DescribeFieldResult dfr = (DescribeFieldResult) value.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getFieldName()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));
    }

    public static Stream<Arguments> testDMLAccessMethods() {
        return SystemNames.DML_FIELD_ACCESS_METHODS.stream().map(s -> Arguments.of(s));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDMLAccessMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeFieldResult dfr = Schema.SObjectType.Account.fields.Phone;\n"
                        + "       System.debug(dfr."
                        + methodName
                        + "());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue booleanValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(booleanValue.isIndeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(booleanValue.getValue().isPresent(), Matchers.equalTo(false));
        DescribeFieldResult dfr = (DescribeFieldResult) booleanValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getFieldName()), Matchers.equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dfr.getSObjectType().get().getType()),
                Matchers.equalTo("Account"));
    }

    public static Stream<Arguments> testSupplierMethods() {
        return Stream.of(
                Arguments.of(
                        "Schema.SObjectType.Account.fields.Name",
                        "DescribeFieldResult var = Schema.SObjectType.Account.fields.Name;\n"),
                Arguments.of(
                        "Schema.SObjectType.Account.fields.getMap().get('Name').getDescribe()",
                        "DescribeFieldResult var = Schema.SObjectType.Account.fields.getMap().get('Name').getDescribe();\n"),
                Arguments.of(
                        "Account.fields.Name.getDescribe()",
                        "DescribeFieldResult var = Account.fields.Name.getDescribe();\n"),
                Arguments.of(
                        "Account.SObjectType.getDescribe().fields.getMap().get('Name').getDescribe()",
                        "DescribeFieldResult var = Account.SObjectType.getDescribe().fields.getMap().get('Name').getDescribe();\n"),
                Arguments.of(
                        "Account.Name.getDescribe()",
                        "DescribeFieldResult var = Account.Name.getDescribe();\n"));
    }

    /**
     * Tests {@link DescribeFieldResultFactory#METHOD_CALL_BUILDER_FUNCTION} and {@link
     * DescribeSObjectResultFactory#VARIABLE_EXPRESSION_BUILDER_FUNCTION}
     */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSupplierMethods(String testName, String codeSnippet) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + codeSnippet
                    + "       System.debug(var);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        DescribeFieldResult describeFieldResult = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeFieldResult.getFieldName()), equalTo("Name"));

        DescribeSObjectResult describeSObjectResult =
                describeFieldResult.getDescribeSObjectResult().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                equalTo("Account"));
    }

    @CsvSource({
        "isCreateable,com.salesforce.graph.symbols.apex.ApexBooleanValue",
        "getName,com.salesforce.graph.symbols.apex.ApexStringValue",
        "getPicklistValues,com.salesforce.graph.symbols.apex.ApexListValue",
        "getReferenceTo,com.salesforce.graph.symbols.apex.ApexListValue",
        "getSObjectField,com.salesforce.graph.symbols.apex.schema.SObjectField"
    })
    @ParameterizedTest
    public void testSecondaryInvocationInForLoop(String methodName, String apexValueType)
            throws ClassNotFoundException {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void doSomething() {\n"
                        + "       List<SObjectField> fields = new List<SObjectField>{Account.Name, Contact.Phone};\n"
                        + "       for (SObjectField myField: fields) {\n"
                        + "           System.debug(myField.getDescribe()."
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
