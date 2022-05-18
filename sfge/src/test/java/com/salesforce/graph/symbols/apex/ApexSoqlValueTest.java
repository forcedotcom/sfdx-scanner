package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexSoqlValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /** Verify a chained soql query that calls a method on the resulting list */
    @Test
    public void testChainedSoqlMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug([SELECT Id FROM SomeSetting__c WHERE Active__c = true].isEmpty());\n"
                    + "       System.debug(![SELECT Id FROM SomeSetting__c WHERE Active__c = true].isEmpty());\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexBooleanValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }
}
