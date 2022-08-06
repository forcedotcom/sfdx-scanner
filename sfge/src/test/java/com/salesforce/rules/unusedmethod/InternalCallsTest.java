package com.salesforce.rules.unusedmethod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A set of tests for methods that are called by the class that defines them. */
public class InternalCallsTest extends BaseUnusedMethodTest {

    /* =============== SECTION 1: STATIC METHODS =============== */

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
        assertNoViolations(sourceCode, 1);
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
        assertNoViolations(sourceCode, 1);
    }

    /* =============== SECTION 2: INSTANCE METHODS =============== */

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

        assertNoViolations(sourceCode, 1);
    }

    /* =============== SECTION 3: CONSTRUCTOR METHODS =============== */

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
        assertNoViolations(sourceCode, 1);
    }
}
