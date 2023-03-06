package com.salesforce.rules.unusedmethod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * These tests verify that static methods are properly handled by {@link
 * com.salesforce.rules.UnusedMethodRule}.
 */
public class StaticMethodsTest extends BaseUnusedMethodTest {

    /**
     * Simple tests verifying that obviously unused static methods are flagged as unused.
     *
     * @param visibility - The target method's visibility scope.
     */
    @ValueSource(
            strings = {
                // No need for protected, since static methods can't be protected.
                "public",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0} static")
    @Disabled
    public void staticWithoutInvocation_expectViolation(String visibility) {
        String sourceCode =
                String.format(SIMPLE_UNUSED_OUTER_METHOD_SOURCE, visibility + " static");
        assertViolations(sourceCode, "unusedMethod");
    }

    /**
     * Tests for cases where a static method is called by the class that defines it.
     *
     * @param testedVisibility - The visibility of the tested method. Either "public" or "private",
     *     since static methods can't be protected and global methods are ineligible.
     * @param callerStatic - Indicates whether the method that calls the tested method should be
     *     static or not.
     * @param invocation - The manner in which the tested method is invoked. Either "method1()" or
     *     "MyClass.method1()". The {@code this} keyword doesn't work for static methods.
     */
    @CsvSource({
        // Every combination of:
        // - private/public tested method (static methods can't be "protected")
        // - static/instance caller method
        // - implicit/explicit type mention.
        "public, true, method1()",
        "public, true, MyClass.method1()",
        "public, false, method1()",
        "public, false, MyClass.method1()",
        "private, true, method1()",
        "private, true, MyClass.method1()",
        "private, false, method1()",
        "private, false, MyClass.method1()",
    })
    @ParameterizedTest(name = "{displayName}: {0} static called by {1} as {2}")
    @Disabled
    public void staticInvokedByOwnClass_expectNoViolation(
            String testedVisibility, boolean callerStatic, String invocation) {
        // The tested method is always static.
        String testedScope = testedVisibility + " static";
        // The calling method is either public static or just public.
        String callerScope = callerStatic ? "public static" : "public";
        String sourceCode =
                String.format(SIMPLE_USED_METHOD_SOURCE, testedScope, callerScope, invocation);
        assertNoViolations(sourceCode, 1);
    }

    /**
     * Tests for cases where a static method is invoked by an inner class of the class that defines
     * it.
     *
     * @param testedVisibility - The visibility of the tested method. Either "public" or "private",
     *     since static methods can't be protected and global are ineligible.
     * @param invocation - The manner in which the method is invoked. Inner classes can invoke outer
     *     static methods with an implicit type (e.g., {@code myMethod()}).
     */
    @CsvSource({
        // Every combination of private/public tested method and implicit/explicit type reference.
        "public, testedMethod()",
        "public, MyClass.testedMethod()",
        "private, testedMethod()",
        "private, MyClass.testedMethod()",
    })
    @ParameterizedTest(name = "{displayName}: {0} static invoked as {1}")
    @Disabled
    public void staticInvokedByInnerClass_expectNoViolation(
            String testedVisibility, String invocation) {
        // spotless:off
        String sourceCode =
            "global class MyClass {\n"
            // Define the tested method.
          + "    " + testedVisibility + " static boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "    \n"
            // Define an inner class.
          + "    public class InnerClass {\n"
            // Give the inner class a method annotated with the engine directive so it doesn't trip the rule.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public boolean callerMethod() {\n"
            // Invoke the tested method.
          + "            return " + invocation + ";\n"
          + "        }\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertNoViolations(sourceCode, 1);
    }

    /**
     * Tests for cases where a static method is called on a subclass of the class that defines it.
     *
     * @param callersAreStatic - Indicates whether the method that calls the tested method should be
     *     static
     * @param callerClass - "child" or "grandchild" depending on which subclass should invoke the
     *     parent method
     * @param invocation - The manner in which the method is invoked. Can be implicit type reference
     *     or explicit reference to self or parent.
     */
    @CsvSource({
        // Every combination of:
        // - Static/instance caller method
        // - Calling in child vs calling in grandchild
        // - Calling via implicit type reference, or reference to specific class
        "true, child, methodOnParent()",
        "true, child, ParentClass.methodOnParent()",
        "true, child, ChildClass.methodOnParent()",
        "true, grandchild, methodOnParent()",
        "true, grandchild, ParentClass.methodOnParent()",
        "true, grandchild, ChildClass.methodOnParent()",
        "true, grandchild, GrandchildClass.methodOnParent()",
        "false, child, methodOnParent()",
        "false, child, ParentClass.methodOnParent()",
        "false, child, ChildClass.methodOnParent()",
        "false, grandchild, methodOnParent()",
        "false, grandchild, ParentClass.methodOnParent()",
        "false, grandchild, ChildClass.methodOnParent()",
        "false, grandchild, GrandchildClass.methodOnParent()",
    })
    @ParameterizedTest(name = "{displayName}: Called by {0} in {1} as {2}")
    @Disabled
    public void staticInvokedBySubclass_expectNoViolation(
            boolean callersAreStatic, String callerClass, String invocation) {
        // The tested method is always public static. No need for private/protected, since private
        // is inaccessible in subclasses and static method can't be protected.
        String testedScope = "public static";
        // The child and grandchild method are always public, but may also be static.
        String childScope = callersAreStatic ? "public static" : "public";
        String grandchildScope = callersAreStatic ? "public static" : "public";
        // Either the child or grandchild will invoke the parent method. The other will return a
        // literal true.
        String childReturn = callerClass.equalsIgnoreCase("child") ? invocation : "true";
        String grandchildReturn = callerClass.equalsIgnoreCase("grandchild") ? invocation : "true";

        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], testedScope),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], childScope, childReturn),
                    String.format(
                            SUBCLASS_NON_OVERRIDDEN_SOURCES[2], grandchildScope, grandchildReturn)
                };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where a static method is invoked by an inner class of its defining class's
     * subclass.
     *
     * @param invocation - The manner in which the method is invoked. Inner classes can use implicit
     *     references to outer type.
     */
    @ValueSource(
            strings = {"testedMethod()", "ParentClass.testedMethod()", "ChildClass.testedMethod()"})
    @ParameterizedTest(name = "{displayName}: Invoked as {0}")
    @Disabled
    public void staticInvokedByInnerOfSubclass_expectNoViolation(String invocation) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Parent class
            "global class ParentClass {\n"
            // Declare a static method to test. Must be public, since static can't be protected.
          + "    public static boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Child class
            "global class ChildClass extends ParentClass {\n"
            // Inner class of child class
          + "    public class InnerOfChild {\n"
            // Give the inner class a method with the directive so it doesn't trigger the rule.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public boolean callTestedMethod() {\n"
            // Invoke the tested method
          + "            " + invocation + ";\n"
          + "        }\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where a static method is invoked in the initialization of a property on the
     * same class.
     *
     * @param methodVisibility - The visibility of the static method being tested. Either public or
     *     private.
     * @param propScope - Modifiers for the scope of the property used to invoke the method.
     * @param invocation - The manner in which the method is invoked.
     */
    @CsvSource({
        // Every combination of:
        // - public/private tested method
        // - public/private and static/instance property
        // - Implicit/explicit type reference in invocation
        "public, public static, testedMethod()",
        "public, public static, MyClass.testedMethod()",
        "public, private static, testedMethod()",
        "public, private static, MyClass.testedMethod()",
        "private, public static, testedMethod()",
        "private, public static, MyClass.testedMethod()",
        "private, private static, testedMethod()",
        "private, private static, MyClass.testedMethod()",
        "public, public, testedMethod()",
        "public, public, MyClass.testedMethod()",
        "public, private, testedMethod()",
        "public, private, MyClass.testedMethod()",
        "private, public, testedMethod()",
        "private, public, MyClass.testedMethod()",
        "private, private, testedMethod()",
        "private, private, MyClass.testedMethod()",
    })
    @ParameterizedTest(name = "{displayName}: {0} static invoked by {1} property as {2}")
    @Disabled
    public void staticInvokedViaOwnProperty_expectNoViolation(
            String methodVisibility, String propScope, String invocation) {
        String sourceCode =
                String.format(
                        METHOD_INVOKED_AS_PROPERTY_SOURCE, propScope, invocation, methodVisibility);
        assertNoViolations(sourceCode, 1);
    }
}
