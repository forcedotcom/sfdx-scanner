package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SharingPolicyAbstractTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // TODO: fix and enable these tests.

    /**
     * Test a method declared in an abstract class "with sharing" but is then implemented in a class
     * "without sharing." Disabled for now because this bug might have to do with path expansion.
     * See {@link
     * com.salesforce.rules.unusedmethod.InstanceMethodsTest#instanceInvokedByOriginatingSuperclass_expectNoViolation(String,
     * String)}
     */
    @Disabled
    @Test
    public void testWithSharingAbstractWithoutSharingImplementation() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing abstract class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                "   doDbOp();\n" +
                "} \n" +
                "abstract void doDbOp();\n" +
                "}\n",

            "public without sharing class ChildClass extends " + MY_CLASS + " {\n" +
                "   void doDbOp() {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(3));
    }

    @Disabled
    @Test
    public void testWithoutSharingAbstractWithSharingImplementation() {
        // spotless:off
        String[] sourceCode = {
            "public without sharing abstract class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                "   doDbOp();\n" +
                "} \n" +
                "abstract void doDbOp();\n" +
                "}\n",

            "public with sharing class ChildClass extends " + MY_CLASS + " {\n" +
                "   void doDbOp() {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Disabled
    @Test
    public void testWithoutSharingAbstractWithoutSharingImplementation() {
        // spotless:off
        String[] sourceCode = {
            "public without sharing abstract class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                "   doDbOp();\n" +
                "} \n" +
                "abstract void doDbOp();\n" +
                "}\n",

            "public without sharing class ChildClass extends " + MY_CLASS + " {\n" +
                "   void doDbOp() {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(3));
    }
}
