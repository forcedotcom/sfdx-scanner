package com.salesforce.rules.unusedmethod;

import java.util.Collections;
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
    @ValueSource(strings = {"public", "protected", "private"})
    @ParameterizedTest(name = "{displayName}: {0} instance")
    public void instanceWithoutInvocation_expectViolation(String visibility) {
        String sourceCode = String.format(SIMPLE_SOURCE, visibility, "global", "true");
        assertNoUsage(
                new String[] {sourceCode}, "MyClass", "entrypointMethod", "MyClass#testedMethod@2");
    }

    /**
     * Simple tests verifying that obviously unused instance methods on inner classes are flagged as
     * unused.
     *
     * @param visibility - The target method's visibility scope.
     */
    @ValueSource(strings = {"public", "protected", "private"})
    @ParameterizedTest(name = "{displayName}: {0} instance")
    public void innerInstanceWithoutInvocation_expectViolation(String visibility) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global class MyClass {\n"
          + "    global class MyInnerClass {\n"
          + "        " + visibility + " boolean testedMethod() {\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "}",
            // Have the entrypoint not call the method.
            String.format(SIMPLE_ENTRYPOINT, "true")
        };
        // spotless:on
        assertNoUsage(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                "MyClass.MyInnerClass#testedMethod@3");
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
        "public, testedMethod()",
        "protected, testedMethod()",
        "private, testedMethod()",
        "public, this.testedMethod()",
        "protected, this.testedMethod()",
        "private, this.testedMethod()"
    })
    @ParameterizedTest(name = "{displayName}: {0} instance method invoked as {1}")
    public void instanceInvokedByOwnClass_expectNoViolation(
            String testedVisibility, String invocation) {
        // The caller method is always a public instance method, since static methods can't invoke
        // instance methods.
        String sourceCode = String.format(SIMPLE_SOURCE, testedVisibility, "global", invocation);
        assertUsage(
                new String[] {sourceCode}, "MyClass", "entrypointMethod", "MyClass#testedMethod@2");
    }

    /**
     * Tests for cases where an instance method is called by a subclass of the class that defines
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
    public void instanceInvokedBySubclass_expectNoViolation(
            String testedVisibility, String callerClass, String invocation) {
        // Either the child or grandchild will invoke the parent method. The other will return a
        // literal true.
        String childReturn = callerClass.equalsIgnoreCase("child") ? invocation : "true";
        String grandchildReturn = callerClass.equalsIgnoreCase("grandchild") ? invocation : "true";
        String entrypointInvocation =
                callerClass.equalsIgnoreCase("child")
                        ? "new ChildClass().methodOnChild()"
                        : "new GrandchildClass().methodOnGrandchild()";

        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], testedVisibility),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], "public", childReturn),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], "public", grandchildReturn),
                    String.format(SIMPLE_ENTRYPOINT, entrypointInvocation)
                };
        assertUsage(
                sourceCodes, "MyEntrypoint", "entrypointMethod", "ParentClass#methodOnParent@2");
    }

    /**
     * Test for the case where a class inherits and overrides an instance method, but calls the
     * inherited version via {@code super}. In this case, the inherited version should count as
     * used.
     */
    @Test
    @Disabled // TODO: FIX AND ENABLE THIS TEST
    public void instanceInvokedByOverridingSubclass_expectNoViolation() {
        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    // The parent class should be virtual, and its secondary method doing nothing
                    // important.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[0], "virtual", "true"),
                    // The child's secondary method should invoke the super version of the method.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], "super.getBool()"),
                    // Add an entrypoint
                    String.format(SIMPLE_ENTRYPOINT, "new ChildClass().childOptionalInvoker()")
                };
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("ParentClass#getBool@2"),
                Collections.singletonList("ChildClass#getBool@2"));
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
                "this.getBool()",
                "getBool()"
            })
    @ParameterizedTest(name = "{displayName}: Invoked via {0}")
    public void instanceNotInvokedByOverridingSubclass_expectViolation(String invocation) {
        // Fill in the source code template.
        String[] sourceCodes =
                new String[] {
                    // The parent class should be virtual, its version of the method un-excluded,
                    // and its secondary method doing nothing important.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[0], "virtual", "true"),
                    // The child's version of the method should be excluded, and invoked by the
                    // secondary method.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], invocation),
                    String.format(SIMPLE_ENTRYPOINT, "new ChildClass().childOptionalInvoker()")
                };
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("ChildClass#getBool@2"),
                Collections.singletonList("ParentClass#getBool@2"));
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
    @Disabled // TODO: FIX AND ENABLE THIS TEST
    public void instanceInvokedByOriginatingSuperclass_expectNoViolation(
            String superclassModifier, String invocation) {
        // spotless:off
        String[] sourceCodes =
                new String[] {
                    // The parent's version of the method should be excluded from analysis, and the
                    // secondary method should invoke it.
                    String.format(
                            SUBCLASS_OVERRIDDEN_SOURCES[0],
                            superclassModifier,
                            invocation),
                    // The child's version of the method should be un-excluded, and the secondary
                    // method should do nothing important.
                    String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], "true"),
                    // The entrypoint should instantiate a child class, upcast it to a parent, and
                    // invoke the parent optional method.
                    String.format(
                            COMPLEX_ENTRYPOINT,
                            "        ParentClass pc = new ChildClass();\n"
                          + "        return pc.parentOptionalInvoker();\n")
                };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("ChildClass#getBool@2"),
                Collections.singletonList("ParentClass#getBool@2"));
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
        // "ParentClass, InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS
        // LONGER THAN 2 ARE SUPPORTED
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
        // "ChildClass, InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS
        // LONGER THAN 2 ARE SUPPORTED
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
        // "GrandchildClass, InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE
        // CHAINS LONGER THAN 2 ARE SUPPORTED
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
    public void instanceInvokedOnInstantiatedObject_expectNoViolation(
            String hostClass, String instantiation) {
        // spotless:off
        String[] sourceCodes =
                new String[] {
                    // The parent method will be public, and neither the child nor grandchild will
                    // invoke it.
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[0], "public"),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[1], "public", "true"),
                    String.format(SUBCLASS_NON_OVERRIDDEN_SOURCES[2], "public", "true"),
                    // Add a version of the invoker class that calls the method as specified.
                    configureInvoker(
                            hostClass,
                            "new " + hostClass + "()",
                            instantiation + ".methodOnParent()"),
                    // Add an entrypoint.
                    String.format(
                            COMPLEX_ENTRYPOINT,
                            "        InvokerClass ic = new InvokerClass();\n"
                          + "        " + hostClass + " param = new " + hostClass + "();\n"
                          + "        return ic.invokeMethod(param);\n")
                };
        // spotless:on
        assertUsage(
                sourceCodes, "MyEntrypoint", "entrypointMethod", "ParentClass#methodOnParent@2");
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
                // "InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS LONGER
                // THAN 2 ARE SUPPORTED
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
    public void instanceNotInvokedOnInstantiatedOverrider_expectViolation(String instantiation) {
        // spotless:off
        String[] sourceCodes =
            new String[] {
                // The parent class should be virtual, its secondary method doing nothing interesting.
                String.format(SUBCLASS_OVERRIDDEN_SOURCES[0], "virtual", "true"),
                // The child class's version of the secondary method does nothing interesting.
                String.format(
                    SUBCLASS_OVERRIDDEN_SOURCES[1],
                    "true"),
                // Add a version of the invoker configured to instantiate the child class and
                // invoke its version of the method.
                configureInvoker("ChildClass", "new ChildClass()", instantiation + ".getBool()"),
                // Add an entrypoint.
                String.format(
                    COMPLEX_ENTRYPOINT,
                    "        InvokerClass ic = new InvokerClass();\n"
                  + "        ChildClass param = new ChildClass();\n"
                  + "        return ic.invokeMethod(param);\n")
            };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("ChildClass#getBool@2"),
                Collections.singletonList("ParentClass#getBool@2"));
    }

    /**
     * Tests for cases where a child class's method overrides a superclass's method, and then the
     * method is called on an instance of the child class that is being upcast to the parent class.
     * In this case, the child's version of the method should count as used. This paradigm is
     * exceedingly popular with abstract classes.
     *
     * @param instantiation - The instance of the parent class on which {@code getBool()} is
     *     invoked.
     */
    @ValueSource(
            strings = {
                // "InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS LONGER
                // THAN 2 ARE SUPPORTED
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
    public void instanceInvokedOnChildUpcastToClass_expectNoViolation(String instantiation) {
        // spotless:off
        String[] sourceCodes =
            new String[] {
                // The parent should be virtual, and its secondary method does nothing.
                String.format(
                    SUBCLASS_OVERRIDDEN_SOURCES[0],
                    "virtual",
                    "true"),
                // The child's version of the secondary method does nothing.
                String.format(SUBCLASS_OVERRIDDEN_SOURCES[1], "true"),
                // Add an invoker that instantiates child classes but upcasts them to parents.
                configureInvoker("ParentClass", "new ChildClass()", instantiation + ".getBool()"),
                // Add an entrypoint that initializes the child class but upcasts it to a parent.
                String.format(COMPLEX_ENTRYPOINT,
                    "        ParentClass param = new ChildClass();\n"
                  + "        InvokerClass ic = new InvokerClass();\n"
                  + "        return ic.invokeMethod(param);\n"
                    )
            };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("ChildClass#getBool@2"),
                Collections.singletonList("ParentClass#getBool@2"));
    }

    /**
     * Tests for cases where a child class's method implements an interface's method, and then the
     * method is called on an instance of the child class that is being upcast to the parent
     * interface. In this case, the child's version of the method should count as used.
     *
     * @param instantiation - The instance of the parent interface on which {@code getBool()} is
     *     invoked.
     */
    @ValueSource(
            strings = {
                // CRUCIAL NOTE: Interfaces cannot be instantiated directly, so there's no
                // constructor case.
                // "InvokerClass.staticProp", TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS LONGER
                // THAN 2 ARE SUPPORTED
                "staticProp",
                "InvokerClass.staticMethod()",
                "staticMethod()",
                "this.instanceProp",
                "instanceProp",
                "this.instanceMethod()",
                "instanceMethod()"
            })
    @ParameterizedTest(name = "{displayName}: Invocation via {0}")
    public void instanceInvokedOnChildUpcastToInterface_expectNoViolation(String instantiation) {
        // spotless:off
        String[] sourceCodes = new String[] {
            // Add an interface that exposes a method.
            "global interface MyInterface {\n"
          + "    boolean getBool();\n"
          + "}",
            // Add a class that implements the interface.
            "global class MyClass implements MyInterface {\n"
          + "    public boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Add an invoker that instantiates child classes but upcasts them to parents.
            configureInvoker("MyInterface", "new MyClass()", instantiation + ".getBool()"),
            // Add an entrypoint that initializes the child class but upcasts it to a parent.
            String.format(COMPLEX_ENTRYPOINT,
                "        MyInterface param = new MyClass();\n"
              + "        InvokerClass ic = new InvokerClass();\n"
              + "        return ic.invokeMethod(param);\n")
        };
        // spotless:on
        assertUsage(sourceCodes, "MyEntrypoint", "entrypointMethod", "MyClass#getBool@2");
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
                "this.middlemanProp.middlemanProp.endProp",
                "middlemanProp.middlemanProp.endProp",
                // property -> property -> end method
                "this.middlemanProp.middlemanProp.endMethod()",
                "middlemanProp.middlemanProp.endMethod()",
                // property -> method -> end property
                "this.middlemanProp.middlemanMethod().endProp",
                "middlemanProp.middlemanMethod().endProp",
                // property -> method -> end method
                "this.middlemanProp.middlemanMethod().endMethod()",
                "middlemanProp.middlemanMethod().endMethod()",
                // method -> property -> end property
                "this.instanceMethod().middlemanProp.endProp",
                "middlemanMethod().middlemanProp.endProp",
                // method -> property -> end method
                "this.middlemanMethod().middlemanProp.endMethod()",
                "middlemanMethod().middlemanProp.endMethod()",
                // method -> method -> end property
                "this.middlemanMethod().middlemanMethod().endProp",
                "middlemanMethod().middlemanMethod().endProp",
                // method -> method -> end method
                "this.middlemanMethod().middlemanMethod().endMethod()",
                "middlemanMethod().middlemanMethod().endMethod()"
            })
    @ParameterizedTest(name = "{displayName}: middleman chain {0}")
    @Disabled // TODO: ENABLE THESE TESTS ONCE REFERENCE CHAINS LONGER THAN 2 ARE SUPPORTED
    public void instanceInvokedViaMiddleman_expectNoViolation(String middlemanChain) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // This is the end class. It can be simple.
            String.format(SIMPLE_SOURCE, "public", "public", "true"),
            // Declare a class that will act as a middleman.
            "global class MiddlemanClass {\n"
            // Give the middleman class a self-referential property and a property holding an instantiated end class.
          + "    public MiddlemanClass middlemanProp = this;\n"
          + "    public MyClass endProp = new MyClass();\n"
            // Give the middleman class a method that returns itself, and a method that returns the end class.
          + "    public MiddlemanClass middlemanMethod() {\n"
          + "        return this;\n"
          + "    }\n"
          + "    public MyClass endMethod() {\n"
          + "        return new MyClass();\n"
          + "    }\n"
            // Give the middleman class a global entrypoint.
          + "    global boolean entrypointMethod() {\n"
          + "        return " + middlemanChain + ";\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertUsage(sourceCodes, "MiddlemanClass", "entrypointMethod", "MyClass#testedMethod@2");
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
    public void innerInstanceMethodCalledFromSiblingViaParameter_expectNoViolation(
            String scope, String paramType) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global class MyClass {\n"
          + "    global class MyInner1 {\n"
          + String.format("        %s boolean innerMethod1() {\n", scope)
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "    global class MyInner2 {\n"
          + String.format(
            "        public boolean innerMethod2(%s instance) {\n", paramType)
          + "            return instance.innerMethod1();\n"
          + "        }\n"
          + "    }\n"
          + "}\n",
            String.format(COMPLEX_ENTRYPOINT,
            "        MyClass.MyInner1 mc1 = new MyClass.MyInner1();\n"
          + "        MyClass.MyInner2 mc2 = new MyClass.MyInner2();\n"
          + "        return mc2.innerMethod2(mc1);\n")
        };
        // spotless:on
        assertUsage(
                sourceCodes, "MyEntrypoint", "entrypointMethod", "MyClass.MyInner1#innerMethod1@3");
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
    public void innerInstanceMethodCalledFromSiblingViaOwnProperty_expectNoViolation(
            String scope, String propType, String propRef) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global class MyClass {\n"
          + "    global class MyInner1 {\n"
          + String.format("        %s boolean innerMethod1() {\n", scope)
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "    global class MyInner2 {\n"
          + String.format("        public %s instance = new %s();\n", propType, propType)
          + "        public boolean innerMethod2() {\n"
          + String.format("            return %s.innerMethod1();\n", propRef)
          + "        }\n"
          + "    }\n"
          + "}\n",
            String.format(COMPLEX_ENTRYPOINT,
                "    MyClass.MyInner2 mc = new MyClass.MyInner2();\n"
              + "    return mc.innerMethod2();\n"
            )
        };
        // spotless:on
        assertUsage(
                sourceCodes, "MyEntrypoint", "entrypointMethod", "MyClass.MyInner1#innerMethod1@3");
    }

    /**
     * If a class has two inner classes, and one inner class's instance methods are invoked by
     * another inner class, then those methods count as used. Specific case: Instance available as
     * property of outer class.
     */
    @CsvSource({
        // Two options for implicit/explicit outer type reference for an instance property,
        // for each of the three visibility scopes.
        "public,  MyClass.MyInner1,  outer.outerInstanceProp",
        "protected,  MyClass.MyInner1,  outer.outerInstanceProp",
        "private,  MyClass.MyInner1,  outer.outerInstanceProp",
        "public,  MyInner1,  outer.outerInstanceProp",
        "protected,  MyInner1,  outer.outerInstanceProp",
        "private,  MyInner1,  outer.outerInstanceProp",
        // Four combinations of implicit/explicit outer class references,
        // for each of the three visibility scopes.
        "public, MyClass.MyInner1,  outerStaticProp",
        "protected, MyClass.MyInner1,  outerStaticProp",
        "private, MyClass.MyInner1,  outerStaticProp",
        "public, MyClass.MyInner1,  MyClass.outerStaticProp",
        "protected, MyClass.MyInner1,  MyClass.outerStaticProp",
        "private, MyClass.MyInner1,  MyClass.outerStaticProp",
        "public, MyInner1,  outerStaticProp",
        "protected, MyInner1,  outerStaticProp",
        "private, MyInner1,  outerStaticProp",
        "public, MyInner1, MyClass.outerStaticProp",
        "protected, MyInner1, MyClass.outerStaticProp",
        "private, MyInner1, MyClass.outerStaticProp"
    })
    @ParameterizedTest(
            name = "{displayName}: Method scope {0}; Declared as static {1}; Reference {2}")
    @Disabled // TODO: FIX THESE TESTS AND ENABLE THEM
    public void innerInstanceMethodCalledFromSiblingViaOuterProperty_expectNoViolation(
            String scope, String propType, String propRef) {
        // spotless:off
        String sourceCodes[] = new String[]{
            "global class MyClass {\n"
            + String.format("    public %s outerInstanceProp = new %s();\n", propType, propType)
            + String.format("    public static %s outerStaticProp = new %s();\n", propType, propType)
            + "    global class MyInner1 {\n"
            + String.format("        %s boolean innerMethod1() {\n", scope)
            + "            return true;\n"
            + "        }\n"
            + "    }\n"
            + "    global class MyInner2 {\n"
            // Give this method a parameter for some of the tests.
            + "        public boolean innerMethod2(MyClass outer) {\n"
            + String.format("            return %s.innerMethod1();\n", propRef)
            + "        }\n"
            + "    }\n"
            + "}\n",
            String.format(COMPLEX_ENTRYPOINT,
                "        MyClass mc = new MyClass();\n"
              + "        MyClass.MyInner2 mc2 = new MyClass.MyInner2();\n"
              + "        return mc2.innerMethod2(mc);\n")
        };
        // spotless:on
        assertUsage(
                sourceCodes, "MyEntrypoint", "entrypointMethod", "MyClass.MyInner1#innerMethod1@5");
    }

    /**
     * This test covers a weird edge case: If a class and its instance property both have an
     * instance method with the same name, then invoking {@code this.prop.theMethod()} needs to
     * count as a usage for {@code prop}'s method, not {@code this}'s.
     */
    @Test
    public void externalReferenceThisCollision_expectViolation() {
        // spotless:off
        String[] sourceCodes = new String[] {
            // The first class should be our generic unused class with a public method.
            String.format(SIMPLE_SOURCE, "public", "global", "true"),
            "global class SecondClass {\n"
            // Give the second class a property that's an instance of MyClass.
          + "    public MyClass prop = new MyClass();\n"
            // Also give it a method with the name "testedMethod", so it overlaps with MyClass.
          + "    public boolean testedMethod() {\n"
          + "        return false;\n"
          + "    }\n"
            // Give it a method that calls the property's method via `this`
          + "    public boolean invoker() {\n"
          + "        return this.prop.testedMethod();\n"
          + "    }\n"
          + "}",
            String.format(SIMPLE_ENTRYPOINT, "new SecondClass().invoker()")
        };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("MyClass#testedMethod@2"),
                Collections.singletonList("SecondClass#testedMethod@3"));
    }

    /**
     * These tests cover a weird edge case: Inner classes can be instantiated by outer/sibling
     * classes with just the inner name. If another outer class shares the same name, and both
     * classes have a method with the same signature, then the inner class takes precedence over the
     * outer class.
     *
     * @param referencerClass - The class performing the reference to the inner class.
     */
    @ValueSource(strings = {"OuterClass", "OuterClass.SiblingClass"})
    @ParameterizedTest(name = "{displayName}: Referenced by {0}")
    public void externalReferenceClassNameCollision_expectViolation(String referencerClass) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare an outer class
            "global class OuterClass {\n"
            // Declare a method that instantiates the inner class and calls its version of the colliding method.
          + "    public boolean causeCollision() {\n"
          + "        return new CollidingClass().getBoolean();\n"
          + "    }\n"
            // Declare an inner class to cause collisions.
          + "    public class CollidingClass {\n"
          + "        public boolean getBoolean() {\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
            // Declare another inner class that instantiates the tested inner class and calls its version of the colliding method.
          + "    public class SiblingClass {\n"
          + "        public boolean causeCollision() {\n"
          + "            return new CollidingClass().getBoolean();\n"
          + "        }\n"
          + "    }\n"
          + "}",
            // Declare an outer class with the same name as the inner class.
            "global class CollidingClass {\n"
            // Give it a method with the same signature as the inner method.
          + "    public boolean getBoolean() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            String.format(SIMPLE_ENTRYPOINT, "new " + referencerClass + "().causeCollision()")
        };
        // spotless:on

        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("OuterClass.CollidingClass#getBoolean@6"),
                Collections.singletonList("CollidingClass#getBoolean@2"));
    }

    /**
     * Helper method for easily configuring {@link #INVOKER_SOURCE}.
     *
     * @param hostClass - The class to be used in all class-related wildcards.
     * @param construction - The class to be used in constructing properties.
     * @param invocation - The invocation to be used in the invocation wildcard.
     */
    private static String configureInvoker(
            String hostClass, String construction, String invocation) {
        return String.format(
                INVOKER_SOURCE,
                hostClass,
                construction,
                hostClass,
                construction,
                hostClass,
                hostClass,
                hostClass,
                hostClass,
                construction,
                invocation);
    }
}
