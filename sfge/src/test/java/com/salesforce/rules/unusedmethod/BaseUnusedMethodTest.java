package com.salesforce.rules.unusedmethod;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.AbstractStaticRule;
import com.salesforce.rules.UnusedMethodRule;
import com.salesforce.rules.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;

public class BaseUnusedMethodTest {
    protected GraphTraversalSource g;

    /* =============== SETUP METHODS =============== */

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /* =============== ASSERT VIOLATIONS =============== */
    // TODO: LONG-TERM, WE MAY WANT TO MODULARIZE THESE AND PUT THEM IN ANOTHER CLASS FOR REUSE.

    /**
     * Assert that each of the provided method names corresponds to a method that threw a violation.
     *
     * @param sourceCode - A single source file
     */
    protected void assertViolations(String sourceCode, String... methodNames) {
        assertViolations(new String[] {sourceCode}, methodNames);
    }

    /**
     * Assert that each of the provided method names corresponds to a method that threw a violation.
     *
     * @param sourceCodes - An array of source files
     */
    protected void assertViolations(String[] sourceCodes, String... methodNames) {
        List<Consumer<Violation.RuleViolation>> assertions = new ArrayList<>();

        for (int i = 0; i < methodNames.length; i++) {
            final int idx = i;
            assertions.add(
                    v -> {
                        assertEquals(methodNames[idx], v.getSourceVertexName());
                    });
        }
        assertViolations(sourceCodes, assertions.toArray(new Consumer[] {}));
    }

    /**
     * Assert that violations were generated that match the provided checks.
     *
     * @param sourceCode - A source file.
     * @param assertions - One or more consumers that perform assertions. The n-th consumer is
     *     applied to the n-th violation.
     */
    protected void assertViolations(
            String sourceCode, Consumer<Violation.RuleViolation>... assertions) {
        assertViolations(new String[] {sourceCode}, assertions);
    }

    /**
     * Assert that violations were generated that match the provided checks.
     *
     * @param sourceCodes - An array of source files.
     * @param assertions - One or more consumers that perform assertions. The n-th consumer is
     *     applied to the n-th violation.
     */
    protected void assertViolations(
            String[] sourceCodes, Consumer<Violation.RuleViolation>... assertions) {
        TestUtil.buildGraph(g, sourceCodes, true);

        AbstractStaticRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(assertions.length)));
        for (int i = 0; i < assertions.length; i++) {
            assertions[i].accept((Violation.RuleViolation) violations.get(i));
        }
    }

    /* =============== ASSERT NO VIOLATIONS =============== */
    /**
     * Assert that the expected number of methods were analyzed, and all determined to be used.
     *
     * @param sourceCode - A source file.
     */
    protected void assertNoViolations(String sourceCode, int eligibleMethodCount) {
        assertNoViolations(new String[] {sourceCode}, eligibleMethodCount);
    }

    /**
     * Assert that the expected number of methods were analyzed, and all determined to be used.
     *
     * @param sourceCodes - An array of source files.
     */
    protected void assertNoViolations(String[] sourceCodes, int eligibleMethodCount) {
        TestUtil.buildGraph(g, sourceCodes, true);

        UnusedMethodRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, empty());
        Set<MethodVertex> eligibleMethods = rule.getEligibleMethods();
        assertEquals(eligibleMethods.size(), eligibleMethodCount);
    }

    /* =============== ASSERT NO ANALYSIS ATTEMPT =============== */

    protected void assertNoAnalysis(String sourceCode) {
        assertNoAnalysis(new String[] {sourceCode});
    }

    protected void assertNoAnalysis(String[] sourceCodes) {
        TestUtil.buildGraph(g, sourceCodes, true);
        UnusedMethodRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, empty());
        MatcherAssert.assertThat(rule.getEligibleMethods(), empty());
    }
}
