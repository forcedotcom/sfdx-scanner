package com.salesforce.graph.build;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MethodUtilStandardLibraryTest {
    private static final Logger LOGGER = LogManager.getLogger(MethodUtilTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSObjectTypeMatching() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static doSomething() {\n"
                    + "       callMethod(Schema.SObjectType.Account);\n"
                    + "   }\n"
                    + "   static void callMethod(SObjectType myObj) {\n"
                    + "       System.debug('hello');\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("hello"));
    }
}
