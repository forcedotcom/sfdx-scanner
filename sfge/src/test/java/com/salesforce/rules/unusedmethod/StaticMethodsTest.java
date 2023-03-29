package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * @param callerScope - The scope of the method that calls the tested method. Either public
     *     static or public.
     * @param invocation - The manner in which the tested method is invoked. Either "method1()" or
     *     "MyClass.method1()". The {@code this} keyword doesn't work for static methods.
     */
    @CsvSource({
        // Every combination of:
        // - private/public tested method (static methods can't be "protected")
        // - static/instance caller method
        // - implicit/explicit type mention.
        "public, public static, method1()",
        "public, public static, MyClass.method1()",
        "public, public, method1()",
        "public, public, MyClass.method1()",
        "private, public static, method1()",
        "private, public static, MyClass.method1()",
        "private, public, method1()",
        "private, public, MyClass.method1()",
    })
    @ParameterizedTest(name = "{displayName}: {0} static called by {1} as {2}")
    public void staticInvokedByOwnClass_expectNoViolation(
            String testedVisibility, String callerScope, String invocation) {
        // The tested method is always static.
        String testedScope = testedVisibility + " static";
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
     * @param callerScope - Scope of the subclass method that invokes the tested method.
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
        "public static, child, methodOnParent()",
        "public static, child, ParentClass.methodOnParent()",
        "public static, child, ChildClass.methodOnParent()",
        "public static, grandchild, methodOnParent()",
        "public static, grandchild, ParentClass.methodOnParent()",
        "public static, grandchild, ChildClass.methodOnParent()",
        "public static, grandchild, GrandchildClass.methodOnParent()",
        "public, child, methodOnParent()",
        "public, child, ParentClass.methodOnParent()",
        "public, child, ChildClass.methodOnParent()",
        "public, grandchild, methodOnParent()",
        "public, grandchild, ParentClass.methodOnParent()",
        "public, grandchild, ChildClass.methodOnParent()",
        "public, grandchild, GrandchildClass.methodOnParent()",
    })
    @ParameterizedTest(name = "{displayName}: Called by {0} in {1} as {2}")
    public void staticInvokedBySubclass_expectNoViolation(
            String callerScope, String callerClass, String invocation) {
        // The tested method is always public static. No need for private/protected, since private
        // is inaccessible in subclasses and static method can't be protected.
        String testedScope = "public static";
        // Either the child or grandchild will invoke the parent method. The other will return a
        // literal true.
        String childReturn = callerClass.equalsIgnoreCase("child") ? invocation : "true";
        String grandchildReturn = callerClass.equalsIgnoreCase("grandchild") ? invocation : "true";

        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], testedScope),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], callerScope, childReturn),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], callerScope, grandchildReturn)
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
    public void staticInvokedByInnerOfSubclass_expectNoViolation(String invocation) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Parent class
            "global virtual class ParentClass {\n"
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
     * Tests for cases where an inner class inherits a static method, and the static method is
     * invoked by the outer class.
     *
     * @param invocation - The manner in which the method is called. Can reference the parent class,
     *     or the child class (with/without full type reference).
     */
    @ValueSource(
            strings = {
                "ParentClass.testedMethod()",
                "ChildClass.testedMethod()",
                "OuterClass.ChildClass.testedMethod()"
            })
    @ParameterizedTest(name = "{displayName}: Invoked as {0}")
    public void staticInnerInvokedByOuter_expectNoViolation(String invocation) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare an extensible class with a static method. This is our tested method.
            "global virtual class ParentClass {\n"
          + "    public static boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare an outer class...
            "global class OuterClass {\n"
            // ...with an inner class that extends the tested class...
          + "    global class ChildClass extends ParentClass {}\n"
            // ...and a method that configurably calls the tested static method.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean invoker() {\n"
          + "        return " + invocation + ";\n"
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
    public void staticInvokedViaOwnProperty_expectNoViolation(
            String methodVisibility, String propScope, String invocation) {
        String sourceCode =
                String.format(
                        METHOD_INVOKED_AS_PROPERTY_SOURCE,
                        propScope,
                        invocation,
                        methodVisibility + " static");
        assertNoViolations(sourceCode, 1);
    }

    /**
     * These tests cover variants of a weird edge case: If an inner class defines/inherits an
     * instance method with the same name as a static method on its outer class, then it can invoke
     * that instance method with the same syntax that would normally be an internal invocation of
     * the outer static method (e.g., {@code method()}). In this case, the static method should
     * count as unused.
     *
     * @param collidingMethod - The name to be used for the static method.
     */
    @ValueSource(strings = {"methodFromInnerClass", "methodFromParentClass", "methodFromInterface"})
    @ParameterizedTest(name = "{displayName}: Collision with {0}")
    public void internalReferenceSyntaxCollision_expectViolation(String collidingMethod) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare an outer class with a static method. This will be our tested method.
            "global class MyClass {\n"
          + "    public static boolean " + collidingMethod + "() {\n"
          + "        return true;\n"
          + "    }\n"
            // Declare an abstract inner class that has a method, and inherits methods from both an interface
            // and another class.
          + "    global abstract class InnerClass extends ParentClass implements ParentInterface {\n"
            // The inner class has a method annotated to not trip the rule.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public boolean methodFromInnerClass() {\n"
          + "            return false;\n"
          + "        }\n"
            // The inner class also has a method that invokes the colliding method.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public boolean callMethod() {\n"
            // IMPORTANT! THE COLLIDING METHOD IS INVOKED WITHOUT ANY QUALIFIERS,
          + "            return " + collidingMethod + "();\n"
          + "        }\n"
          + "    }\n"
          + "}",
            // Declare a parent class for the inner class to extend
            "global virtual class ParentClass {\n"
            // Give the parent class a method for the inner class to inherit.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean methodFromParentClass() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare an interface for the inner class to implement
            "global interface ParentInterface {\n"
          + "    boolean methodFromInterface();\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("MyClass", v.getSourceDefiningType());
                    assertEquals(collidingMethod, v.getSourceVertexName());
                });
    }

    /**
     * These tests cover variants of a weird edge case: The syntax for external calls of static
     * methods (i.e., {@code SomeClass.someMethod()}) is identical to the syntax for calling {@code
     * someMethod()} on a variable/property/whatever named {@code SomeClass}. In this case, the
     * static method should count as unused.
     *
     * @param collidingName - The name given to the tested class to cause a collision.
     */
    @ValueSource(strings = {"instanceProp", "staticProp", "methodParam", "variable", "InnerClass"})
    @ParameterizedTest(name = "{displayName}: Collides with {0}")
    public void externalReferenceSyntaxCollision_expectViolation(String collidingName) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare a class with a static method and a configurable name. That's our  tested method.
            "global class " + collidingName + " {\n"
          + "    public static boolean getBoolean() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare a class with an instance method for the tested method to collide with.
            "global class InstanceCollider {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean getBoolean() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare a class with a static method for the tested method to collide with.
            "global virtual class StaticCollider {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public static boolean getBoolean () {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare a class that will do all sorts of collision-y operations.
            "global class CollisionCreator {\n"
            // These properties can be collided with.
          + "    public InstanceCollider instanceProp;\n"
          + "    public static InstanceCollider staticProp;\n"
            // This inner class extends StaticCollider, meaning it has the static
            // method and can therefore cause collisions.
          + "    public class InnerClass extends StaticCollider {}\n"
            // IMPORTANT: This method allows us to create a configurable collision.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean causeCollision(InstanceCollider methodParam) {\n"
          + "        InstanceCollider variable = new InstanceCollider();\n"
          + "        return " + collidingName + ".getBoolean();\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("getBoolean", v.getSourceVertexName());
                    assertEquals(collidingName, v.getSourceDefiningType());
                });
    }
}
