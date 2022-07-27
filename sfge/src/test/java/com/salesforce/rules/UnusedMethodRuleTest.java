package com.salesforce.rules;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.metainfo.MetaInfoCollectorTestProvider;
import com.salesforce.metainfo.VisualForceHandlerImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class UnusedMethodRuleTest {
    private GraphTraversalSource g;

    /* =============== SECTION 0: SETUP METHODS =============== */
    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /* =============== SECTION 1: SIMPLE POSITIVE/NEGATIVE CASES =============== */

    /** Obviously unused static/instance methods are unused. */
    // TODO: Enable subsequent tests as we implement functionality.
    @ValueSource(
            strings = {
                // "public static",
                // "public",
                // "protected", // No need for protected static, since those are mutually exclusive.
                // "private static",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void outerMethodWithoutInvocation_expectViolation(String methodScope) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean unusedMethod() {\n", methodScope)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertViolations(sourceCode, "unusedMethod");
    }

    /** Obviously unused inner class instance methods are unused. */
    // TODO: Enable subsequent tests as we implement functionality.
    @ValueSource(
            strings = {
                /*"public", "protected", */
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void innerInstanceMethodWithoutInvocation_expectViolation(String scope) {
        String sourceCode =
                "global class MyClass {\n"
                        + "    global class MyInnerClass {\n"
                        + String.format("        %s boolean unusedMethod() {\n", scope)
                        + "            return true;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertViolations(sourceCode, "unusedMethod");
    }

    /**
     * When no constructor is declared on a class, a no-parameter constructor is implicitly
     * generated. To reduce noise, this should always count as used.
     */
    @Test
    public void impliedConstructorWithoutInvocation_expectNoViolation() {
        String sourceCode = "public class MyClass {}";
        assertNoViolations(sourceCode);
    }

    /**
     * We want tests for arity of both 0 and 1, since an explicitly declared 0-arity constructor
     * should cause a violation, unlike the implicitly generated one.
     */
    @CsvSource({
        // One test per constructor, per visibility scope.
        "public,  MyClass(),  0",
        "protected,  MyClass(),  0",
        "private,  MyClass(),  0",
        "public,  MyClass(boolean b) , 1",
        "protected,  MyClass(boolean b),  1",
        "private,  MyClass(boolean b),  1"
    })
    @ParameterizedTest(name = "{displayName}: Scope {0} Method {1}, arity {2}")
    @Disabled
    public void declaredConstructorWithoutInvocation_expectViolation(
            String scope, String declaration, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s %s {\n", scope, declaration)
                        + "    }\n"
                        + "}\n";
        Consumer<Violation.RuleViolation> assertion =
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(arity, ((MethodVertex) v.getSourceVertex()).getArity());
                };
        assertViolations(sourceCode, assertion);
    }

    /* =============== SECTION 2: NEGATIVE CASES W/ENGINE DIRECTIVES =============== */

    @Test
    public void applySkipStack_expectNoViolation() {
        String sourceCode =
                "public class MyClass {\n"
                        // Unused static method, annotated with the directive.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private static boolean unusedStaticMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused instance method, annotated with the directive.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private boolean unusedInstanceMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused constructor, annotated with the method.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private MyClass () {\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    @Test
    public void applySkipClassDirective_expectNoViolation() {
        String sourceCode =
                "/* sfge-disable UnusedMethodRule */\n"
                        + "public class MyClass {\n"
                        // Unused static method
                        + "    private static boolean unusedStaticMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused instance method
                        + "    private boolean unusedInstanceMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused constructor
                        + "    private MyClass () {\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /* =============== SECTION 3: NEGATIVE CASES W/PATH ENTRY POINTS =============== */
    // REASONING: Public-facing entry points probably aren't explicitly invoked within the codebase,
    //            but they're almost definitely used by external sources.

    /** Global methods are entrypoints, and should count as used. */
    @ValueSource(strings = {"global", "global static"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void globalMethod_expectNoViolation(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean someMethod() {\n", annotation)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /** public methods on controllers are entrypoints, and should count as used. */
    @Test
    public void publicControllerMethod_expectNoViolation() {
        try {
            String sourceCode =
                    "global class MyController {\n"
                            + "    public String getSomeProperty() {\n"
                            + "        return 'beep';\n"
                            + "    }\n"
                            + "}\n";

            MetaInfoCollectorTestProvider.setVisualForceHandler(
                    new VisualForceHandlerImpl() {
                        private final TreeSet<String> references =
                                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

                        @Override
                        public void loadProjectFiles(List<String> sourceFolders) {
                            // NO-OP
                        }

                        @Override
                        public TreeSet<String> getMetaInfoCollected() {
                            references.add("MyController");
                            return references;
                        }
                    });
            assertNoViolations(sourceCode);
        } finally {
            MetaInfoCollectorTestProvider.removeVisualForceHandler();
        }
    }

    /** Methods returning PageReferences are entrypoints, and should count as used. */
    @Test
    public void pageReferenceMethod_expectNoViolation() {
        String sourceCode =
                "global class MyClass {\n"
                        + "    private PageReference someMethod() {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /** Certain annotated methods are entrypoints, and should count as used. */
    @ValueSource(
            strings = {
                Schema.AURA_ENABLED,
                Schema.INVOCABLE_METHOD,
                Schema.REMOTE_ACTION,
                Schema.NAMESPACE_ACCESSIBLE
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void annotatedMethod_expectNoViolation(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    @%s\n", annotation)
                        + "    private boolean someMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class implements Messaging.InboundEmailHandler, its handleInboundEmail() method is an
     * entrypoint, and should count as used.
     */
    @Test
    public void emailHandlerMethod_expectNoViolation() {
        String sourceCode =
                "global class MyClass implements Messaging.InboundEmailhandler {\n"
                        + "    private Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /* =============== SECTION 4: NEGATIVE CASES W/ABSTRACT METHOD DECLARATION =============== */
    // REASONING: If an interface/class has abstract methods, then subclasses must implement
    //            those methods for the code to compile. And if the interface/class isn't
    //            implemented anywhere, we have separate rules for surfacing that.

    /** Abstract methods on abstract classes/interfaces are abstract, and count as used. */
    @Test
    public void abstractMethodDeclaration_expectNoViolation() {
        String[] sourceCodes = {
            "global abstract class AbstractWithPublic {\n"
                    + "    public abstract boolean someMethod();\n"
                    + "}\n",
            "global abstract class AbstractWithProtected {\n"
                    + "    protected abstract boolean someMethod();\n"
                    + "}\n",
            "global interface MyInterface {\n" + "    boolean anotherMethod();\n" + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /* =============== SECTION 5: NEGATIVE CASE W/INTERNAL CALL =============== */

    /**
     * If a class has static methods that call its other static methods, the called methods count as
     * used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @CsvSource({
        "public,  method1", // Invocation with implicit type reference
        "private,  method1", // Invocation with implicit type reference
        "public,  this.method1", // Invocation with explicit `this` reference
        "private,  this.method1", // Invocation with explicit `this` reference
        "public,  MyClass.method1", // Invocation with explicit class reference
        "private,  MyClass.method1" // Invocation with explicit class reference
    })
    @ParameterizedTest(name = "{displayName}: scope {0}, invocation {1}")
    @Disabled
    public void staticMethodCalledFromOwnStatic_expectNoViolation(String scope, String methodCall) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s static boolean method1() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public static boolean method2() {\n"
                        + String.format("        return %s();\n", methodCall)
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class has instance methods that call its static methods, those static methods count as
     * used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @CsvSource({
        "public,  method1", // Invocation with implicit type reference
        "private,  method1", // Invocation with implicit type reference
        "public,  MyClass.method1", // Invocation with explicit class reference
        "private,  MyClass.method1" // Invocation with explicit class reference
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, invocation {1}")
    @Disabled
    public void staticMethodCalledFromOwnInstanceMethod_expectNoViolation(
            String scope, String methodCall) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s static boolean method1() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public boolean method2() {\n"
                        + String.format("        return %s();\n", methodCall)
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If an inner class calls its outer class's static methods, those static methods count as used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @CsvSource({
        "public,  method1", // Invocation with implicit type reference
        "private,  method1", // Invocation with implicit type reference
        "public,  MyClass.method1", // Invocation with explicit class reference
        "private,  MyClass.method1" // Invocation with explicit class reference
    })
    @ParameterizedTest(name = "{displayName}: scope {0}, invocation {1}")
    @Disabled
    public void staticMethodCalledFromInnerInstance_expectNoViolation(
            String scope, String methodCall) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s static boolean method1() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        + "    global class InnerClass1 {\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "        /* sfge-disable-stack UnusedMethodRule */\n"
                        + "        public boolean method2() {\n"
                        + String.format("            return %s();\n", methodCall)
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class's instance methods call its other instance methods, the called instance methods
     * count as used.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // "public,  method1", // Invocation with implicit type reference
        // "protected,  method1", // Invocation with implicit type reference
        "private,  method1", // Invocation with implicit type reference
        // "public,  this.method1", // Invocation with explicit this reference
        // "protected,  this.method1", // Invocation with explicit this reference
        "private,  this.method1" // Invocation with explicit this reference
    })
    @ParameterizedTest(name = "{displayName}: scope {0}, invocation {1}")
    public void instanceMethodInternallyCalled_expectNoViolation(String scope, String methodCall) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean method1() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public boolean method2() {\n"
                        + String.format("        return %s();\n", methodCall)
                        + "    }\n"
                        + "}\n";

        assertNoViolations(sourceCode);
    }

    /** If a class internally calls its own constructor, that constructor counts as used. */
    @ValueSource(strings = {"public", "protected", "private"})
    @ParameterizedTest(name = "{displayName}: scope {0}")
    @Disabled
    public void constructorInternallyCalled_expectNoViolation(String scope) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s MyClass(boolean b, boolean b2) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public MyClass(boolean b) {\n"
                        + "        this(b, true);\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /* =============== SECTION 6: NEGATIVE CASE W/SIBLING CLASS CALLS =============== */

    /**
     * If a class has two inner classes, and one inner class's instance methods are invoked by
     * another inner class, then those methods count as used. Specific case: Instance provided as
     * method parameter.
     */
    @ValueSource(strings = {"MyClass.MyInner1", "MyInner1"})
    @CsvSource({
        "public,  MyClass.MyInner1",
        "protected,  MyClass.MyInner1",
        "private,  MyClass.MyInner1",
        "public,  MyInner1",
        "protected,  MyInner1",
        "private,  MyInner1"
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, param type {1}")
    @Disabled
    public void innerInstanceMethodCalledFromSiblingViaParameter_expectNoViolation(
            String scope, String paramType) {
        String sourceCode =
                "global class MyClass {\n"
                        + "    global class MyInner1 {\n"
                        + String.format("        %s boolean innerMethod1() {\n", scope)
                        + "            return true;\n"
                        + "        }\n"
                        + "    }\n"
                        + "    global class MyInner2 {\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "        /* sfge-disable-stack UnusedMethodRule */\n"
                        + String.format(
                                "        public boolean innerMethod2(%s instance) {\n", paramType)
                        + "            return instance.innerMethod1();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class has two inner classes, and one inner class's instance methods are invoked by
     * another inner class, then those methods count as used. Specific case: Instance available as
     * property of invoking inner class.
     */
    @CsvSource({
        // The four possible combinations of implicit/explicit outer type reference and
        // implicit/explicit `this`, for all three visibility scopes
        "public,  MyClass.MyInner1,  this.instance",
        "protected,  MyClass.MyInner1,  this.instance",
        "private,  MyClass.MyInner1,  this.instance",
        "public,  MyClass.MyInner1,  instance",
        "protected,  MyClass.MyInner1,  instance",
        "private,  MyClass.MyInner1,  instance",
        "public,  MyInner1,  this.instance",
        "protected,  MyInner1,  this.instance",
        "private,  MyInner1,  this.instance",
        "public,  MyInner1,  instance",
        "protected,  MyInner1,  instance",
        "private,  MyInner1,  instance"
    })
    @ParameterizedTest(name = "{displayName}: Method scope {0}; Declaration {1}; Reference {2}")
    @Disabled
    public void innerInstanceMethodCalledFromSiblingViaOwnProperty_expectNoViolation(
            String scope, String propType, String propRef) {
        String sourceCode =
                "global class MyClass {\n"
                        + "    global class MyInner1 {\n"
                        + String.format("        %s boolean innerMethod1() {\n", scope)
                        + "            return true;\n"
                        + "        }\n"
                        + "    }\n"
                        + "    global class MyInner2() {\n"
                        + String.format("        public %s instance;\n", propType)
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "        /* sfge-disable-stack UnusedMethodRule */\n"
                        + "        public boolean innerMethod2 {\n"
                        + String.format("            return %s.innerMethod1();\n", propRef)
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class has two inner classes, and one inner class's instance methods are invoked by
     * another inner class, then those methods count as used. Specific case: Instance available as
     * property of outer class.
     */
    @CsvSource({
        // Two options for implicit/explicit outer type reference for an instance property,
        // for each of the three visibility scopes.
        "public,  MyClass.MyInner1,  outer.outerProp",
        "protected,  MyClass.MyInner1,  outer.outerProp",
        "private,  MyClass.MyInner1,  outer.outerProp",
        "public,  MyInner1,  outer.outerProp",
        "protected,  MyInner1,  outer.outerProp",
        "private,  MyInner1,  outer.outerProp",
        // Four combinations of implicit/explicit outer class references,
        // for each of the three visibility scopes.
        "public,  static MyClass.MyInner1,  outerProp",
        "protected,  static MyClass.MyInner1,  outerProp",
        "private,  static MyClass.MyInner1,  outerProp",
        "public,  static MyClass.MyInner1,  MyClass.outerProp",
        "protected,  static MyClass.MyInner1,  MyClass.outerProp",
        "private,  static MyClass.MyInner1,  MyClass.outerProp",
        "public,  static MyInner1,  outerProp",
        "protected,  static MyInner1,  outerProp",
        "private,  static MyInner1,  outerProp",
        "public,  static MyInner1, MyClass.outerProp",
        "protected,  static MyInner1, MyClass.outerProp",
        "private,  static MyInner1, MyClass.outerProp"
    })
    @ParameterizedTest(name = "{displayName}: Method scope {0}; Declaration {1}; Reference {2}")
    @Disabled
    public void innerInstanceMethodCalledFromSiblingViaOuterProperty_expectNoViolation(
            String scope, String propType, String propRef) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    public %s outerProp", propType)
                        + "    global class MyInner1 {\n"
                        + String.format("        %s boolean innerMethod1() {\n", scope)
                        + "            return true;\n"
                        + "        }\n"
                        + "    }\n"
                        + "    global class MyInner2 {\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        // Give this method a parameter for some of the tests.
                        + "        public boolean innerMethod2(MyClass outer) {\n"
                        + String.format("            return %s.innerMethod1();\n", propRef)
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /**
     * If a class has two inner classes, and one inner class's constructor is invoked by another
     * inner class, then that constructor counts as used.
     */
    @CsvSource({
        // Four combinations of explicit/implicit outer class reference between variable declaration
        // and constructor, for each of the three visibility scopes.
        "public,  MyClass.MyInner1,  MyClass.MyInner1",
        "protected,  MyClass.MyInner1,  MyClass.MyInner1",
        "private,  MyClass.MyInner1,  MyClass.MyInner1",
        "public,  MyClass.MyInner1,  MyInner1",
        "protected,  MyClass.MyInner1,  MyInner1",
        "private,  MyClass.MyInner1,  MyInner1",
        "public,  MyInner1,  MyClass.MyInner1",
        "protected,  MyInner1,  MyClass.MyInner1",
        "private,  MyInner1,  MyClass.MyInner1",
        "public,  MyInner1,  MyInner1",
        "protected,  MyInner1,  MyInner1",
        "private,  MyInner1,  MyInner1",
    })
    @ParameterizedTest(name = "{displayName}: Method scope {0}; Var type {1}; Constructor {2}")
    @Disabled
    public void innerConstructorCalledFromSibling_expectNoViolation(
            String scope, String varType, String constructor) {
        String sourceCode =
                "global class MyClass {\n"
                        + "    global class MyInner1 {\n"
                        + String.format("        %s MyInner1(boolean b) {\n", scope)
                        + "        }\n"
                        + "    }\n"
                        + "    global class MyInner2 {\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "        /* sfge-disable-stack UnusedMethodRule */\n"
                        + "        public MyInner2() {\n"
                        + String.format(
                                "            %s instance = new %s(true);\n", varType, constructor)
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolations(sourceCode);
    }

    /* =============== SECTION 7: NEGATIVE CASES W/EXTERNAL CALLS =============== */

    /**
     * If a class has static methods, and those methods are invoked by another class, then they
     * count as used.
     */
    @Test
    @Disabled
    public void staticMethodCalledExternallyWithinMethod_expectNoViolation() {
        String[] sourceCodes = {
            "global class DefiningClass {\n"
                    + "    public static boolean someMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class InvokingClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean anotherMethod() {\n"
                    + "        return DefiningClass.someMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a class has static methods, and those methods are invoked to set properties on another
     * class, then they count as used.
     */
    @Test
    @Disabled
    public void staticMethodCalledExternallyByProperty_expectNoViolation() {
        String[] sourceCodes = {
            "global class DefiningClass {\n"
                    + "    public static boolean someMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    public static boolean someOtherMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class InvokingClass {\n"
                    // Reference one method with a static property and the other with an instance
                    // property.
                    + "    public static boolean b1 = DefiningClass.someMethod();\n"
                    + "    public boolean b2 = DefiningClass.someMethod();\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a class has instance methods, and those methods are invoked on an instance of the object,
     * then they count as used.
     */
    @ValueSource(
            strings = {
                // Call on a parameter to the method.
                "directParam",
                // Call on a variable
                "directVariable",
                // Call on an instance property
                "instanceProp",
                "this.instanceProp",
                // Call on a static property
                "staticProp",
                "InvokingClass.staticProp",
                // Call on a static method return
                "staticMethod()",
                "InvokingClass.staticMethod()",
                // Call on an instance method return
                "instanceMethod()",
                "this.instanceMethod()",
                // Call properties and methods on a middleman parameter
                "middlemanParam.instanceMiddlemanProperty",
                "middlemanParam.instanceMiddlemanMethod()",
                // Call properties and methods on a middleman variable
                "middlemanVariable.instanceMiddlemanProperty",
                "middlemanVariable.instanceMiddlemanMethod()",
                // Call static properties and methods on middleman class
                "MiddlemanClass.staticMiddlemanProperty",
                "MiddlemanClass.staticMiddlemanMethod()"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void instanceMethodCalledExternallyWithinMethod_expectNoViolation(
            String objectInstance) {
        String[] sourceCodes = {
            "global class DefiningClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public DefiningClass() {\n"
                    + "    }\n"
                    + "    public boolean testedMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class MiddlemanClass {\n"
                    + "    public DefiningClass instanceMiddlemanProperty;\n"
                    + "    public static DefiningClass staticMiddlemanProperty;\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public DefiningClass instanceMiddlemanMethod() {\n"
                    + "        return new DefiningClass();\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public static DefiningClass staticMiddlemanMethod() {\n"
                    + "        return new DefiningClass();\n"
                    + "    }\n"
                    + "}\n",
            "global class InvokingClass {\n"
                    + "    public DefiningClass instanceProp;"
                    + "    public static DefiningClass staticProp;"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public DefiningClass instanceMethod() {\n"
                    + "        return new DefiningClass();\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public static DefiningClass staticMethod() {\n"
                    + "        return new DefiningClass();\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean anotherMethod(DefiningClass directParam, MiddlemanClass middlemanParam) {\n"
                    + "        DefiningClass directVariable = new DefiningClass();\n"
                    + "        MiddlemanClass middlemanVariable = new MiddlemanClass();\n"
                    + String.format("        return %s.testedMethod();\n", objectInstance)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a class has constructors, and those constructors are invoked by other classes, then they
     * count as used. (Note: Test cases for both explicitly declared 0-arity and 1-arity
     * constructor.)
     */
    @CsvSource({"(),  ()", "(boolean b),  (false)"})
    @ParameterizedTest(name = "{displayName}: {0}/{1}")
    @Disabled
    public void constructorInvokedExternally_expectNoViolation(
            String definingParams, String invokingParams) {
        String[] sourceCodes = {
            "global class DefiningClass {\n"
                    + "    public boolean prop = false;\n"
                    + String.format("    public DefiningClass%s {\n", definingParams)
                    + "    }\n",
            "global class InvokingClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean someMethod() {\n"
                    + String.format("        return new DefiningClass%s.prop;\n", invokingParams)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /* =============== SECTION 8: NEGATIVE CASES W/INHERITANCE BY SUBCLASSES =============== */

    /**
     * If a subclass invokes a static method on its parent class, then that method counts as used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @CsvSource({
        // Invocation in static method, with implicit/explicit `this`,
        // and explicit references to the parent and child classes
        "static boolean,  this.staticMethod()",
        "static boolean,  staticMethod()",
        "static boolean,  ParentClass.staticMethod()",
        "static boolean,  ChildClass.staticMethod()",
        // Invocation in instance method, with implicit/explicit class reference.
        "boolean,  staticMethod()",
        "boolean,  ParentClass.staticMethod()"
    })
    @ParameterizedTest(name = "{displayName}: invoker scope {0}; invocation {1}")
    @Disabled
    public void staticMethodInvokedInSubclass_expectNoViolation(
            String invokerScope, String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + "    public static boolean staticMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public %s invokingMethod() {\n", invokerScope)
                    + String.format("        return %s;\n", invocation)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a subclass's inner class invokes a static method on the parent class, then that method
     * counts as used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @ValueSource(
            strings = {"staticMethod()", "ParentClass.staticMethod()", "ChildClass.staticMethod()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void staticMethodInvokedInSubclassInnerClass_expectNoViolation(String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + "    public static boolean staticMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    + "    global class InnerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "        public boolean invokingMethod() {\n"
                    + String.format("            return %s;\n", invocation)
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a subclass invokes methods it inherited from the parent without overriding them, those
     * methods count as used.
     */
    @CsvSource({
        // Implicit/explicit `this`, and `super`, for each of the two relevant
        // visibility scopes.
        "public,  parentMethod1,  parentMethod2",
        "protected,  parentMethod1,  parentMethod2",
        "public,  this.parentMethod1,  this.parentMethod2",
        "protected,  this.parentMethod1,  this.parentMethod2",
        "public,  super.parentMethod1,  super.parentMethod2",
        "protected,  super.parentMethod1,  super.parentMethod2"
    })
    @ParameterizedTest(
            name =
                    "{displayName}: method scopes {0}, method 1 reference {1}, method 2 reference {2}")
    @Disabled
    public void instanceMethodInvokedWithinNonOverridingSubclass_expectNoViolation(
            String scope, String method1Reference, String method2Reference) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + String.format("    %s boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean childMethod() {\n"
                    + String.format("        return %s();\n", method1Reference)
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean grandchildMethod() {\n"
                    + String.format("        return %s();\n", method2Reference)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a subclass inherits a method from a parent without overriding it, and that method is
     * called on an instance of the subclass, then the parent method counts as used.
     */
    @CsvSource({
        // Both the child and grandchild classes, for each of the two
        // relevant visibility scopes.
        "public,  ChildClass",
        "protected,  ChildClass",
        "public,  GrandchildClass",
        "protected,  GrandchildClass",
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, invoking class {1}")
    @Disabled
    public void instanceMethodInvokedOnInstanceOfNonOverridingSubclass_expectNoViolation(
            String scope, String subclass) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n}\n",
            "global class GrandchildClass extends ChildClass {\n}\n",
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public boolean invokerMethod(%s instance) {\n", subclass)
                    + "        return instance.parentMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If a subclass overrides an inherited method, but calls the `super` version of that method,
     * the parent method counts as used.
     */
    @ValueSource(strings = {"public", "protected"})
    @ParameterizedTest(name = "{displayName}: method scope {0}")
    @Disabled
    public void instanceMethodInvokedViaSuperInOverridingSubclass_expectNoViolation(String scope) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + String.format("    %s virtual boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod1() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean childMethod() {\n"
                    // Invoke the super version instead of the override version.
                    + "        return super.parentMethod1();\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod2() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean grandchildMethod() {\n"
                    // Invoke the super version instead of the override version.
                    + "        return super.parentMethod2();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * If subclass's constructor calls a `super` constructor, the relevant parent constructor counts
     * as used. (Note: Tests for both explicitly-declared 0-arity and 1-arity constructors.)
     */
    @CsvSource({
        "public,  (),  ()",
        "protected,  (),  ()",
        "public,  (boolean b),  (b)",
        "protected,  (boolean b),  (b)"
    })
    @ParameterizedTest(name = "{displayName}: scope {0}; signature{1}")
    @Disabled
    public void constructorInvokedViaSuperInSubclass_expectNoViolation(
            String scope, String paramTypes, String invocationArgs) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s ParentClass%s {\n", scope, paramTypes)
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public ChildClass%s {\n", paramTypes)
                    + String.format("        super%s;\n", invocationArgs)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /* =============== SECTION 9: POSITIVE CASES W/INHERITANCE BY SUBCLASSES =============== */

    /**
     * If a subclass overrides an inherited method and calls its own version of the method rather
     * than the `super`, then the original parent method doesn't count as used.
     */
    @CsvSource({
        // Implicit/explicit `this`, for each of the two relevant
        // visibility scopes.
        "public,  this.parentMethod",
        "protected,  this.parentMethod",
        "public,  parentMethod",
        "protected,  parentMethod",
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, method reference {1}")
    @Disabled
    public void overrideMethodInSubclass_expectViolation(String scope, String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // This calls the overrider, not the original method, so it shouldn't count.
                    + String.format("        return %s();\n", invocation)
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // This calls the overrider, not the original method, so it shouldn't count.
                    + String.format("        return %s();\n", invocation)
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("parentMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * If an overridden method is invoked on an instance of a subclass, then the overridden version
     * of the method on the parent doesn't count as used.
     */
    @CsvSource({
        // Child/Grandchild class, for each of the two relevant visibility scopes.
        "public,  ChildClass",
        "protected,  ChildClass",
        "public,  GrandchildClass",
        "protected,  GrandchildClass",
    })
    @ParameterizedTest(name = "{displayName}: method visibility {0}, instance type {1}")
    @Disabled
    public void overrideMethodOnInstanceOfSubclass_expectViolation(
            String scope, String instanceType) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n" + "}\n",
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format(
                            "    public boolean invokeMethod(%s instance) {\n", instanceType)
                    + "        return instance.parentMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("parentMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /* =============== SECTION 10: NEGATIVE CASES W/OVERRIDDEN INVOCATIONS IN SUPERCLASS =============== */

    /**
     * If a superclass internally calls an instance method, that counts as invoking all overriding
     * versions of that method on subclasses. Reasoning: This is commonly done with abstract classes
     * especially, but using elsewhere isn't unheard of.
     */
    @CsvSource({
        // All four combinations of implicit/explicit `this` on each method, for each parent type,
        // for each of the two relevant visibility scopes.
        "public,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  VirtualParent,  'this.parentMethod1() && parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && parentMethod2()",
        "public,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  VirtualParent,  'parentMethod1() && parentMethod2()",
        "protected,  VirtualParent,  'parentMethod1() && parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  AbstractParent,  'parentMethod1() && parentMethod2()",
        "protected,  AbstractParent,  'parentMethod1() && parentMethod2()",
    })
    @ParameterizedTest(name = "{displayName}: Method scope {0}; Parent class {1}; invocation {2}")
    @Disabled
    public void invocationOfOverriddenInstanceMethodInSuperclass_expectNoViolation(
            String scope, String parentClass, String invocation) {
        String[] sourceCodes = {
            // Have a virtual class that defines a set of methods.
            "global virtual class VirtualParent {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    // The super method needs to be at least as visible as its override,
                    // or else the code won't compile.
                    + String.format("    %s virtual boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    // The super method needs to be at least as visible as its override,
                    // or else the code won't compile.
                    + String.format("    %s virtual boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // Invoke both virtual methods.
                    + String.format("        return %s;\n", invocation)
                    + "    }\n"
                    + "}\n",
            // Have an abstract class that defines the same set of methods.
            "global abstract class AbstractParent {\n"
                    // The super methods need to be at least as visible as their overrides,
                    // or else the code won't compile.
                    + String.format("    %s abstract boolean parentMethod1();\n", scope)
                    + String.format("    %s abstract boolean parentMethod2();\n", scope)
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // Invoke both abstract methods.
                    + String.format("        return %s() && %s();\n", invocation)
                    + "    }\n"
                    + "}\n",
            // Have a child class that extends one of the two available parents. Make it abstract
            // to guarantee compilation in all test cases.
            String.format("global abstract class ChildClass extends %s {\n", parentClass)
                    // The child class overrides one of the parent methods.
                    + String.format("    %s override boolean parentMethod1() {\n", scope)
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // The grandchild class overrides the other.
                    + String.format("    %s override boolean parentMethod2() {\n", scope)
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /**
     * Calling an instance method on a parent class also counts as calling all overrides of that
     * method on subclasses. Reasoning: Particularly with abstract classes, it's frequent for
     * something typed as a parent class to actually be an instance of the child class.
     */
    @ValueSource(strings = {"InterfaceParent", "VirtualParent", "AbstractParent"})
    @ParameterizedTest(name = "{displayName}: Parent class {0}")
    @Disabled
    public void externalInvocationOfOverriddenMethodOnSuperclass_expectNoViolation(
            String parentClass) {
        String[] sourceCodes = {
            // Have an interface that declares some methods.
            "global interface InterfaceParent {\n"
                    + "    public boolean inheritedMethod1();\n"
                    + "    public boolean inheritedMethod2();\n"
                    + "}\n",
            // Have a virtual class that declares the same methods.
            "global virtual class VirtualParent {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public virtual boolean inheritedMethod1() {\n"
                    + "        return true;\n\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public virtual boolean inheritedMethod2() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            // have an abstract class that declares the same methods.
            "global abstract class AbstractParent {\n"
                    + "    public abstract boolean inheritedMethod1();\n"
                    + "    public abstract boolean inheritedMethod2();\n"
                    + "}\n",
            // Have a child class that extends a specified parent class. Make it abstract
            // to guarantee compilation.
            String.format("global abstract class ChildClass extends %s {\n", parentClass)
                    // Have the child class extend one of the parent methods.
                    + "    public override boolean inheritedMethod1() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            // Have a grandchild class that extends the child class.
            "global class GrandchildClass extends ChildClass {\n"
                    // Have the grandchild class extend the other parent method.
                    + "    public override boolean inheritedMethod2() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            // Have an unrelated class that uses an instance of a superclass to invoke a method.
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public boolean doInvocation(%s instance) {\n", parentClass)
                    // Invoke both methods on the instance.
                    + "        return instance.inheritedMethod1() && instance.inheritedMethod2();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes);
    }

    /* =============== SECTION 11: POSITIVE CASES WITH METHOD OVERLOADS =============== */

    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with different arities.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // Provide the arity of the *other* method, since that's the one that is uncalled.
        // One set per method, per visibility scope.
        // "public,  overloadedMethod(),  1",
        // "protected,  overloadedMethod(),  1",
        "private,  overloadedMethod(),  1",
        // "public,  overloadedMethod(false),  0",
        // "protected,  overloadedMethod(false),  0",
        "private,  overloadedMethod(false),  0"
    })
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    public void callInstanceMethodWithDifferentArityOverloads_expectViolation(
            String scope, String invocation, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean overloadedMethod() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        + String.format("    %s boolean overloadedMethod(boolean b) {\n", scope)
                        + "        return b;\n"
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public boolean methodInvoker() {\n"
                        + String.format("        return %s;\n", invocation)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals(v.getSourceVertexName(), "overloadedMethod");
                    assertEquals(((MethodVertex) v.getSourceVertex()).getArity(), arity);
                });
    }

    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with the same arity, but different signatures.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // Specify the beginning line of the overload that WASN'T called.
        // One test per method, per argument source.
        // Argument sources are:
        // - Literal values
        "overloadedMethod(42),  11",
        "overloadedMethod(true),  8",
        // - Method parameters (and their instance properties)
        "overloadedMethod(iParam),  11",
        "overloadedMethod(bParam),  8",
        "overloadedMethod(phcParam.iExternalInstanceProp),  11",
//        "overloadedMethod(phcParam.getIntegerProp()),  11",
        "overloadedMethod(phcParam.bExternalInstanceProp),  8",
//        "overloadedMethod(phcParam.getBooleanProp()),  8",
        // - Variables (and their instance properties)
        "overloadedMethod(iVar),  11",
        "overloadedMethod(bVar),  8",
        "overloadedMethod(phcVar.iExternalInstanceProp),  11",
        "overloadedMethod(phcVar.bExternalInstanceProp),  8",
        // - Method returns (and their instance properties)
        "overloadedMethod(intReturner()),  11",
        "overloadedMethod(this.intReturner()),  11",
        "overloadedMethod(boolReturner()),  8",
        "overloadedMethod(this.boolReturner()),  8",
//        "overloadedMethod(phcReturner().iExternalInstanceProp),  11",
//        "overloadedMethod(this.phcReturner().iExternalInstanceProp),  11",
//        "overloadedMethod(phcReturner().bExternalInstanceProp),  8",
//        "overloadedMethod(this.phcReturner().bExternalInstanceProp),  8",
        // - Internal instance properties (and their instance properties)
        // (with implicit/explicit this)
        "overloadedMethod(iInstanceProp),  11",
        "overloadedMethod(this.iInstanceProp),  11",
        "overloadedMethod(bInstanceProp),  8",
        "overloadedMethod(this.bInstanceProp),  8",
        "overloadedMethod(phcInstanceProp.iExternalInstanceProp),  11",
//        "overloadedMethod(this.phcInstanceProp.iExternalInstanceProp),  11",
        "overloadedMethod(phcInstanceProp.bExternalInstanceProp),  8",
//        "overloadedMethod(this.phcInstanceProp.bExternalInstanceProp),  8",
        // - Internal static properties (and their instance properties)
        // (with implicit/explicit class reference)
        "overloadedMethod(iStaticProp),  11",
        "overloadedMethod(MethodHostClass.iStaticProp),  11",
        "overloadedMethod(bStaticProp),  8",
        "overloadedMethod(MethodHostClass.bStaticProp),  8",
        "overloadedMethod(phcStaticProp.iExternalInstanceProp),  11",
//        "overloadedMethod(MethodHostClass.phcStaticProp.iExternalInstanceProp),  11",
        "overloadedMethod(phcStaticProp.bExternalInstanceProp),  8",
//        "overloadedMethod(MethodHostClass.phcStaticProp.bExternalInstanceProp),  8",
        // - External static instance properties
        "overloadedMethod(PropertyHostClass.iExternalStaticProp),  11",
        "overloadedMethod(PropertyHostClass.bExternalStaticProp),  8"
    })
    @ParameterizedTest(name = "{displayName}: invocation of {0}")
    public void callInstanceMethodWithDifferentSignatureOverloads_expectViolation(
            String invocation, int uncalledBeginLine) {
        String[] sourceCodes = {
            "global class MethodHostClass {\n"
                + "    private static integer iStaticProp = 42;\n"
                + "    private static boolean bStaticProp = false;\n"
                + "    private static PropertyHostClass phcStaticProp = new PropertyHostClass();\n"
                + "    private integer iInstanceProp = 32;\n"
                + "    private boolean bInstanceProp = true;\n"
                + "    private PropertyHostClass phcInstanceProp = new PropertyHostClass();\n"
                + "    private boolean overloadedMethod(Integer i) {\n"
                + "        return true;\n"
                + "    }\n"
                + "    private boolean overloadedMethod(boolean b) {\n"
                + "        return b;\n"
                + "    }\n"
                // Use the engine directive to prevent this method from tripping the rule.
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public integer intReturner() {\n"
                + "        return 7;\n"
                + "    }\n"
                // Use the engine directive to prevent this method from tripping the rule.
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public boolean boolReturner() {\n"
                + "        return true;\n"
                + "    }\n"
                // Use the engine directive to prevent this method from tripping the rule.
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public PropertyHostClass phcReturner() {\n"
                + "        return new PropertyHostClass();\n"
                + "    }\n"
                // Use the engine directive to prevent this method from tripping the rule.
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public boolean methodInvoker(Integer iParam, Boolean bParam, PropertyHostClass phcParam) {\n"
                + "        Integer iVar = 42;\n"
                + "        Boolean bVar = true;\n"
                + "        PropertyHostClass phcVar = new PropertyHostClass();\n"
                + String.format("        return %s;\n", invocation)
                + "    }\n"
                + "}\n",
            "global class PropertyHostClass {\n"
                + "    public static integer iExternalStaticProp = 11;\n"
                + "    public static boolean bExternalStaticProp = false;\n"
                + "    public integer iExternalInstanceProp = 9;\n"
                + "    public boolean bExternalInstanceProp = true;\n"
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public integer getIntegerProp() {\n"
                + "        return iExternalInstanceProp;\n"
                + "    }\n"
                + "    /* sfge-disable-stack UnusedMethodRule */\n"
                + "    public boolean getBooleanProp() {\n"
                + "        return bExternalInstanceProp;\n"
                + "    }\n"
                + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals(v.getSourceVertexName(), "overloadedMethod");
                    assertEquals(v.getSourceVertex().getBeginLine(), uncalledBeginLine);
                });
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with different arities.
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called,
        // and have one variant per visibility scope.
        "public,  new MyClass(true),  2",
        "protected,  new MyClass(true),  2",
        "private,  new MyClass(true),  2",
        "public,  new MyClass(true, true),  1",
        "protected,  new MyClass(true, true),  1",
        "private,  new MyClass(true, true),  1"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    @Disabled
    public void callConstructorWithDifferentArityOverloads_expectViolation(
            String scope, String constructor, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s MyClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MyClass(boolean b, boolean c) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public static void constructorInvocation() {\n"
                        + String.format("        MyClass mc = %s;\n", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals(v.getSourceVertexName(), "<init>");
                    assertEquals(((MethodVertex) v.getSourceVertex()).getArity(), arity);
                });
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with the same arity, but different signatures.
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called.
        // One test per constructor, per visibility scope.
        "public,  new MethodHostClass(42),  4",
        "protected,  new MethodHostClass(42),  4",
        "private,  new MethodHostClass(42),  4",
        "public,  new MethodHostClass(true),  2",
        "protected,  new MethodHostClass(true),  2",
        "private,  new MethodHostClass(true),  2"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    @Disabled
    public void callConstructorWithDifferentSignatureOverloads_expectViolation(
            String scope, String constructor, int beginLine) {
        String sourceCode =
                "global class MethodHostClass {\n"
                        + String.format("    %s MethodHostClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MethodHostClass(Integer i) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public void methodInvoker() {\n"
                        + String.format("        MethodHostClass mhc = %s;\n", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(beginLine, ((MethodVertex) v.getSourceVertex()).getArity());
                });
    }

    /* =============== SECTION 12: WEIRD CORNER CASES =============== */

    /**
     * If an outer class has a static method, and its inner class has an instance method with the
     * same name, then invoking that method without the `this` keyword should still count as using
     * the instance method, not the static method.
     */
    @Test
    @Disabled
    public void innerInstanceOverlapsWithOuterStatic_expectViolationForOuter() {
        String[] sourceCodes = {
            "global virtual class ParentClass {"
                    // Declare a static method on the outer class with a certain name.
                    + "    public static boolean overlappingName() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    global class InnerOfParent {\n"
                    + "        public boolean overlappingName() {\n"
                    + "            return true;\n"
                    + "        }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "        /* sfge-disable-stack UnusedMethodRule */\n"
                    + "        public boolean invoker() {"
                    // Invoke the instance method without using `this`.
                    + "            return overlappingName();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    + "    global class InnerOfChild {\n"
                    + "        public boolean overlappingName() {\n"
                    + "            return true;\n"
                    + "        }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "        /* sfge-disable-stack UnusedMethodRule */\n"
                    + "        public boolean invoker() {\n"
                    // Invoke the instance method without using `this`.
                    + "            return overlappingName();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
        };
        // We expect the outer static method to be unused, and both inner methods to be used.
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("overlappingName", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * If an outer class defines an inner class, it can reference the class with just the inner
     * class name instead of the full class name, even if an unrelated outer class shares the same
     * name. In this case, the methods invoked are the ones on the inner class.
     */
    @Test
    @Disabled
    public void innerClassNameOverlapsWithOuter_expectViolations() {
        String[] sourceCodes = {
            "global class OuterClass {\n"
                    + "    global class OverlappingNameClass {\n"
                    // Declare a constructor.
                    + "        public OverlappingNameClass(boolean b) {\n"
                    + "        }\n"
                    // Declare an instance method.
                    + "        public boolean someMethod() {\n"
                    + "            return true;\n"
                    + "        }\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // Invoke the constructor.
                    + "        OverlappingNameClass instance = new OverlappingNameClass(true);\n"
                    // Invoke the instance method.
                    + "        return instance.someMethod();\n"
                    + "    }\n"
                    + "}\n",
            // Declare another class with the same name as the other class's inner class.
            "global class OverlappingNameClass {\n"
                    // Give it a constructor with the same signature as the inner class.
                    + "    public OverlappingNameClass(boolaen b) {\n"
                    + "    }\n"
                    // Give it a method with the same signature as the instance method on the inner
                    // class.
                    + "    public boolean someMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n"
        };
        // All methods on the outer class should be unused.
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals("OverlappingNameClass", v.getSourceDefiningType());
                    assertEquals(1, v.getSourceLineNumber());
                },
                v -> {
                    assertEquals("someMethod", v.getSourceVertexName());
                    assertEquals("OverlappingNameClass", v.getSourceDefiningType());
                    assertEquals(4, v.getSourceLineNumber());
                });
    }

    /**
     * If a variable shares the same name as a wholly unrelated class, and it has an instance method
     * whose name overlaps with that of a static method on that other class, then calling
     * `var.theMethod()` invokes the instance method, not the static one. So the static method
     * should count as unused.
     */
    @Test
    @Disabled
    public void variableSharesNameWithOtherClass_expectViolation() {
        String[] sourceCodes = {
            "global class MyClass {\n"
                    // Declare a static method.
                    + "    public static boolean someMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class MyOtherClass {\n"
                    // Declare an instance method with the same name as the
                    // other class's static method.
                    + "    public boolean someMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    // This method's param parameter is an instance of MyOtherClass
                    // whose name is myClass.
                    + "    public boolean invokerMethod(MyOtherClass myClass) {\n"
                    // Per manual experimentation, this counts as an invocation of the
                    // instance method, NOT the static method.
                    + "        return myClass.someMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("someMethod", v.getSourceVertexName());
                    assertEquals("MyClass", v.getSourceDefiningType());
                });
    }

    /* =============== SECTION OMEGA: HELPER METHODS =============== */
    // TODO: LONG-TERM, WE MAY WANT TO MODULARIZE THESE AND PUT THEM IN ANOTHER CLASS FOR REUSE.

    /**
     * Assert that each of the provided method names corresponds to a method that threw a violation.
     *
     * @param sourceCode - A single source file.
     */
    private void assertViolations(String sourceCode, String... methodNames) {
        assertViolations(new String[] {sourceCode}, methodNames);
    }

    /**
     * Assert that each of the provided method names corresponds to a method that threw a violation.
     *
     * @param sourceCodes - An array of source files.
     */
    private void assertViolations(String[] sourceCodes, String... methodNames) {
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
    private void assertViolations(
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
    private void assertViolations(
            String[] sourceCodes, Consumer<Violation.RuleViolation>... assertions) {
        TestUtil.buildGraph(g, sourceCodes, true);

        AbstractStaticRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, hasSize(equalTo(assertions.length)));
        for (int i = 0; i < assertions.length; i++) {
            assertions[i].accept((Violation.RuleViolation) violations.get(i));
        }
    }

    /**
     * Assert that all methods in the provided source code are used.
     *
     * @param sourceCode - A source file.
     */
    private void assertNoViolations(String sourceCode) {
        assertNoViolations(new String[] {sourceCode});
    }

    /**
     * Assert that all methods in the provided source codes are used.
     *
     * @param sourceCodes - An array of source files.
     */
    private void assertNoViolations(String[] sourceCodes) {
        TestUtil.buildGraph(g, sourceCodes, true);

        AbstractStaticRule rule = UnusedMethodRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(violations, empty());
    }
}
