package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SObjectFieldTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testDirectSObjectField() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       SObjectField sObjField = Schema.Account.Fields.Name;\n"
                        + "       System.debug(sObjField);\n"
                        + "       System.debug(sObjField.getDescribe());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // sObjectField
        SObjectField sObjectField = (SObjectField) visitor.getAllResults().get(0).get();
        assertThat(sObjectField.isIndeterminant(), equalTo(false));
        assertThat(TestUtil.apexValueToString(sObjectField.getFieldname()), equalTo("Name"));

        // sObjField.getDescribe()
        DescribeFieldResult describeFieldResult =
                (DescribeFieldResult) visitor.getAllResults().get(1).get();
        assertThat(describeFieldResult.isIndeterminant(), equalTo(false));
        assertThat(
                TestUtil.apexValueToString(describeFieldResult.getSObjectType()),
                equalTo("Account"));
        assertThat(describeFieldResult.getReturnedFrom().get(), instanceOf(SObjectField.class));
    }
}
