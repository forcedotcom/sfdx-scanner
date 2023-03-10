package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * These tests verify that instance methods are properly handled by {@link
 * com.salesforce.rules.UnusedMethodRule}.
 */
public class InstanceMethodsTest extends BaseUnusedMethodTest {

    /**
     * Simple tests verifying that obviously unused instance methods are flagged as unused.
     *
     * @param visibility - The target method's visibility scope.
     */
    // TODO: ENABLE MORE TESTS AS WE ADD MORE FUNCTIONALITY
    @ValueSource(
            strings = {
                // "public",
                // "protected",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0} instance")
    public void instanceWithoutInvocation_expectViolation(String visibility) {
        String sourceCode = String.format(SIMPLE_UNUSED_OUTER_METHOD_SOURCE, visibility);
        assertViolations(sourceCode, "unusedMethod");
    }

    /**
     * Simple tests verifying that obviously unused instance methods on inner classes are flagged as
     * unused.
     *
     * @param visibility - The target method's visibility scope.
     */
    // TODO: ENABLE MORE TESTS AS WE ADD MORE FUNCTIONALITY
    @ValueSource(
            strings = {
                // "public",
                // "protected",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0} instance")
    public void innerInstanceWithoutInvocation_expectViolation(String visibility) {
        // spotless:off
        String sourceCode =
            "global class MyClass {\n"
          + "    global class MyInnerClass {\n"
          + "        " + visibility + " boolean unusedMethod() {\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertViolations(sourceCode, "unusedMethod");
    }

    /**
     * Tests for cases where an instance method is called by the class that defines it.
     *
     * @param testedVisibility - The visibility of the tested method. Any except global, since
     *     globals are ineligible.
     * @param invocation - The manner in which the method is called. Either {@code method1()} or
     *     {@code this.method1()}.
     */
    @CsvSource({
        // "public, method1()",
        // "protected, method1()",
        "private, method1()",
        // "public, this.method1()",
        // "protected, this.method1()",
        "private, this.method1()"
    })
    @ParameterizedTest(name = "{displayName}: {0} instance method invoked as {1}")
    public void instanceInvokedByOwnClass_expectNoViolation(
            String testedVisibility, String invocation) {
        // The caller method is always a public instance method, since static methods can't invoke
        // instance methods.
        String sourceCode =
                String.format(SIMPLE_USED_METHOD_SOURCE, testedVisibility, "public", invocation);
        assertNoViolations(sourceCode, 1);
    }

    /**
     * Tests for cases where an instance method is called on a subclass of the class that defines
     * it.
     *
     * @param testedVisibility - The visibility of the tested method. Either public or protected,
     *     since private isn't heritable.
     * @param callerClass - "child" or "grandchild" depending on which subclass should invoke the
     *     parent method
     * @param invocation - The manner in which the method is invoked. Either "methodOnParent()" or
     *     "ParentClass.methodOnParent()". The {@code this} keyword doesn't work for static methods.
     */
    @CsvSource({
        // Every combination of:
        // - protected/public tested method
        // - Calling in child vs calling in grandchild
        // - Calling via implicit `this`, explicit `this`, or `super`.
        "public, child, methodOnParent()",
        "public, child, this.methodOnParent()",
        "public, child, super.methodOnParent()",
        "public, grandchild, methodOnParent()",
        "public, grandchild, this.methodOnParent()",
        "public, grandchild, super.methodOnParent()",
        "protected, child, methodOnParent()",
        "protected, child, this.methodOnParent()",
        "protected, child, super.methodOnParent()",
        "protected, grandchild, methodOnParent()",
        "protected, grandchild, this.methodOnParent()",
        "protected, grandchild, super.methodOnParent()",
    })
    @ParameterizedTest(name = "{displayName}: Called by {0} in {1} as {2}")
    @Disabled
    public void instanceInvokedBySubclass_expectNoViolation(
            String testedVisibility, String callerClass, String invocation) {
        // Either the child or grandchild will invoke the parent method. The other will return a
        // literal true.
        String childReturn = callerClass.equalsIgnoreCase("child") ? invocation : "true";
        String grandchildReturn = callerClass.equalsIgnoreCase("grandchild") ? invocation : "true";

        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], testedVisibility),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], "public", childReturn),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], "public", grandchildReturn)
                };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where a method is overridden by a subclass, and that version is called
     * instead of the original version. The original version should have a violation.
     *
     * @param invocation - The manner in which the override is invoked
     */
    @ValueSource(
            strings = {
                // Implicit vs explicit 'this'
                "this.testedMethod()",
                "testedMethod()"
            })
    @ParameterizedTest(name = "{displayName}: Invoked via {0}")
    @Disabled
    public void instanceOverriddenAndNotInvoked_expectViolation(String invocation) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global virtual class ParentClass {\n"
            // Define the method that will be overridden
          + "    public virtual boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            "global class ChildClass extends ParentClass {\n"
            // Override the method from the parent.
          + "    /* sfge-disable-stack UnusedMethodRule */"
          + "    public override boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
            // Call the override, not the variant on the parent.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean invoker() {\n"
          + "        return " + invocation + ";\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("testedMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * Tests for cases where an instance method is invoked on an instance of its defining class or a
     * subclass that inherits it without overriding it.
     *
     * @param hostClass - The class to be instantiated
     */
    @ValueSource(strings = {"ParentClass", "ChildClass", "GrandchildClass"})
    @ParameterizedTest(name = "{displayName}: Instance of {0}")
    @Disabled
    public void instanceInvokedOnObjectInstance_expectNoViolation(String hostClass) {
        // spotless:off
        String[] sourceCodes = new String[] {
            // The parent method will be public, and neither the child nor grandchild will invoke it.
            String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], "public"),
            String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], "public", "true"),
            String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], "public", "true"),
            // Also add another class that instantiates the class and calls the method.
            "global class InvokerClass {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean invokeMethod() {\n"
          + "        " + hostClass + " obj = new " + hostClass + "();\n"
          + "        return obj.methodOnParent();\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Test for the case where an instance method is overridden by a subclass, then the subclass is
     * instantiated and the override version is called instead of the original.
     */
    @Test
    @Disabled
    public void instanceInvokedOnOverriddenSubclass_expectViolation() {
        // spotless:off
        String[] sourceCodes = new String[]{
            // PARENT CLASS
            "global virtual class ParentClass {\n"
          + "    public virtual boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // CHILD CLASS overrides parent method
            "global class ChildClass extends ParentClass {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public override boolean testedMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}\n",
            // Invoker class instantiates the subclass and invokes the method.
            "global class Invoker {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean callMethod() {\n"
          + "        return new ChildClass().testedMethod();\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("testedMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceDefiningType());
                });
    }

    /**
     * Tests for the cases where a method overrides a method it inherited from a parent class, and
     * the parent class calls the method. The method should count as used, because this paradigm is
     * exceedingly popular for abstract classes.
     *
     * @param superclassModifier - The modifier that allows the superclass to be heritable.
     * @param invocation - The manner in which the superclass invokes the method.
     */
    @CsvSource({
        "virtual, this.getBool()",
        "virtual, getBool()",
        "abstract, this.getBool()",
        "abstract, getBool()"
    })
    @ParameterizedTest(name = "{displayName}: {0} superclass, invoked as {1}")
    @Disabled
    public void instanceInvokedBySuperclass_expectNoViolation(
            String superclassModifier, String invocation) {
        // spotless:off
        String[] sourceCodes = new String[] {
            "global " + superclassModifier + " class Superclass {\n"
            // Declare a method in the superclass, annotated so it can't trip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public virtual boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
            // Declare a method that invokes the tested method.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean callTestedMethod() {\n"
          + "        return " + invocation + ";\n"
          + "    }\n"
          + "}",
            // Declare a subclass that overrides the inherited method.
            "global class MyClass extends Superclass {\n"
          + "    public override boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where a method overrides a superclass's method, and the method is called on
     * an instance of the superclass. In this case, the method should count as used, because this
     * paradigm is very common with abstract classes in particular.
     *
     * @param superclassModifier - The modifier applied to the superclass that makes it heritable.
     */
    @ValueSource(strings = {"virtual", "abstract"})
    @ParameterizedTest(name = "{displayName}: {0} superclass")
    @Disabled
    public void instanceInvokedOnSuperclass_expectNoViolation(String superclassModifier) {
        // spotless:off
        String[] sourceCodes = new String[] {
            "global " + superclassModifier + " class Superclass {\n"
            // Declare a method in the superclass, annotated so it can't trip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public virtual boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare a subclass that overrides the inherited method.
            "global class MyClass extends Superclass {\n"
          + "    public override boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Declare a class with a method that accepts a superclass instance and calls the target method.
            "global class InvokerClass {\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean callMethod(Superclass obj) {\n"
          + "        return obj.getBool();\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

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
        assertNoViolations(sourceCode, 1);
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
        assertNoViolations(sourceCode, 1);
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
        assertNoViolations(sourceCode, 1);
    }
}
