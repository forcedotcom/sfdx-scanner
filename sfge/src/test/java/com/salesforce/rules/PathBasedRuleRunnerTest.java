package com.salesforce.rules;

import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathBasedRuleRunnerTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        g = TestUtil.getGraph();
    }

    @Test
    public void pathsWithoutInterestingVerticesAreIgnored() {
        // This code defines a method that returns a PageReference, but does nothing interesting.
        String sourceCode =
                "public class MyClass {\n"
                        + "	public PageReference foo(boolean arg) {\n"
                        + "		if (arg) {\n"
                        + "			return null;\n"
                        + "		} else {\n"
                        + "			return null;\n"
                        + "		}\n"
                        + "	}\n"
                        + "}";
        TestUtil.buildGraph(g, sourceCode, true);

        // Get the vertex corresponding to the method, and an instance of the FLS rule in a
        // singleton list.
        MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        List<AbstractPathBasedRule> rules =
                Collections.singletonList(ApexFlsViolationRule.getInstance());

        // Define a PathBasedRuleRunner to apply the rule against the method vertex.
        PathBasedRuleRunner runner = new PathBasedRuleRunner(g, rules, methodVertex);

        Set<Violation> violations = runner.runRules();
        MatcherAssert.assertThat(violations, hasSize(0));
    }
}
