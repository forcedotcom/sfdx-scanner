package com.salesforce.graph.ops.expander;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StackDepthLimitTest {
    private GraphTraversalSource g;
    private static final int STACK_DEPTH_LIMIT_OVERRIDE = 10;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
        SfgeConfigTestProvider.set(
                new TestSfgeConfig() {
                    @Override
                    public int getStackDepthLimit() {
                        return STACK_DEPTH_LIMIT_OVERRIDE;
                    }
                });
    }

    @AfterEach
    public void cleanUp() {
        SfgeConfigTestProvider.remove();
    }

    private static final String METHOD_NAME_BASE = "invokeLevel";
    public static final String METHOD_INVOKE_LEVEL_TEMPLATE =
            "  void "
                    + METHOD_NAME_BASE
                    + "%d() {\n"
                    + "       "
                    + METHOD_NAME_BASE
                    + "%d();\n"
                    + "   }\n";

    @CsvSource({
        "ExceedsLimit, " + (STACK_DEPTH_LIMIT_OVERRIDE + 2) + ", false",
        "WithinLimit, " + (STACK_DEPTH_LIMIT_OVERRIDE - 2) + ", true"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testStackDepth(
            String testName, int stackDepthToCreate, boolean expectToReachSystemDebug) {
        // Since we already create two methods other than the dynamic ones, subtract them from
        // required count
        final int depthCountToAdd = stackDepthToCreate - 2;
        final String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       "
                        + METHOD_NAME_BASE
                        + depthCountToAdd
                        + "();\n"
                        + "   }\n"
                        + getInvocationDepthSource(depthCountToAdd)
                        + "   void "
                        + METHOD_NAME_BASE
                        + "1() {\n"
                        + "       printData();\n"
                        + "   }\n"
                        + "   public void printData() {\n"
                        + "       System.debug('hi');\n"
                        + "   }\n"
                        + "}\n";

        if (expectToReachSystemDebug) {
            final TestRunner.Result<SystemDebugAccumulator> result =
                    TestRunner.walkPath(g, sourceCode);
            final SystemDebugAccumulator visitor = result.getVisitor();

            final ApexStringValue stringValue = visitor.getResult(0);
            assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
        } else {
            // TODO: this should be handled differently when stack depth rule is added
            List<TestRunner.Result<PathVertexVisitor>> paths =
                    TestRunner.get(g, sourceCode).walkPaths();
            // No paths should be found since the only path hit the stack depth limit.
            assertThat(paths.size(), equalTo(0));
        }
    }

    private static String getInvocationDepthSource(int depthCount) {
        final StringBuilder builder = new StringBuilder();
        int counter = depthCount;
        while (counter > 1) {
            builder.append(String.format(METHOD_INVOKE_LEVEL_TEMPLATE, counter, --counter));
        }

        return builder.toString();
    }

    @Test
    public void testChainedCallShouldNotAddToStackDepth() {
        String selfReferenceMethodName = "level";
        int count = STACK_DEPTH_LIMIT_OVERRIDE + 2;

        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String myString = "
                        + getSelfReferenceMethodCalls(selfReferenceMethodName, count)
                        + "getValue();\n"
                        + "       System.debug(myString);\n"
                        + "   }\n"
                        + "   public String getValue() {\n"
                        + "       return 'hi';\n"
                        + "   }\n"
                        + getSelfReferenceMethods(selfReferenceMethodName, count)
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexStringValue stringValue = visitor.getResult(0);
        assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testComplexBranchingAndStackDepthWithinLimit() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       if (checkDelete()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public Boolean checkDelete() {\n"
                    + "       return PermissionsSingleton.getInstance().canDelete(MyObject__c.SObjectType);\n"
                    + "   }\n"
                    + "}",
            "public class PermissionsSingleton {\n"
                    + "   private static PermissionsSingleton singleton;\n"
                    + "   public static PermissionsSingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new PermissionsSingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "   }\n"
                    + "   public Boolean canDelete(SObjectType sObjectType) {\n"
                    + "       return DescribeSingleton.getObjectDescribe(sObjectType).isDeletable();\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   private static void fillMapsForObject(string objectName) {\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<PathVertexVisitor>> paths =
                TestRunner.get(g, sourceCode).walkPaths();
        assertThat(paths.size(), equalTo(2));
    }

    private static String getSelfReferenceMethods(String methodName, int count) {
        int index = 0;
        final StringBuilder builder = new StringBuilder();
        while (index < count) {
            builder.append(
                    String.format(
                            "   public MyClass %s%d() {\n" + "       return this;\n" + "   }\n",
                            methodName, index++));
        }
        return builder.toString();
    }

    private static String getSelfReferenceMethodCalls(String methodName, int count) {
        int index = 0;
        final StringBuilder builder = new StringBuilder();
        while (index < count) {
            builder.append(String.format("%s%d().", methodName, index++));
        }
        return builder.toString();
    }
}
