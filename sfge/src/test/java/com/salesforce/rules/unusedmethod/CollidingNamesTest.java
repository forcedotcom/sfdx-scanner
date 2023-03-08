package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * There are a bunch of weird edge cases that can happen when class names, variable names, and
 * method names collide. These tests validate those. They are complicated enough to justify being
 * their own file.
 */
public class CollidingNamesTest extends BaseUnusedMethodTest {

    /**
     * If an outer class defines an inner class, it can reference the class with just the inner
     * class name instead of the full class name, even if an unrelated outer class shares the same
     * name. In this case, the methods invoked are the ones on the inner class.
     */
    @Test
    // TODO: INSTEAD OF SIMPLY ENABLING THIS TEST, CONSIDER MOVING IT TO InstanceMethodsTest.java!
    @Disabled
    public void innerClassNameOverlapsWithOuter_expectViolations() {
        // spotless:off
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
        // spotless:on
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
}
