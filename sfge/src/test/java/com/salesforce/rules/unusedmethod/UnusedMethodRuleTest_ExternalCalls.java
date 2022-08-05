package com.salesforce.rules.unusedmethod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for methods called on classes that are wholly unrelated to the class where they're defined.
 */
public class UnusedMethodRuleTest_ExternalCalls extends UnusedMethodRuleTest_BaseClass {

    /* =============== SECTION 1: STATIC METHODS =============== */
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
        assertNoViolations(sourceCodes, 1);
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
                    + "    public boolean b2 = DefiningClass.someOtherMethod();\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 2);
    }

    /* =============== SECTION 2: INSTANCE METHODS =============== */

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
        assertNoViolations(sourceCodes, 1);
    }

    /* =============== SECTION 3: CONSTRUCTOR METHODS =============== */

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
        assertNoViolations(sourceCodes, 1);
    }
}
