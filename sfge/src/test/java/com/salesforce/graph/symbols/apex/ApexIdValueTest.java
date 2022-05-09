package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexIdValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testValueOf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(Id.valueOf(s));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIdValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(true));

        ApexStringValue s = (ApexStringValue) value.getValue().get();
        MatcherAssert.assertThat(s.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testGetSObjectType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug(Id.valueOf(s).getSObjectType());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectType sObjectType = visitor.getSingletonResult();
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));

        ApexIdValue value = (ApexIdValue) sObjectType.getReturnedFrom().get();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(true));
    }

    @Test
    public void testCastStringToId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       Id asId = (Id)s;\n"
                        + "		System.debug(asId);\n"
                        + "		Schema.SObjectType objType = asId.getSObjectType();\n"
                        + "		System.debug(objType);\n"
                        + "		Schema.DescribeSObjectResult dr = objType.getDescribe();\n"
                        + "		System.debug(dr);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexIdValue id = visitor.getResult(0);
        // This is indeterminant because an it could possibly be null and influence an if/else path
        MatcherAssert.assertThat(id.isIndeterminant(), equalTo(true));

        // These are determinant because we know they are non-null and their influence on an if/else
        // can be correctly
        // determined
        SObjectType sObjectType = visitor.getResult(1);
        MatcherAssert.assertThat(sObjectType.isIndeterminant(), equalTo(false));

        DescribeSObjectResult describeSObjectResult = visitor.getResult(2);
        MatcherAssert.assertThat(describeSObjectResult.isIndeterminant(), equalTo(false));
    }
}
