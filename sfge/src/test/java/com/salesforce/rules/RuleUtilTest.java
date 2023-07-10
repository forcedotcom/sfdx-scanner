package com.salesforce.rules;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.salesforce.TestUtil;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuleUtilTest {
    private static final Logger LOGGER = LogManager.getLogger(RuleUtilTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void getAllRules_noExceptionThrown() {
        try {
            List<AbstractRule> allRules = RuleUtil.getEnabledRules();
            MatcherAssert.assertThat("Wrong number of rules returned. Did you add any?", allRules, hasSize(6));
            assertTrue(allRules.contains(ApexFlsViolationRule.getInstance()));
        } catch (Exception ex) {
            fail("Unexpected " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Test
    public void getRule_RealRuleReturned() {
        try {
            AbstractRule realRule = RuleUtil.getRule(UnimplementedTypeRule.class.getSimpleName());
            MatcherAssert.assertThat(realRule, not(nullValue()));
        } catch (RuleUtil.RuleNotFoundException rnfe) {
            fail("No exception should be thrown when a real rule is requested");
        }
    }

    @Test
    public void getRule_FakeRuleThrowsErr() {
        try {
            AbstractRule fakeRule = RuleUtil.getRule("DefinitelyAFakeRule");
            fail("Exception should have been thrown when a non-existent rule is requested");
        } catch (RuleUtil.RuleNotFoundException rnfe) {
            // Nothing is needed in this catch block, since merely entering it confirms the test.
        }
    }
}
