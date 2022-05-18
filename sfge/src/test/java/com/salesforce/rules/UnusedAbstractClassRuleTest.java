package com.salesforce.rules;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnusedAbstractClassRuleTest {
    private static final Logger LOGGER = LogManager.getLogger(UnusedAbstractClassRuleTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleAbstract() {
        String unusedAbstractSource =
                "public abstract class UnusedAbstract {\n"
                        + "	public abstract boolean abstractMethod1();\n"
                        + "}";

        String usedAbstractSource =
                "public abstract class UsedAbstract {\n"
                        + "	public abstract boolean abstractMethod1();\n"
                        + "}";

        String abstractExtenderSource =
                "public class AbstractExtender extends UsedAbstract {\n"
                        + "	public override boolean abstractMethod1() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}";

        TestUtil.buildGraph(
                g,
                new String[] {unusedAbstractSource, usedAbstractSource, abstractExtenderSource},
                false);

        UnusedAbstractClassRule rule = UnusedAbstractClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(1)));
        Violation.RuleViolation violation = (Violation.RuleViolation) violations.get(0);
        assertEquals("UnusedAbstract", violation.getSourceVertex().getDefiningType());
    }

    @Test
    public void testInnerAbstract() {
        // THis class declares four inner abstract classes, and another inner class that extends one
        // of them.
        String innerAbstractSource =
                "public class HasInnerAbstract {\n"
                        +
                        // This abstract class is extended later in this class.
                        "	public abstract class UsedInnerAbstract {\n"
                        + "		public abstract boolean boolMethod1();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This abstract class is extended in a different file.
                        "	public abstract class CrossClassInnerAbstract {\n"
                        + "		public abstract boolean boolMethod2();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This abstract class is unused and should violate the rule.
                        "	public abstract class UnusedInnerAbstract {\n"
                        + "		public abstract boolean boolMethod3();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // Thus abstract class has the same name as an outer class declared
                        // somewhere else. This one isn't used anywhere.
                        "	public abstract class CollidingAbstract {\n"
                        + "		public abstract boolean boolMethod4();\n"
                        + "	}\n"
                        +
                        // Ths is the aforementioned extension of UsedInnerAbstract.
                        "	public class InnerAbstractUser extends UsedInnerAbstract {\n"
                        + "		public override boolean boolMethod1() {\n"
                        + "			return true;\n"
                        + "		}\n"
                        + "	}\n"
                        + "}";
        // This class extends one of the inner abstract classes declared earlier.
        String crossClassExtenderSource =
                "public class CrossClassExtender extends HasInnerAbstract.CrossClassInnerAbstract {\n"
                        + "	public override boolean boolMethod2() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}";
        // This class has the same name as one of the inner abstract classes declared elsewhere.
        // This one is used.
        String collidingOuterAbstractSource =
                "public abstract class CollidingAbstract {\n"
                        + "	public abstract boolean boolMethod5();\n"
                        + "}";
        // This class extends the outer version of CollidingAbstract.
        String collidingExtenderSource =
                "public class CollidingExtender extends CollidingAbstract {\n"
                        + "	public override boolean boolMethod5() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}";

        TestUtil.buildGraph(
                g,
                new String[] {
                    innerAbstractSource,
                    crossClassExtenderSource,
                    collidingOuterAbstractSource,
                    collidingExtenderSource
                },
                false);

        UnusedAbstractClassRule rule = UnusedAbstractClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(2)));
        for (Violation violation : violations) {
            Violation.RuleViolation ruleViolation = (Violation.RuleViolation) violation;
            if (ruleViolation
                    .getSourceVertex()
                    .getDefiningType()
                    .equals("HasInnerAbstract.UnusedInnerAbstract")) {
                assertEquals(10, ruleViolation.getSourceLineNumber());
            } else if (ruleViolation
                    .getSourceVertex()
                    .getDefiningType()
                    .equals("HasInnerAbstract.CollidingAbstract")) {
                assertEquals(14, ruleViolation.getSourceLineNumber());
            } else {
                // This only happens if we get an unexpected violation.
                assertEquals(true, false);
            }
        }
    }
}
