package com.salesforce.rules.unusedmethod;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.rules.*;
import com.salesforce.rules.unusedmethod.operations.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;

public class BaseUnusedMethodTest {
    protected GraphTraversalSource g;

    /* ============== SOURCE CODE TEMPLATES ============== */

    /**
     * Template for a global class {@code MyEntrypoint} with a global boolean method {@code
     * entrypointMethod()}, for use as a simple entrypoint in tests that require one. Has the
     * following wildcards:
     *
     * <ol>
     *   <li>%s for the return value of {@code entrypointMethod}. E.g., {@code true} or {@code new
     *       Whatever().someMethod()}.
     * </ol>
     */
    // spotless:off
    protected static final String SIMPLE_ENTRYPOINT =
        "global class MyEntrypoint {\n"
      + "    global boolean entrypointMethod() {\n"
      + "        return %s;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a global class {@code MyEntrypoint} with a global boolean method {@code
     * entrypointMethod()}, for use as a more complicated entrypoint in tests that require one. Has
     * the following wildcards:
     *
     * <ol>
     *   <li>%s for the entire body of {@code entrypointMethod}. Input should end with returning a
     *       boolean, but the rest is up to you.
     * </ol>
     */
    // spotless:off
    protected static final String COMPLEX_ENTRYPOINT =
        "global class MyEntrypoint {\n"
      + "    global boolean entrypointMethod() {\n"
      + "%s"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Template for a simple class {@code MyClass} with boolean methods {@code testedMethod()} and
     * {@code entrypointMethod()}. Has the following wildcards.
     *
     * <ol>
     *   <li>%s for modifiers to {@code testedMethod()}, e.g. {@code public}, {@code public static},
     *       etc.
     *   <li>%s for modifiers to {@code entrypointMethod()}, e.g. {@code global}, {@code global
     *       static}, etc.
     *   <li>%s for the return value of {@code entrypointMethod}, e.g. {@code true}, {@code
     *       someMethod()}.
     *   <li>
     * </ol>
     */
    // spotless:off
    protected static final String SIMPLE_SOURCE =
        "global class MyClass {\n"
      + "    %s boolean testedMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
      + "    %s boolean entrypointMethod() {\n"
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
        // Declare a method on the child.
      + "    %s boolean methodOnChild() {\n"
        // Wildcard allows this method to either call the parent method or return a literal.
      + "        return %s;\n"
      + "    }\n"
      + "}",
        // GRANDCHILD CLASS
        "global class GrandchildClass extends ChildClass {\n"
        // Declare a method on the grandchild.
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
     *   <li>%s in Source 1 allowing the second method in the parent class to potentially call the
     *       overrideable method.
     *   <li>%s in Source 2 to allow the second method in the child class to potentially call either
     *       version of the overrideable method.
     * </ol>
     */
    // spotless:off
    protected static final String[] SUBCLASS_OVERRIDDEN_SOURCES = new String[]{
        // PARENT CLASS, configurably abstract or virtual.
        "global %s class ParentClass {\n"
        // Define an overrideable method.
      + "    public virtual boolean getBool() {\n"
      + "        return true;\n"
      + "    }\n"
        // Also define a method that can be configured to invoke the overrideable method.
        // Annotate it to not trip the rule.
      + "    public boolean parentOptionalInvoker() {\n"
      + "        return %s;\n"
      + "    }\n"
      + "}",
        // CHILD CLASS
        "global class ChildClass extends ParentClass {\n"
        // define an override of the inherited method.
      + "    public override boolean getBool() {\n"
      + "        return false;\n"
      + "    }\n"
        // Also define a method that can be configured to invoke whatever method is needed.
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
     *   <li>%s for the construction of static property {@code staticProp}.
     *   <li>%s for the type of instance property {@code instanceProp}.
     *   <li>%s for the construction of instance property {@code instanceProp}.
     *   <li>%s for the return type of static method {@code staticMethod()}.
     *   <li>%s for the return type of instance method {@code instanceMethod()}.
     *   <li>%s for the type of parameter {@code param} for method {@code invokeMethod()}.
     *   <li>%s for the type of variable {@code var} declared within method {@code invokeMethod()}.
     *   <li>%s for the construction of variable {@code var} declared within method {@code
     *       invokeMethod()}.
     *   <li>%s for the return value of method {@code invokeMethod()}.
     * </ol>
     */
    // spotless:off
    protected static final String INVOKER_SOURCE =
        "global class InvokerClass {\n"
        // Add a static property of configurable type.
      + "    public static %s staticProp = %s;\n"
        // Add an instance property of configurable type.
      + "    public %s instanceProp = %s;\n"
        // Add a static method of configurable return type.
      + "    public static %s staticMethod() {\n"
      + "        return staticProp;\n"
      + "    }\n"
        // Add an instance method of configurable return type.
      + "    public %s instanceMethod() {\n"
      + "        return instanceProp;\n"
      + "    }\n"
        // Declare a method with a parameter of configurable type.
      + "    public boolean invokeMethod(%s param) {\n"
        // Declare a variable of configurable type. UnusedMethodRule won't care
        // that the variable is uninitialized.
      + "        %s var = %s;\n"
      + "        return %s;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /* ============== SETUP/UTILITY METHODS ============== */
    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Run {@link RemoveUnusedMethod} against the provided files, using as an entry point the
     * specified method on the specified type. Verify that the provided methods are used/unused as
     * expected.
     */
    protected void assertExpectations(
            String[] sourceCodes,
            String entryDefiningType,
            String entryMethod,
            Collection<String> usedMethodKeys,
            Collection<String> unusedMethodKeys) {

        // TODO: This method attempts to simulate what AbstractRuleRunner.runRules does minus all
        // the thread specific code
        //      We should refactor the AbstractRuleRunner so that the Thread Specific parts can be
        // injected as a strategy
        //      and then for testing we would inject a strategy that executes without threads so
        // that we can simply
        //      call through to the top level rule runner here in the tests. Doing this refactoring
        // would help us
        //      prevent accidentally forgetting to update this code when changes occur somewhere
        // deep down
        //      inside of the runner's logic (like calling postProcess for example).

        TestUtil.buildGraph(g, sourceCodes);
        RemoveUnusedMethod rule = RemoveUnusedMethod.getInstance();
        MethodVertex entryMethodVertex =
                TestUtil.getMethodVertex(g, entryDefiningType, entryMethod);
        PathBasedRuleRunner runner =
                new PathBasedRuleRunner(g, Lists.newArrayList(rule), entryMethodVertex);
        List<Violation> violations = new ArrayList<>(runner.runRules());
        violations.addAll(
                rule.postProcess(
                        g)); // Important! This is where the violations are created for this rule

        UsageTracker usageTracker = new UsageTracker();
        for (String usedMethodKey : usedMethodKeys) {
            assertTrue(
                    usageTracker.isUsed(usedMethodKey),
                    "Expected usage of method " + usedMethodKey);
        }

        Set<String> unusedMethodsFoundByRule =
                violations.stream()
                        .map(
                                v ->
                                        v.getSourceDefiningType()
                                                + "#"
                                                + v.getSourceVertexName()
                                                + "@"
                                                + v.getSourceLineNumber())
                        .collect(Collectors.toSet());
        for (String unusedMethodKey : unusedMethodKeys) {
            assertThat(
                    "Expected non-usage violation for " + unusedMethodKey,
                    unusedMethodsFoundByRule,
                    hasItem(unusedMethodKey));
        }
    }

    /* ============== ASSERT NO USAGE ============== */
    // TODO: Refactoring opportunity. Long-term, we may want to modularize these methods and put
    // them in another class for re-use.

    /**
     * Run {@link RemoveUnusedMethod} against the provided files, using as an entry point the
     * specified method on the specified type. Verify that the method corresponding to the given key
     * is NOT found to be used.
     */
    protected void assertNoUsage(
            String[] sourceCodes,
            String entryDefiningType,
            String entryMethod,
            String testedMethodKey) {
        assertExpectations(
                sourceCodes,
                entryDefiningType,
                entryMethod,
                new ArrayList<>(),
                Collections.singletonList(testedMethodKey));
    }

    /* ============== ASSERT USAGE ============== */

    /**
     * Run {@link RemoveUnusedMethod} against the provided files, using as an entry point the
     * specified method on the specified type. Verify that the method corresponding to the given key
     * is found to be used.
     */
    protected void assertUsage(
            String[] sourceCodes,
            String entryDefiningType,
            String entryMethod,
            String testedMethodKey) {
        assertExpectations(
                sourceCodes,
                entryDefiningType,
                entryMethod,
                Collections.singletonList(testedMethodKey),
                new ArrayList<>());
    }

    /* ============== ASSERT INELIGIBILITY ============== */

    /**
     * Verify that the specified method is not eligible for analysis under {@link
     * RemoveUnusedMethod}.
     */
    protected void assertMethodIneligibility(
            String sourceCode, String defType, String methodName, int beginLine) {
        assertMethodIneligibility(
                sourceCode,
                new String[] {defType},
                new String[] {methodName},
                new int[] {beginLine});
    }

    /**
     * Verify that the specified methods are not eligible for analysis under {@link
     * RemoveUnusedMethod}.
     */
    protected void assertMethodIneligibility(
            String sourceCode, String[] defTypes, String[] methodNames, int[] beginLines) {
        assertMethodIneligibility(new String[] {sourceCode}, defTypes, methodNames, beginLines);
    }

    /**
     * Verify that the specified methods are not eligible for analysis under {@link
     * RemoveUnusedMethod}.
     *
     * @param sourceCodes
     * @param defTypes
     * @param methodNames
     * @param beginLines
     */
    protected void assertMethodIneligibility(
            String[] sourceCodes, String[] defTypes, String[] methodNames, int[] beginLines) {
        TestUtil.buildGraph(g, sourceCodes);
        RemoveUnusedMethod rule = RemoveUnusedMethod.getInstance();
        // Get every eligible method.
        List<MethodVertex> eligibleMethods = rule.getEligibleMethods(g);
        for (int i = 0; i < defTypes.length; i++) {
            String definingType = defTypes[i];
            String methodName = methodNames[i];
            int beginLine = beginLines[i];
            List<MethodVertex> testedMethods =
                    SFVertexFactory.loadVertices(
                            g,
                            g.V()
                                    .has(NodeType.METHOD, Schema.DEFINING_TYPE, definingType)
                                    .has(Schema.NAME, methodName)
                                    .has(Schema.BEGIN_LINE, beginLine));
            for (MethodVertex testedMethod : testedMethods) {
                assertFalse(
                        eligibleMethods.contains(testedMethod),
                        "Expected "
                                + testedMethod.toMinimalString()
                                + " to be ineligible for rule");
            }
        }
    }
}
