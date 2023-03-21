package com.salesforce.rules.unusedmethod;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractStaticRule;
import com.salesforce.rules.UnusedMethodRule;
import com.salesforce.rules.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;

public class BaseUnusedMethodTest {
    protected GraphTraversalSource g;

    /* ============== SOURCE CODE TEMPLATES ============== */
    /**
     * Template for an obviously unused method on an outer class. Has the following wildcards:
     *
     * <ol>
     *   <li>%s for method modifiers. (e.g., "public", "public static", etc).
     * </ol>
     */
    // spotless:off
    protected static final String SIMPLE_UNUSED_OUTER_METHOD_SOURCE =
        "global class MyClass {\n"
      + "    %s boolean unusedMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a class that defines a rule-eligible method and then uses it. Has the following
     * wildcards:
     *
     * <ol>
     *   <li>%s for modifiers to the tested method. (e.g., "public", "public static", etc).
     *   <li>%s for modifiers to the method that invokes the tested method. (e.g., "public", "public
     *       static", etc).
     *   <li>%s for the invocation of the tested method. (e.g., "this.method1()").
     * </ol>
     */
    // spotless:off
    protected static final String SIMPLE_USED_METHOD_SOURCE =
        "global class MyClass {\n"
        // Declare the tested method.
      + "    %s boolean method1() {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
        // Use the engine directive to prevent this method from tripping the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    %s boolean method2() {\n"
        // Invocation of the tested method.
      + "        return %s;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a parent class that defines a method, along with child and grandchild classes
     * that can use it. Has the following wildcards:
     *
     * <ol>
     *   <li>%s in Source 1 for modifiers to the scope of the method declared in the parent class.
     *       (E.g., "public", "public static", etc.)
     *   <li>%s in Source 2 for modifiers to the scope of the method declared in the child class.
     *       (E.g., "public", "public static", etc.)
     *   <li>%s in Source 2 for the value returned by the child method. (E.g., invocation of parent
     *       method, or a literal value.)
     *   <li>%s in Source 3 for modifiers to the scope of the method declared in the grandchild
     *       class. (E.g., "public", "public static", etc.)
     *   <li>%s in Source 3 for the value returned by the grandchild method. (E.g., invocation of
     *       parent/child method, or a literal value.)
     * </ol>
     */
    // spotless:off
    protected static final String[] SUBCLASS_NON_OVERRIDDEN_SOURCES = new String[]{
        // PARENT CLASS
        "global virtual class ParentClass {\n"
        // Declare a method on the parent class.
      + "    %s boolean methodOnParent() {\n"
      + "        return true;\n"
      + "    }\n"
      + "}",
        // CHILD CLASS
        "global virtual class ChildClass extends ParentClass {\n"
        // Declare a method on the child, with the engine directive to prevent it tripping the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    %s boolean methodOnChild() {\n"
        // Wildcard allows this method to either call the parent method or return a literal.
      + "        return %s;\n"
      + "    }\n"
      + "}",
        // GRANDCHILD CLASS
        "global class GrandchildClass extends ChildClass {\n"
        // Declare a method on the grandchild, with the engine directive to prevent it tripping the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    %s boolean methodOnGrandchild() {\n"
        // Wildcard allows this method to either call the parent method, child method, or return a literal.
      + "        return %s;\n"
      + "    }\n"
      + "}"
    };
    // spotless:on

    /**
     * Template for a parent class that defines a method, along with a child class that overrides
     * it. Each class has another method that can be configured as needed. Has the following
     * wildcards:
     *
     * <ol>
     *   <li>%s in Source 1 for modifiers to the declaration of the parent class (e.g
     *       "abstract/virtual").
     *   <li>%s in Source 1 to allow injection of engine directive to exclude the parent's version
     *       of the method.
     *   <li>%s in Source 1 allowing the second method in the parent class to potentially call the
     *       overrideable method.
     *   <li>%s in Source 2 to allow injection of engine directive to exclude the child's version of
     *       the method.
     *   <li>%s in Source 2 to allow the second method in the child class to potentially call either
     *       version of the overrideable method.
     * </ol>
     */
    // spotless:off
    protected static final String[] SUBCLASS_OVERRIDDEN_SOURCES = new String[]{
        // PARENT CLASS, configurably abstract or virtual.
        "global %s class ParentClass {\n"
        // Define an overrideable, configurably excludeable method.
      + "    %s\n"
      + "    public virtual boolean getBool() {\n"
      + "        return true;\n"
      + "    }\n"
        // Also define a method that can be configured to invoke the overrideable method.
        // Annotate it to not trip the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    public boolean parentOptionalInvoker() {\n"
      + "        return %s;\n"
      + "    }\n"
      + "}",
        // CHILD CLASS
        "global class ChildClass extends ParentClass {\n"
        // define an override of the inherited method, configurably excludeable.
      + "    %s\n"
      + "    public override boolean getBool() {\n"
      + "        return false;\n"
      + "    }\n"
        // Also define a method that can be configured to invoke whatever method is needed.
        // Annotate it to not trip the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    public boolean childOptionalInvoker() {\n"
      + "        return %s;\n"
      + "    }\n"
      + "}"
    };
    // spotless:on

    /**
     * Template for a class that defines a method and invokes it via a property. Has the following
     * wildcards:
     *
     * <ol>
     *   <li>%s for modifiers to the scope of the property. (E.g., "public", "public static", etc.)
     *   <li>%s for the invocation of the tested method.
     *   <li>%s for modifiers to the scope of the tested method. (E.g., "public", "public static",
     *       etc.)
     * </ol>
     */
    // spotless:off
    protected static final String METHOD_INVOKED_AS_PROPERTY_SOURCE =
        "global class MyClass {\n"
        // Declare a property with the ability to invoke our tested method.
      + "    %s boolean boolProp = %s;\n"
      + "    \n"
        // Declare our tested method.
      + "    %s boolean testedMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a class that can configurably invoke a method on another class. Has the
     * following wildcards:
     *
     * <ol>
     *   <li>%s for the type of static property {@code staticProp}.
     *   <li>%s for the type of instance property {@code instanceProp}.
     *   <li>%s for the return type of static method {@code staticMethod()}.
     *   <li>%s for the return type of instance method {@code instanceMethod()}.
     *   <li>%s for the type of parameter {@code param} for method {@code invokeMethod()}.
     *   <li>%s for the type of variable {@code var} declared within method {@code invokeMethod()}.
     *   <li>%s for the return value of method {@code invokeMethod()}.
     * </ol>
     */
    // spotless:off
    protected static final String INVOKER_SOURCE =
        "global class InvokerClass {\n"
        // Add a static property of configurable type.
      + "    public static %s staticProp;\n"
        // Add an instance property of configurable type.
      + "    public %s instanceProp;\n"
        // Add a static method of configurable return type, annotated to not trip the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    public static %s staticMethod() {\n"
        // UnusedMethodRule won't care that the method returns null.
      + "        return null;\n"
      + "    }"
        // Add an instance method of configurable return type, annotated to not trip the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    public %s instanceMethod() {\n"
        // UnusedMethodRule won't care that the method returns null.
      + "        return null;\n"
      + "    }\n"
        // Declare a method annotated to not trip the rule, and with a parameter
        // of configurable type.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    public boolean invokeMethod(%s param)\n"
        // Declare a variable of configurable type. UnusedMethodRule won't care
        // that the variable is uninitialized.
      + "        %s var;\n"
      + "        return %s;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a class with two overloads of a method, and a third method that calls one
     * overload or the other. Has the following wildcards:
     *
     * <ol>
     *   <li>%s for modifiers to scope of first overload (e.g. "public", "public static", etc).
     *   <li>%s for parameters accepted by the first overload.
     *   <li>%s for modifiers to scope of second overload (e.g. "public", "public static", etc).
     *   <li>%s for parameters accepted by the second overload.
     *   <li>%s for modifiers to scope of overload invoker (e.g. "public", "public static", etc).
     *   <li>%s for arguments provided to overload invocation.
     * </ol>
     */
    // spotless:off
    protected static final String OVERLOADS_SOURCE =
        "global class MyClass {\n"
        // Declare two overloads of the same method.
      + "    %s boolean getBool(%s) {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
      + "    %s boolean getBool(%s) {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
        // Declare a method that invokes one overload or the other, annotated to not trip the rule.
      + "    /* sfge-disable-stack UnusedMethodRule */\n"
      + "    %s boolean callOverload() {\n"
      + "        return getBool(%s);\n"
      + "    }\n"
      + "}";
    // spotless:on

    /* ============== SETUP METHODS ============== */
    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /* ============== ASSERT VIOLATIONS ============== */
    // TODO: Refactoring opportunity. Long-term, we may want to modularize these methods and put
    // them in another class for re-use.

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
     * @param sourceCode - A source file
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
     * @param sourceCodes - An array of source files
     * @param assertions - One or more consumers that perform assertions. The n-th consumer is
     *     applied to the n-th violation.
     */
    protected void assertViolations(
            String[] sourceCodes, Consumer<Violation.RuleViolation>... assertions) {
        TestUtil.buildGraph(g, sourceCodes);

        AbstractStaticRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(assertions.length)));
        for (int i = 0; i < assertions.length; i++) {
            assertions[i].accept((Violation.RuleViolation) violations.get(i));
        }
    }

    /* ============== ASSERT NO VIOLATIONS ============== */

    /**
     * Assert that the expected number of methods were analyzed, and all were determined to be used.
     *
     * @param sourceCode - A source file
     */
    protected void assertNoViolations(String sourceCode, int eligibleMethodCount) {
        assertNoViolations(new String[] {sourceCode}, eligibleMethodCount);
    }

    /**
     * Assert that the expected number of methods were analyzed, and all were determined to be used.
     *
     * @param sourceCodes - An array of source files
     */
    protected void assertNoViolations(String[] sourceCodes, int eligibleMethodCount) {
        TestUtil.buildGraph(g, sourceCodes);

        UnusedMethodRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, empty());
        assertEquals(eligibleMethodCount, rule.getRuleStateTracker().getEligibleMethodCount());
    }

    /* ============== ASSERT NO ANALYSIS ATTEMPT ============== */
    protected void assertNoAnalysis(String sourceCode) {
        assertNoAnalysis(new String[] {sourceCode});
    }

    protected void assertNoAnalysis(String[] sourceCodes) {
        TestUtil.buildGraph(g, sourceCodes, true);
        UnusedMethodRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, empty());
        assertEquals(0, rule.getRuleStateTracker().getEligibleMethodCount());
    }
}
