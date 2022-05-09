package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests paths that are collapsible using only the {@link SyntheticResultReturnValuePathCollapser}
 */
public class SyntheticResultReturnValuePathCollapserTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    // TODO: Remove NPSP
    // Copied from github.com/SalesforceFoundation/NPSP
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDescribeSObjectResultCollapser(boolean withCollapsers) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeSObjectResult result = DescribeSingleton.getObjectDescribe(MyObject__c.SObjectType);\n"
                    + "       System.debug(result);\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       if (objectDescribesByType == null || !objectDescribesByType.containsKey(objType)) {\n"
                    + "           fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       }\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   static void fillMapsForObject(string objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       if (gd == null) {\n"
                    + "           gd = Schema.getGlobalDescribe();\n"
                    + "       }\n"
                    + "       if (gd.containsKey(objectName)) {\n"
                    + "           if (!objectDescribes.containsKey(objectName)) {\n"
                    + "               Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "               objectDescribes.put(objectName, objDescribe);\n"
                    + "               objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "           }\n"
                    + "       } else {\n"
                    + "           throw new SchemaDescribeException('Invalid object name \\'' + objectName + '\\'');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        ApexPathExpanderConfig expanderConfig;
        if (withCollapsers) {
            expanderConfig = getApexPathExpanderConfig();
        } else {
            expanderConfig = ApexPathUtil.getSimpleExpandingConfig();
        }

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();

        if (withCollapsers) {
            MatcherAssert.assertThat(results, hasSize(equalTo(1)));
            for (TestRunner.Result<SystemDebugAccumulator> result : results) {
                SystemDebugAccumulator visitor = result.getVisitor();
                DescribeSObjectResult value = visitor.getSingletonResult();
                MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
                MatcherAssert.assertThat(
                        value.getCanonicalType(), equalTo("Schema.DescribeSObjectResult"));
                // This was lower cased in the fillMapsForObject method
                MatcherAssert.assertThat(
                        TestUtil.apexValueToString(value.getSObjectType()),
                        equalTo("MyObject__c".toLowerCase()));
            }
        } else {
            MatcherAssert.assertThat(results, hasSize(equalTo(5)));
            // TODO: More inspection of invalid results
        }
    }

    /** This tests that maps which return indeterminant values aren't collapsed */
    @Test
    public void testIndeterminantTestsAreNotCollapsed() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String sObjectType) {\n"
                    + "       Schema.DescribeSObjectResult dr = MyOtherClass.getObjectDescribe(sObjectType);\n"
                    + "       Boolean isSearchAble = dr.isSearchable();\n"
                    + "       if (isSearchable) {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "       System.debug(isSearchable);\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   private static Map<String, Schema.DescribeSObjectResult> objectDescribes = new Map<String, Schema.DescribeSObjectResult>();\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(String objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       fillMap(objectName);\n"
                    + "       return objectDescribes.get(objectName);\n"
                    + "   }\n"
                    + "   public static void fillMap(String objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       Schema.DescribeSObjectResult dr = Schema.getGlobalDescribe().get(objectName).getDescribe();\n"
                    + "       objectDescribes.put(objectName, dr);\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        // Walk both paths, ensuring that each path was traversed and "isSearchable" is an
        // ApexBooleanValue for each
        List<String> branches = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));
            branches.add(TestUtil.apexValueToString(visitor.getAllResults().get(0).get()));
            ApexBooleanValue booleanValue = (ApexBooleanValue) visitor.getAllResults().get(1).get();
            MatcherAssert.assertThat(booleanValue.isDeterminant(), equalTo(false));
            MatcherAssert.assertThat(booleanValue.isIndeterminant(), equalTo(true));
        }
        MatcherAssert.assertThat(branches, containsInAnyOrder("ifBranch", "elseBranch"));
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        return ApexPathExpanderConfig.Builder.get()
                .expandMethodCalls(true)
                .with(SyntheticResultReturnValuePathCollapser.getInstance())
                .build();
    }

    private TestRunner.Result<SystemDebugAccumulator> walkPath(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPath();
    }

    private List<TestRunner.Result<SystemDebugAccumulator>> walkPaths(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPaths();
    }
}
