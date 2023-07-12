package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** simple sharing policy tests */
public class SharingPolicySimpleTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // spotless:off
    private static final String SIMPLE_SOURCE =
            "public %s class " + MY_CLASS + " {\n"
            + "     void foo(Account a) {\n"
            + "         %s;"
            + "     }\n"
            + "}\n";
    // spotless:on

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleNoDeclaration(String operation) {
        String sourceCode = String.format(SIMPLE_SOURCE, "", operation);

        assertViolations(RULE, sourceCode, expect(3));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleWithSharing(String operation) {
        String sourceCode = String.format(SIMPLE_SOURCE, "with sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleWithoutSharing(String operation) {
        String sourceCode = String.format(SIMPLE_SOURCE, "without sharing", operation);

        assertViolations(RULE, sourceCode, expect(3));
    }

    @MethodSource("provideAllDatabaseOperations")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleInheritedSharingOk(String operation) {
        String sourceCode = String.format(SIMPLE_SOURCE, "inherited sharing", operation);

        assertNoViolation(RULE, sourceCode);
    }
}
