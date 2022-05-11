package com.salesforce.graph.symbols.apex.system;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexGlobalDescribeMapValue;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemSchemaTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testGetGlobalDescribe() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug(Schema.getGlobalDescribe());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexGlobalDescribeMapValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getReturnedFrom().get(), instanceOf(SystemSchema.class));
    }

    /**
     * The resulting list should give answers for its methods because we know exactly which objects
     * were asked for
     */
    @Test
    public void testDescribeSObjectsWithInlineParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<Schema.DescribeSObjectResult> describeList = Schema.describeSObjects(new String[]{'Account','Contact'});\n"
                        + "       System.debug(describeList);\n"
                        + "       System.debug(describeList.size());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // describeList
        ApexListValue describeList = (ApexListValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(describeList.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(describeList.getValues(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                describeList.getReturnedFrom().get(), instanceOf(SystemSchema.class));

        // describeList.size()
        ApexIntegerValue size = (ApexIntegerValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(size.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
        MatcherAssert.assertThat(size.getReturnedFrom().get(), instanceOf(ApexListValue.class));
    }

    /**
     * The resulting list should give answers for its methods because we know exactly which objects
     * were asked for
     */
    @Test
    public void testDescribeSObjectsWithVariableParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String[] objectTypes = new String[]{'Account','Contact'};\n"
                        + "       List<Schema.DescribeSObjectResult> describeList = Schema.describeSObjects(objectTypes);\n"
                        + "       System.debug(describeList);\n"
                        + "       System.debug(describeList.size());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // describeList
        ApexListValue describeList = (ApexListValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(describeList.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(describeList.getValues(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                describeList.getReturnedFrom().get(), instanceOf(SystemSchema.class));

        // describeList.size()
        ApexIntegerValue size = (ApexIntegerValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(size.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
        MatcherAssert.assertThat(size.getReturnedFrom().get(), instanceOf(ApexListValue.class));
    }

    /**
     * The resulting list should give indeterminant answers for its methods because we don't know
     * which objects were asked for
     */
    @Test
    public void testDescribeSObjectsWithUnresolvedMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String[] objectTypes) {\n"
                        + "       List<Schema.DescribeSObjectResult> describeList = Schema.describeSObjects(objectTypes);\n"
                        + "       System.debug(describeList);\n"
                        + "       System.debug(describeList.size());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // describeList
        ApexListValue describeList = (ApexListValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(describeList.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(describeList.getValues(), hasSize(equalTo(0)));
        MatcherAssert.assertThat(
                describeList.getReturnedFrom().get(), instanceOf(SystemSchema.class));

        // describeList.size()
        ApexIntegerValue size = (ApexIntegerValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(size.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(size.getValue().isPresent(), equalTo(false));
        MatcherAssert.assertThat(size.getReturnedFrom().get(), instanceOf(ApexListValue.class));
    }
}
