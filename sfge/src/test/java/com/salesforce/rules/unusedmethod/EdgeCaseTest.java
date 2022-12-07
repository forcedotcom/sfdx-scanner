package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A collection of tests for weird edge cases. */
public class EdgeCaseTest extends BaseUnusedMethodTest {
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
                    // Declare an instance method on the inner class with the same name.
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
            // Declare a class that extends the parent class, thereby inheriting its static method.
            "global class ChildClass extends ParentClass {\n"
                    + "    global class InnerOfChild {\n"
                    // Declare an instance method on the inner class with the same name.
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
     * If an inner class extends another class and inherits a method whose name matches a static
     * method on the outer class, then invoking the method in the inner class without the `this`
     * keyword should still count as an invocation of the inherited method, not the outer static
     * one.
     */
    @ValueSource(strings = {"public", "public static"})
    @ParameterizedTest(name = "{displayName}: parent method {0}")
    @Disabled
    public void inheritedInnerClassOverlapsWithOuter_expectViolations(String scope) {
        String[] sourceCodes = {
            // Declare a parent class with a method.
            "public virtual class ParentClass {\n"
                    + String.format("    %s boolMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            // Declare an outer class with a static method whose name matches the parent class's
            // method.
            "public class OuterClass {\n"
                    + "    public static boolean boolMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Declare an inner class that extends the parent class,
                    // thereby inheriting its methods.
                    + "    public class InnerChild extends ParentClass {\n"
                    // Give the inner class a method that calls the inherited method.
                    + "        /* sfge-disable-stack UnusedMethodRule */\n"
                    + "        public boolean invoker() {\n"
                    + "            return boolMethod();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
        };
        // We expect the inherited method on the parent class to be called, not the static
        // method on the outer class.
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("boolMethod", v.getSourceVertexName());
                    assertEquals("OuterClass", v.getSourceVertex().getDefiningType());
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
}
