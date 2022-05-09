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

public class ApexCustomValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testCustomFieldAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       MySettings__c ms = MySettings__c.getOrgDefaults();\n"
                    + "       ms.MyInt__c = 10;\n"
                    + "       ms.MyBool__c = true;\n"
                    + "       System.debug(ms.MyInt__c);\n"
                    + "       System.debug(ms.MyBool__c);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexIntegerValue integerValue = (ApexIntegerValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(integerValue.getValue().get(), equalTo(10));

        ApexBooleanValue booleanValue = (ApexBooleanValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(booleanValue.getValue().get(), equalTo(true));
    }
}
