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

    // ====== SECTION 1: BLATANTLY UNUSED METHODS ======

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

    // ====== SECTION 2: INTERNAL CALLS ======

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
    public void instanceInvokedByInheritingSubclass_expectNoViolation(
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
    public void instanceNotInvokedByOverridingSubclass_expectViolation(String invocation) {
        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    // The parent class should be virtual, its version of the method un-excluded,
                    // and its secondary method doing nothing important.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[0], "virtual", "", "true"),
                    // The child's version of the method should be excluded, and invoked by the
                    // secondary method.
                    String.format(
                            SUBCLASS_OVERRIDDEN_SOURCES[1],
                            "/* sfge-disable-stack UnusedMethodRule */",
                            invocation)
                };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("getBool", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * Tests for the cases where a method is invoked in a superclass and overridden in a subclass.
     * The subclass's version of the method should count as used. This paradigm is most common in
     * abstract classes.
     *
     * @param superclassModifier - The modifier allowing the superclass to be heritable.
     * @param invocation - The manner in which the superclass invokes the tested method.
     */
    @CsvSource({
        "virtual, this.getBool()",
        "virtual, getBool()",
        "abstract, this.getBool()",
        "abstract, getBool()",
    })
    @ParameterizedTest(name = "{displayName}: superclass is {0}, method invoked as {1}")
    @Disabled
    public void instanceInvokedByOriginatingSuperclass_expectNoViolation(
            String superclassModifier, String invocation) {
        String[] sourceCodes =
                new String[] {
                    // The parent's version of the method should be excluded from analysis, and the
                    // secondary method should invoke it.
                    String.format(
                            SUBCLASS_OVERRIDDEN_SOURCES[0],
                            superclassModifier,
                            "/* sfge-disable-stack UnusedMethodRule */",
                            invocation),
                    // The child's version of the method should be un-excluded, and the secondary
                    // method should do nothing important.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], "", "true")
                };
        assertNoViolations(sourceCodes, 1);
    }

    // ====== SECTION 3: EXTERNAL CALLS ======
    /**
     * Tests for cases where an instance method is invoked on an instance of its defining class or a
     * subclass that inherits it without overriding it.
     *
     * @param hostClass - The class to be instantiated
     * @param instantiation - The instance of {@code hostClass} on which {@code getBool()} is
     *     invoked.
     */
    @CsvSource({
        // Every combination of:
        // - Parent/child/grandchild class
        // - Invocation via
        //    - static property/method with/without explicit type
        //    - instance property/method with/without explicit this
        //    - method parameter
        //    - variable
        //    - inline construction
        "ParentClass, InvokerClass.staticProp",
        "ParentClass, staticProp",
        "ParentClass, InvokerClass.staticMethod()",
        "ParentClass, staticMethod()",
        "ParentClass, this.instanceProp",
        "ParentClass, instanceProp",
        "ParentClass, this.instanceMethod()",
        "ParentClass, instanceMethod()",
        "ParentClass, param",
        "ParentClass, var",
        "ParentClass, new ParentClass()",
        "ChildClass, InvokerClass.staticProp",
        "ChildClass, staticProp",
        "ChildClass, InvokerClass.staticMethod()",
        "ChildClass, staticMethod()",
        "ChildClass, this.instanceProp",
        "ChildClass, instanceProp",
        "ChildClass, this.instanceMethod()",
        "ChildClass, instanceMethod()",
        "ChildClass, param",
        "ChildClass, var",
        "ChildClass, new ChildClass()",
        "GrandchildClass, InvokerClass.staticProp",
        "GrandchildClass, staticProp",
        "GrandchildClass, InvokerClass.staticMethod()",
        "GrandchildClass, staticMethod()",
        "GrandchildClass, this.instanceProp",
        "GrandchildClass, instanceProp",
        "GrandchildClass, this.instanceMethod()",
        "GrandchildClass, instanceMethod()",
        "GrandchildClass, param",
        "GrandchildClass, var",
        "GrandchildClass, new GrandchildClass()",
    })
    @ParameterizedTest(name = "{displayName}: Called on {1}, instance of {0}")
    @Disabled
    public void instanceInvokedOnInstantiatedObject_expectNoViolation(
            String hostClass, String instantiation) {
        String[] sourceCodes =
                new String[] {
                    // The parent method will be public, and neither the child nor grandchild will
                    // invoke it.
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], "public"),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], "public", "true"),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], "public", "true"),
                    // Add a version of the invoker class that calls the method as specified.
                    configureInvoker(hostClass, instantiation + ".getBool()")
                };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Test for the case where an instance method is overridden by a subclass, then the subclass is
     * instantiated and its version of the method is called. This should not count as a usage of the
     * parent method.
     *
     * @param instantiation - The instance of the child class on which {@code getBool()} is invoked.
     */
    @ValueSource(
            strings = {
                "InvokerClass.staticProp",
                "staticProp",
                "InvokerClass.staticMethod()",
                "staticMethod()",
                "this.instanceProp",
                "instanceProp",
                "this.instanceMethod()",
                "instanceMethod()",
                "param",
                "var",
                "new ChildClass()"
            })
    @ParameterizedTest(name = "{displayName}: Child method invoked via {0}")
    @Disabled
    public void instanceNotInvokedOnInstantiatedOverrider_expectViolation(String instantiation) {
        String[] sourceCodes =
                new String[] {
                    // The parent class should be virtual, its version of the method un-excluded,
                    // and its secondary method doing nothing interesting.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[0], "virtual", "", "true"),
                    // The child class's version of the method should be excluded and its secondary
                    // method does nothing interesting.
                    String.format(
                            SUBCLASS_OVERRIDDEN_SOURCES[1],
                            "/* sfge-disable-stack UnusedMethodRule */",
                            "true"),
                    // Add a version of the invoker configured to instantiate the child class and
                    // invoke its version of the method.
                    configureInvoker("ChildClass", instantiation + ".getBool()")
                };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("testedMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceDefiningType());
                });
    }

    /**
     * Tests for cases where a method overrides a superclass's method, and the method is called on
     * an instance of the superclass. In this case, the method should count as used, because it's
     * possible that it's actually a child class instance being up-cast. This paradigm is
     * exceedingly popular with abstract classes.
     *
     * @param instantiation - The instance of the parent class on which {@code getBool()} is
     *     invoked.
     */
    @ValueSource(
            strings = {
                // CRUCIAL NOTE: This test does NOT include an inline construction of ParentClass,
                // since in such
                // a case it's transparently obvious that the object isn't an up-cast subclass.
                "InvokerClass.staticProp",
                "staticProp",
                "InvokerClass.staticMethod()",
                "staticMethod()",
                "this.instanceProp",
                "instanceProp",
                "this.instanceMethod()",
                "instanceMethod()",
                "param",
                "var"
            })
    @ParameterizedTest(name = "{displayName}: Invocation via {0}")
    @Disabled
    public void instanceInvokedOnInstantiatedOriginator_expectNoViolation(String instantiation) {
        String[] sourceCodes =
                new String[] {
                    // The parent should be virtual, its version of the method should be excluded
                    // from analysis, and its secondary method does nothing.
                    String.format(
                            SUBCLASS_OVERRIDDEN_SOURCES[0],
                            "virtual",
                            "/* sfge-disable-stack UnusedMethodRule */",
                            "true"),
                    // The child's version of the method should be non-excluded, and its secondary
                    // method does nothing.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], "", "true"),
                    // Add an invoker that calls the method on an instantiation of the parent.
                    configureInvoker("ParentClass", instantiation + ".getBool()")
                };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where an instance method is called at the end of a long chain of middleman
     * calls and properties.
     *
     * @param middlemanChain - The intermediate chain of properties/method calls.
     */
    @ValueSource(
            strings = {
                // property -> property -> end property
                "middlemanProp.middlemanProp.endProp",
                // property -> property -> end method
                "middlemanProp.middlemanProp.endMethod()",
                // property -> method -> end property
                "middlemanProp.middlemanMethod().endProp",
                // property -> method -> end method
                "middlemanProp.middlemanMethod().endMethod()",
                // method -> property -> end property
                "middlemanMethod().middlemanProp.endProp",
                // method -> property -> end method
                "middlemanMethod().middlemanProp.endMethod()",
                // method -> method -> end property
                "middlemanMethod().middlemanMethod().endProp",
                // method -> method -> end method
                "middlemanMethod().middlemanMethod().endMethod()"
            })
    @ParameterizedTest(name = "{displayName}: middleman chain {0}")
    @Disabled
    public void instanceInvokedViaMiddleman_expectNoViolation(String middlemanChain) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // The target class can be simple.
            String.format(SIMPLE_UNUSED_OUTER_METHOD_SOURCE, "public"),
            // Declare a class that will act as a middleman.
            "global class MiddlemanClass {\n"
            // Give the middleman class a self-referential property, and a property instantiated as the target class.
          + "    public MiddlemanClass middlemanProp = this;\n"
          + "    public MyClass endProp = new MyClass();\n"
            // Give the middleman class a self-returning method and a MyClass-returning method.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public MiddlemanClass middlemanMethod() {\n"
          + "        return this;\n"
          + "    }\n"
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public MyClass endMethod() {\n"
          + "        return new MyClass();\n"
          + "    }\n"
          + "}",
            // Create an invoker that uses the middleman as requested.
            configureInvoker("MiddlemanClass", "this.instanceProp." + middlemanChain + ".unusedMethod()")
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * If a class has two inner classes, and one inner class's instance methods are invoked by
     * another inner class, then those methods count as used. Specific case: Instance provided as
     * method parameter.
     */
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

    /**
     * This test covers a weird edge case: If a class and its instance property both have an
     * instance method with the same name, then invoking {@code this.prop.theMethod()} needs to
     * count as a usage for {@code prop}'s method, not {@code this}'s.
     */
    @Test
    @Disabled
    public void externalReferenceThisCollision_expectViolation() {
        // spotless:off
        String[] sourceCodes = new String[] {
            // The first class should be our generic unused class with a public method.
            String.format(SIMPLE_UNUSED_OUTER_METHOD_SOURCE, "public"),
            "global class SecondClass {\n"
            // Give the second class a property that's an instance of MyClass.
          + "    public MyClass prop;\n"
            // Also give it a method with the name "UnusedMethod", so it overlaps with MyClass.
          + "    public boolean unusedMethod() {\n"
          + "        return false;\n"
          + "    }\n"
            // Give it a method that calls the property's method via `this`, annotated to skip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean invoker() {\n"
          + "        return this.prop.unusedMethod();\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("SecondClass", v.getSourceDefiningType());
                    assertEquals("unusedMethod", v.getSourceVertexName());
                });
    }

    /**
     * This test covers a weird edge case: Inner classes can be instantiated by outer/sibling
     * classes with just the inner name. If another outer class shares the same name, and both
     * classes have a method with the same signature, then the inner class takes precedence over the
     * outer class.
     */
    @Test
    @Disabled
    public void externalReferenceClassNameCollision_expectViolation() {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare an outer class
            "global class OuterClass {\n"
            // Declare a method that instantiates the inner class
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public boolean causeCollision() {\n"
          + "        return new CollidingClass().getBoolean();\n"
          + "    }\n"
            // Declare an inner class to cause collisions.
          + "    public class CollidingClass {\n"
            // Declare a method. Annotate it to not trip the rule.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public boolean getBoolean() {\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "}",
            // Declare an outer class with the same name as the inner class.
            "global class CollidingClass {\n"
            // Give it a method with the same signature as the inner method.
          + "    public boolean getBoolean() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("CollidingClass", v.getSourceDefiningType());
                    assertEquals("getBoolean", v.getSourceVertexName());
                });
    }

    /**
     * Helper method for easily configuring {@link #INVOKER_SOURCE}.
     *
     * @param hostClass - The class to be used in all class-related wildcards.
     * @param invocation - The invocation to be used in the invocation wildcard.
     */
    private static String configureInvoker(String hostClass, String invocation) {
        return String.format(
                INVOKER_SOURCE,
                hostClass,
                hostClass,
                hostClass,
                hostClass,
                hostClass,
                hostClass,
                invocation);
    }
}
