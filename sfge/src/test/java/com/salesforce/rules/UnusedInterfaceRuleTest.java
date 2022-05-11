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

public class UnusedInterfaceRuleTest {
    private static final Logger LOGGER = LogManager.getLogger(UnusedInterfaceRuleTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleInterface() {
        String unimplInterfaceSource =
                "public interface UnimplInterface {\n"
                        + "	boolean boolMethod();\n"
                        + "	integer intMethod();\n"
                        + "}";
        String implInterfaceSource =
                "public interface ImplInterface {\n"
                        + "	boolean boolMethod();\n"
                        + "	integer intMethod();\n"
                        + "}";
        String interfaceUserSource =
                "public class InterfaceUser implements ImplInterface{\n"
                        + "	public boolean boolMethod() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "	private integer intMethod() {\n"
                        + "		return 16;\n"
                        + "	}\n"
                        + "}";

        TestUtil.buildGraph(
                g,
                new String[] {unimplInterfaceSource, implInterfaceSource, interfaceUserSource},
                false);

        StaticRule rule = UnusedInterfaceRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(1)));
        Violation.RuleViolation violation = (Violation.RuleViolation) violations.get(0);
        assertEquals("UnimplInterface", violation.getSourceVertex().getDefiningType());
    }

    @Test
    public void testExtendedInterface() {
        // This should not count as unused, because it's extended by another interface.
        String parentInterfaceSource =
                "public interface ParentInterface {\n"
                        + "	boolean boolMethod();\n"
                        + "	integer intMethod();\n"
                        + "}";

        // This should count as unused, because nothing extends or implements it.
        String unimplChildInterfaceSource =
                "public interface UnimplChildInterface extends ParentInterface {\n"
                        + "	boolean boolMethod2();\n"
                        + "	integer intMethod2();\n"
                        + "}";

        // This should not count as unused, because it's implemented by another class.
        String implChildInterfaceSource =
                "public interface ImplChildInterface extends ParentInterface {\n"
                        + "	boolean boolMethod2();\n"
                        + "	integer intMethod2();\n"
                        + "}";

        String interfaceUserSource =
                "public class InterfaceUser implements ImplChildInterface {\n"
                        + "	public boolean boolMethod() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "	public boolean boolMethod2() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "	public integer intMethod() {\n"
                        + "		return 15;\n"
                        + "	}\n"
                        + "	public integer intMethod2() {\n"
                        + "		return 17;\n"
                        + "	}\n"
                        + "}";

        TestUtil.buildGraph(
                g,
                new String[] {
                    parentInterfaceSource,
                    unimplChildInterfaceSource,
                    implChildInterfaceSource,
                    interfaceUserSource
                },
                false);

        StaticRule rule = UnusedInterfaceRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(1)));
        Violation.RuleViolation violation = (Violation.RuleViolation) violations.get(0);
        assertEquals("UnimplChildInterface", violation.getSourceVertex().getDefiningType());
    }

    @Test
    public void testInnerInterfaces() {
        // This class declares four inner interfaces, and an inner class that implements one of
        // them.
        String innerInterfaceSource =
                "public class HasInnerInterface {\n"
                        + "\n"
                        +
                        // This interface is implemented later in this class.
                        "	public interface UsedInnerInterface {\n"
                        + "		boolean boolMethod1();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This interface is implemented in another class.
                        "	public interface CrossClassInnerInterface {\n"
                        + "		boolean boolMethod2();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This interface isn't implemented anywhere.
                        "	public interface UnusedInnerInterface {\n"
                        + "		boolean boolMethod3();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This interface has the same name as an outer interface declared
                        // elsewhere. This one isn't used anywhere.
                        "	public interface CollidingInterface {\n"
                        + "		boolean boolMethod4();\n"
                        + "	}\n"
                        + "\n"
                        +
                        // This is the implementation of UsedInnerInterface.
                        "	public class InnerInterfaceUser implements UsedInnerInterface {\n"
                        + "		public boolean boolMethod1() {\n"
                        + "			return true;\n"
                        + "		}\n"
                        + "	}\n"
                        + "}";
        // This class implements one of the inner interfaces from the previous class.
        String crossClassImplementerSource =
                "public class CrossClassImplementer implements HasInnerInterface.CrossClassInnerInterface {\n"
                        + "	public boolean boolMethod2() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}";
        // This interface has the same name as one of the inner interfaces declared earlier. This
        // one is used.
        String collidingOuterInterfaceSource =
                "public interface CollidingInterface {\n" + "	boolean boolMethod5();\n" + "}";
        // This class implements the outer interface version of CollidingInterface.
        String collidingImplementerSource =
                "public class CollidingImplementer implements CollidingInterface {\n"
                        + "	public boolean boolMethod5() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}";

        TestUtil.buildGraph(
                g,
                new String[] {
                    innerInterfaceSource,
                    crossClassImplementerSource,
                    collidingOuterInterfaceSource,
                    collidingImplementerSource
                },
                false);

        StaticRule rule = UnusedInterfaceRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(2)));
        for (Violation violation : violations) {
            Violation.RuleViolation ruleViolation = (Violation.RuleViolation) violation;
            if (ruleViolation
                    .getSourceVertex()
                    .getDefiningType()
                    .equals("HasInnerInterface.UnusedInnerInterface")) {
                assertEquals(11, ruleViolation.getSourceLineNumber());
            } else if (ruleViolation
                    .getSourceVertex()
                    .getDefiningType()
                    .equals("HasInnerInterface.CollidingInterface")) {
                assertEquals(15, ruleViolation.getSourceLineNumber());
            } else {
                // This only happens if we get an unexpected violation.
                assertEquals(true, false);
            }
        }
    }
}
