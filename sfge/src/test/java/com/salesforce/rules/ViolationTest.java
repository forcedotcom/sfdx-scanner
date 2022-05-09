package com.salesforce.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ViolationTest {
    private static final String SOURCE_CODE =
            "public class MyClass {\n"
                    + "	public Integer foo() {\n"
                    + "		return 15 + 12 + 78;\n"
                    + "	}\n"
                    + "}";

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        g = TestUtil.getGraph();
    }

    @Test
    public void compareTo_twoIdenticalStaticRuleViolations() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);
        // Use the same vertex, rule, and message to instantiate two RuleViolations.
        final String message = "Some test message, this does not matter";
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();

        final Violation.RuleViolation violation1 =
                new Violation.StaticRuleViolation(message, methodVertex);
        violation1.setPropertiesFromRule(rule);

        final Violation.RuleViolation violation2 =
                new Violation.StaticRuleViolation(message, methodVertex);
        violation2.setPropertiesFromRule(rule);

        // Any comparison should return 0, since they're identical in all ways.
        assertEquals(0, violation1.compareTo(violation2));
        assertEquals(0, violation2.compareTo(violation1));
    }

    @Test
    public void compareTo_twoIdenticalInternalErrorViolations() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);
        // Use the same vertex, and message to instant two InternalErrorViolations.
        final String message = "Some test message. This does not matter.";
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");

        final Violation.InternalErrorViolation violation1 =
                new Violation.InternalErrorViolation(message, methodVertex);
        final Violation.InternalErrorViolation violation2 =
                new Violation.InternalErrorViolation(message, methodVertex);

        // Any comparison should return 0, since they're identical in all ways.
        assertEquals(0, violation1.compareTo(violation2));
        assertEquals(0, violation2.compareTo(violation1));
    }

    @Test
    public void compareTo_twoDifferentStaticRuleViolations() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same vertex and rule to create two violations, but give them different messages.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();

        final Violation.RuleViolation violation1 =
                new Violation.StaticRuleViolation("Message one", methodVertex);
        violation1.setPropertiesFromRule(rule);
        final Violation.RuleViolation violation2 =
                new Violation.StaticRuleViolation("Message two", methodVertex);
        violation2.setPropertiesFromRule(rule);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(violation1.compareTo(violation2) > 0 ^ violation2.compareTo(violation1) > 0);
    }

    @Test
    public void compareTo_staticVsPathBasedRuleViolation() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same vertex and rule to create two violations. Make one static and one
        // path-based.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();

        final Violation.RuleViolation staticViolation =
                new Violation.StaticRuleViolation("Shared message", methodVertex);
        staticViolation.setPropertiesFromRule(rule);
        // The same vertex can be both source and sink. It doesn't matter terribly.
        final Violation.RuleViolation pathBasedViolation =
                new Violation.PathBasedRuleViolation("Shared message", methodVertex, methodVertex);
        pathBasedViolation.setPropertiesFromRule(rule);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(
                staticViolation.compareTo(pathBasedViolation) > 0
                        ^ pathBasedViolation.compareTo(staticViolation) > 0);
    }

    @Test
    public void compareTo_twoDifferentPathBasedViolations() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same source vertex and rule to create two path-based violations, but give them
        // different sink vertices.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();
        SFVertex returnVertex =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.RETURN_STATEMENT, 3);

        // For one, the sink is the method vertex. It's fine for the source and sink to be the same.
        final Violation.RuleViolation violation1 =
                new Violation.PathBasedRuleViolation("Shared message", methodVertex, methodVertex);
        violation1.setPropertiesFromRule(rule);
        // For the other, the sink is the return statement that ends the method in question.
        final Violation.RuleViolation violation2 =
                new Violation.PathBasedRuleViolation("Shared message", methodVertex, returnVertex);
        violation2.setPropertiesFromRule(rule);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(violation1.compareTo(violation2) > 0 ^ violation2.compareTo(violation1) > 0);
    }

    @Test
    public void compareTo_twoDifferentInternalErrorViolations() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same vertex to create two violations, but give them different messages.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");

        final Violation.InternalErrorViolation violation1 =
                new Violation.InternalErrorViolation("Message one", methodVertex);
        final Violation.InternalErrorViolation violation2 =
                new Violation.InternalErrorViolation("Message two", methodVertex);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(violation1.compareTo(violation2) > 0 ^ violation2.compareTo(violation1) > 0);
    }

    @Test
    public void compareTo_staticRuleViolationVsTimeoutViolation() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same message and vertex to create a rule violation and a timeout violation.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final String message = "This message does not matter";
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();

        final Violation.RuleViolation ruleViolation =
                new Violation.StaticRuleViolation(message, methodVertex);
        ruleViolation.setPropertiesFromRule(rule);
        final Violation.TimeoutViolation timeoutViolation =
                new Violation.TimeoutViolation(message, methodVertex);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(
                ruleViolation.compareTo(timeoutViolation) > 0
                        ^ timeoutViolation.compareTo(ruleViolation) > 0);
    }

    @Test
    public void compareTo_staticRuleViolationVsInternalErrorViolation() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same message and vertex to create a rule violation and an internal error
        // violation.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final String message = "This message does not matter";
        final ApexFlsViolationRule rule = ApexFlsViolationRule.getInstance();

        final Violation.RuleViolation ruleViolation =
                new Violation.StaticRuleViolation(message, methodVertex);
        ruleViolation.setPropertiesFromRule(rule);
        final Violation.InternalErrorViolation internalErrorViolation =
                new Violation.InternalErrorViolation(message, methodVertex);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(
                ruleViolation.compareTo(internalErrorViolation) > 0
                        ^ internalErrorViolation.compareTo(ruleViolation) > 0);
    }

    @Test
    public void compareTo_timeoutViolationVsInternalErrorViolation() {
        TestUtil.buildGraph(g, SOURCE_CODE, true);

        // Use the same message and vertex to create a timeout violation and an internal error
        // violation.
        final MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyClass", "foo");
        final String message = "This message does not matter";

        final Violation.TimeoutViolation timeoutViolation =
                new Violation.TimeoutViolation(message, methodVertex);
        final Violation.InternalErrorViolation internalErrorViolation =
                new Violation.InternalErrorViolation(message, methodVertex);

        // The violations can't *both* be greater than the other, so exactly one comparison should
        // be greater than 0.
        assertTrue(
                timeoutViolation.compareTo(internalErrorViolation) > 0
                        ^ internalErrorViolation.compareTo(timeoutViolation) > 0);
    }
}
