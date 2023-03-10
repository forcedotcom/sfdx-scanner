package com.salesforce.rules.unusedmethod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for instance methods that are called via middlemen on other objects. These tests are
 * complicated enough to justify having their own class.
 */
public class MiddlemanTest extends BaseUnusedMethodTest {

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
}
