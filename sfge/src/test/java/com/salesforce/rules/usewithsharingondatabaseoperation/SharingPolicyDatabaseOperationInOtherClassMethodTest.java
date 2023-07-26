package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * tests MyClass instantiating OtherClass and calling DatabaseClass#doDbOp which contains a database
 * operation.
 */
public class SharingPolicyDatabaseOperationInOtherClassMethodTest
        extends BaseUseWithSharingOnDatabaseOperationTest {
    // spotless:off
    private static final String MAIN_CLASS =
        "public %s class " + MY_CLASS + " {\n" +
            "void foo(Account a) {\n" +
            "   OtherClass sub = new OtherClass();\n" +
            "   sub.doDbOp(a);\n" +
            "}\n" +
        "}\n";

    private static final String OTHER_CLASS =
        "public %s class OtherClass {\n" +
            "   void doDbOp(Account a) {\n" +
            "       %s;\n" +
            "   }\n" +
            "}\n";
    // spotless:on

    /** sink line for database operation in OtherClass */
    private static final int SINK_LINE = 3;

    // TODO consolidate these into expectWarning, expectNoViolation, etc. like in
    // SharingPolicyComplexInheritanceTest.java

    // MAIN: WITH SHARING

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingMainWithSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "with sharing"),
                    String.format(OTHER_CLASS, "with sharing", operation)
                };

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingMainWithoutSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "with sharing"),
                    String.format(OTHER_CLASS, "without sharing", operation)
                };

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingMainInheritedSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "with sharing"),
                    String.format(OTHER_CLASS, "inherited sharing", operation)
                };

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingMainNoDeclarationInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "with sharing"),
                    String.format(OTHER_CLASS, "", operation)
                };

        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.CALLING, MY_CLASS));
    }

    // MAIN: WITHOUT SHARING

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingMainWithSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "without sharing"),
                    String.format(OTHER_CLASS, "with sharing", operation)
                };

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingMainWithoutSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "without sharing"),
                    String.format(OTHER_CLASS, "without sharing", operation)
                };

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingMainInheritedSharingInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "without sharing"),
                    String.format(OTHER_CLASS, "inherited sharing", operation)
                };

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideSelectDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingMainNoDeclarationInstantiated(String operation) {
        String[] sourceCode =
                new String[] {
                    String.format(MAIN_CLASS, "without sharing"),
                    String.format(OTHER_CLASS, "", operation)
                };

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }
}
