package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SObjectTypeTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @CsvSource({
        "Schema.getGlobalDescribe().get('Account'), ApexGlobalDescribeMapValue",
        "Account.SObjectType," /*Empty column is converted to null*/
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetDescribeWithResolvedObjectType(
            String initializer, String sObjectTypeReturnedFrom) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       SObjectType sObjType = "
                        + initializer
                        + ";\n"
                        + "       System.debug(sObjType);\n"
                        + "       System.debug(sObjType.getDescribe());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // sObjectType
        SObjectType sObjectType = (SObjectType) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));
        ApexValue<?> returnedFrom = sObjectType.getReturnedFrom().orElse(null);
        if (sObjectTypeReturnedFrom == null) {
            MatcherAssert.assertThat(returnedFrom, is(nullValue()));
        } else {
            MatcherAssert.assertThat(
                    returnedFrom.getClass().getSimpleName(), equalTo(sObjectTypeReturnedFrom));
        }

        // sObjType.getDescribe()
        DescribeSObjectResult describeSObjectResult =
                (DescribeSObjectResult) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(describeSObjectResult.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                equalTo("Account"));
        MatcherAssert.assertThat(
                describeSObjectResult.getReturnedFrom().get(), instanceOf(SObjectType.class));
    }

    @Test
    public void testGlobalDescribeGetDescribedWithUnresolvedObjectType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(SObjectType sObjType) {\n"
                        + "       System.debug(sObjType);\n"
                        + "       System.debug(sObjType.getDescribe());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // sObjectType
        SObjectType sObjectType = (SObjectType) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(sObjectType.getType().isPresent(), equalTo(false));
        MatcherAssert.assertThat(sObjectType.getReturnedFrom().isPresent(), equalTo(false));

        // sObjType.getDescribe()
        DescribeSObjectResult describeSObjectResult =
                (DescribeSObjectResult) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(describeSObjectResult.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                describeSObjectResult.getSObjectType().get(), equalTo(sObjectType));
        MatcherAssert.assertThat(
                describeSObjectResult.getReturnedFrom().get(), instanceOf(SObjectType.class));
    }

    @ValueSource(strings = {"Schema.getGlobalDescribe().get('Account')", "Account.SObjectType"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNewSObjectWithResolvedObjectType(String initializer) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       SObjectType sObjType = "
                        + initializer
                        + ";\n"
                        + "       System.debug(sObjType.newSObject());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // sObjType.newSObject()
        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexSingleValue.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                apexSingleValue.getReturnedFrom().get(), instanceOf(SObjectType.class));

        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));
    }

    @Test
    public void testNewSObjectWithUnresolvedObjectType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(SObjectType sObjType) {\n"
                        + "       System.debug(sObjType.newSObject());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // sObjType.newSObject()
        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexSingleValue.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                apexSingleValue.getReturnedFrom().get(), instanceOf(SObjectType.class));

        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(sObjectType.getType().isPresent(), equalTo(false));
        MatcherAssert.assertThat(sObjectType.getReturnedFrom().isPresent(), equalTo(false));
    }
}
