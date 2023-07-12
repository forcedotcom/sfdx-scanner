package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * tests instantiating and calling a method with a database operation method inside a class that is
 * a subclass of the current class
 */
public class SharingPolicySubclassesTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // spotless:off
    private static final String SUBCLASS_SOURCE =
        "public %s class " + MY_CLASS + " {\n" +
            "void foo(Account a) {\n" +
            "   SubClass sub = new SubClass();\n" +
            "   sub.doDbOp(a);\n" +
            "}\n" +
            "public %s class SubClass {\n" +
            "   void doDbOp(Account a) {\n" +
            "       %s;\n" +
            "   }\n" +
            "}\n" +
        "}\n";
    // spotless:on
    /**
     * sink line in the source code above, in the method MyClass.SubClass#doDbOp(Account). Use when
     * expecting a violation, since that's the only place a database opeartion occurs.
     */
    private static final int SINK_LINE = 8;

    // TODO consolidate these into expectWarning, expectNoViolation, etc. like in
    // SharingPolicyComplexInheritanceTest.java

    // PARENT - NO DECLARATION

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNoDeclarationParentWithSharingSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "", "with sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNoDeclarationParentWithoutSharingSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "", "without sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNoDeclarationParentInheritedSharingSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "", "inherited sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNoDeclarationParentNoDeclarationSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "", "", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    // PARENT - WITH SHARING

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingParentNoDeclarationSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "with sharing", "", operation);

        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.CALLING, MY_CLASS));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingParentWithSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "with sharing", "with sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingParentInheritedSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "with sharing", "inherited sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSharingParentWithoutSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "with sharing", " without sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    // PARENT - WITHOUT SHARING

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingParentWithoutSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "without sharing", "without sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingParentWithSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "without sharing", "with sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingParentInheritedSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "without sharing", "inherited sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithoutSharingParentNoDeclarationSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "without sharing", "", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    // PARENT - INHERITED SHARING

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInheritedSharingParentWithoutSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "inherited sharing", "without sharing", operation);

        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInheritedSharingParentWithSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "inherited sharing", "with sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInheritedSharingParentInheritedSharingSubclass(String operation) {
        String sourceCode =
                String.format(SUBCLASS_SOURCE, "inherited sharing", "inherited sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInheritedSharingParentNoDeclarationSubclass(String operation) {
        String sourceCode = String.format(SUBCLASS_SOURCE, "inherited sharing", "", operation);

        //            assertNoViolation(RULE, sourceCode);
        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.CALLING, MY_CLASS));
    }
}
