package com.salesforce.rules.unusedmethod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A set of tests involving methods being called on and/or by inner classes. These tests are
 * important because inner classes can be referenced in a variety of ways by their outer/sibling
 * classes.
 */
public class UnusedMethodRuleTest_InnerClassCalls extends UnusedMethodRuleTest_BaseClass {

    /* =============== SECTION 1: STATIC METHODS =============== */
    // Inner classes can't have static methods, but they can implicitly
    // invoke their outer class's static methods in the same way the outer
    // class can.

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
        assertNoViolations(sourceCode, 1);
    }

    /* =============== SECTION 2: INSTANCE METHODS =============== */

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

    /* =============== SECTION 3: CONSTRUCTOR METHODS =============== */

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
        assertNoViolations(sourceCode, 1);
    }
}
