package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexGlobalDescribeMapValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexMapValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.SystemNames;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DescribeSObjectResultTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @ValueSource(
            strings = {
                "Schema.SObjectType.Account",
                "SObjectType.Account",
                "Account.SObjectType.getDescribe()",
                "Schema.getGlobalDescribe().get('Account').getDescribe()"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectWithResolvedInitializer(String initializer) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeSObjectResult dr = "
                        + initializer
                        + ";\n"
                        + "       System.debug(dr);\n"
                        + "       System.debug(dr.getLocalName());\n"
                        + "       System.debug(dr.getName());\n"
                        + "       System.debug(dr.getSObjectType());\n"
                        + "       System.debug(dr.getRecordTypeInfos());\n"
                        + "       System.debug(dr.getRecordTypeInfosByDeveloperName());\n"
                        + "       System.debug(dr.isDeletable());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(7)));

        // dr
        DescribeSObjectResult dr = visitor.getResult(0);
        MatcherAssert.assertThat(dr.isIndeterminant(), equalTo(false));
        SObjectType sObjectType = (SObjectType) dr.getReturnedFrom().get();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        // dr.getLocalName()
        ApexStringValue localName = visitor.getResult(1);
        MatcherAssert.assertThat(localName.isIndeterminant(), equalTo(true));

        // dr.getName()
        ApexStringValue name = visitor.getResult(2);
        MatcherAssert.assertThat(name.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(TestUtil.apexValueToString(name), equalTo("Account"));

        // dr.getSObjectType()
        sObjectType = visitor.getResult(3);
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        // dr.getRecordTypeInfos
        ApexListValue recordTypeInfos = visitor.getResult(4);
        MatcherAssert.assertThat(recordTypeInfos.isIndeterminant(), equalTo(true));

        // dr.getRecordTypeInfosByDeveloperName
        ApexMapValue recordTypeInfosMap = visitor.getResult(5);
        MatcherAssert.assertThat(recordTypeInfosMap.isIndeterminant(), equalTo(true));

        // dr.isDeletable() - if casting happens successfully, we are good.
        ApexBooleanValue isDeletableValue = visitor.getResult(6);
    }

    @Test
    public void testObjectWithUnresolvedMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Schema.DescribeSObjectResult dr) {\n"
                        + "       System.debug(dr);\n"
                        + "       System.debug(dr.getName());\n"
                        + "       System.debug(dr.getSObjectType());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        // dr
        DescribeSObjectResult dr = visitor.getResult(0);
        MatcherAssert.assertThat(dr.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(dr.getReturnedFrom().isPresent(), equalTo(false));

        // dr.getName()
        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isIndeterminant(), equalTo(true));

        // dr.getSObjectType()
        SObjectType sObjectType = visitor.getResult(2);
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(sObjectType.getType().isPresent(), equalTo(false));
    }

    @ValueSource(strings = {"dr.getKeyPrefix()", "dr.getLabel()", "dr.getLabelPlural()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStringMethodsResolvedObjectType(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeSObjectResult dr = Schema.SObjectType.Account;\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(stringValue.getValue().isPresent(), equalTo(false));
        DescribeSObjectResult dr = (DescribeSObjectResult) stringValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dr.getSObjectType().get().getType()),
                equalTo("Account"));
    }

    @Test
    public void testObjectAccessDirectCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       System.debug(SObjectType.Account.isDeletable());\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue isDeletableValue = visitor.getResult(0);
    }

    public static Stream<Arguments> testDMLAccessMethods() {
        return SystemNames.DML_OBJECT_ACCESS_METHODS.stream().map(s -> Arguments.of(s));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDMLAccessMethods(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Schema.DescribeSObjectResult dr = Schema.SObjectType.Account;\n"
                        + "       System.debug(dr."
                        + methodName
                        + "());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue booleanValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(booleanValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(booleanValue.getValue().isPresent(), equalTo(false));
        DescribeSObjectResult dr = (DescribeSObjectResult) booleanValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(dr.getSObjectType().get().getType()),
                equalTo("Account"));
    }

    @ValueSource(strings = {"dr.getKeyPrefix()", "dr.getLabel()", "dr.getLabelPlural()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStringMethodsUnresolvedMethodParameter(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Schema.DescribeSObjectResult dr) {\n"
                        + "       System.debug("
                        + methodName
                        + ");\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(stringValue.getValue().isPresent(), equalTo(false));

        DescribeSObjectResult dr = (DescribeSObjectResult) stringValue.getReturnedFrom().get();
        MatcherAssert.assertThat(dr.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(dr.getSObjectType().isPresent(), equalTo(false));
    }

    public static Stream<Arguments> testSupplierMethods() {
        return Stream.of(
                Arguments.of(
                        "Schema.getGlobalDescribe()-AllIntermediateVariables",
                        "Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                                + "SObjectType sot = gd.get('Account');\n"
                                + "DescribeSObjectResult var = sot.getDescribe();\n",
                        true,
                        true),
                Arguments.of(
                        "Schema.getGlobalDescribe().get('Account')",
                        "SObjectType sot = Schema.getGlobalDescribe().get('Account');\n"
                                + "DescribeSObjectResult var = sot.getDescribe();\n",
                        true,
                        true),
                Arguments.of(
                        "Schema.getGlobalDescribe().get('Account').getDescribe()",
                        "DescribeSObjectResult var = Schema.getGlobalDescribe().get('Account').getDescribe();\n",
                        true,
                        true),
                Arguments.of(
                        "Schema.SObjectType.Account",
                        "DescribeSObjectResult var = Schema.SObjectType.Account;\n",
                        false,
                        false),
                Arguments.of(
                        "Account.SObjectType.getDescribe()",
                        "DescribeSObjectResult var = Account.SObjectType.getDescribe();\n",
                        true,
                        false),
                Arguments.of(
                        "Schema.describeSObjects",
                        "DescribeSObjectResult var = Schema.describeSObjects(new String[]{'Account','Contact'})[0];\n",
                        false,
                        false));
    }

    /**
     * Tests {@link DescribeSObjectResultFactory#METHOD_CALL_BUILDER_FUNCTION} and {@link
     * DescribeSObjectResultFactory#VARIABLE_EXPRESSION_BUILDER_FUNCTION}
     */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSupplierMethods(
            String testName,
            String codeSnippet,
            boolean expectReturnedFrom,
            boolean expectGlobalDescribeMap) {
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

        SObjectType sObjectType;
        ApexGlobalDescribeMapValue globalDescribeMapValue;

        DescribeSObjectResult describeSObjectResult = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                CoreMatchers.equalTo("Account"));

        if (expectReturnedFrom) {
            sObjectType = (SObjectType) describeSObjectResult.getReturnedFrom().orElse(null);
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(sObjectType.getType()),
                    CoreMatchers.equalTo("Account"));
            globalDescribeMapValue =
                    (ApexGlobalDescribeMapValue) sObjectType.getReturnedFrom().orElse(null);
            if (expectGlobalDescribeMap) {
                MatcherAssert.assertThat(globalDescribeMapValue, not(nullValue()));
            } else {
                MatcherAssert.assertThat(globalDescribeMapValue, is(nullValue()));
            }
        }
    }
}
