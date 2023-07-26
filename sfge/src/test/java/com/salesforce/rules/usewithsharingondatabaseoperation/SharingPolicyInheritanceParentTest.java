package com.salesforce.rules.usewithsharingondatabaseoperation;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** tests when MyClass containing a database operation extends ParentClass */
public class SharingPolicyInheritanceParentTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // spotless:off
    private static final String PARENT_CLASS_SOURCE =
        "public %s class ParentClass {\n"
            + "void foo() {\n"
            + "}\n"
        + "}\n";

    private static final String MY_CLASS_SOURCE =
        "public %s class " + MY_CLASS + " extends ParentClass {\n"
            + "void foo(Account a) {\n"
            + "    %s;\n"
            + "}\n"
        + "}\n";
    // spotless:on

    private static final int SINK_LINE = 3;

    // there are 4 * 4 = 16 possible combinations with two sharing policies

    @MethodSource("provideNoViolationTests")
    @ParameterizedTest(name = "{displayName}: {0} in class with \"{1}\" with parent \"{2}\"")
    public void testInheritanceNoViolation(String operation, String policyOne, String policyTwo) {
        String[] sourceCode = {
            String.format(PARENT_CLASS_SOURCE, policyOne),
            String.format(MY_CLASS_SOURCE, policyTwo, operation),
        };
        assertNoViolation(RULE, sourceCode);
    }

    private static Stream<Arguments> provideNoViolationTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] noViolationSharingPolicyCombos = {
            // anything "inherited sharing" inherits from the calling class, overriding the parent (left column).
            // In this case, since MyClass is the entrypoint, it runs as "with sharing" (as noted in Apex documentation).
            {"with sharing      "," inherited sharing ",},
            {"inherited sharing "," inherited sharing ",},
            {"without sharing   "," inherited sharing ",},
            {"                  "," inherited sharing ",},
        // anything "with sharing" overrides its parent to be safe.
            {"with sharing      "," with sharing      ",},
            {"without sharing   "," with sharing      ",},
            {"inherited sharing "," with sharing      ",},
            {"                  "," with sharing      ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : noViolationSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim()));
                    }
                });

        return sb.build();
    }

    @MethodSource("provideViolationTests")
    @ParameterizedTest(name = "{displayName}: {0} in class with \"{1}\" with parent \"{2}\"")
    public void testInheritanceViolation(String operation, String policyOne, String policyTwo) {
        String[] sourceCode = {
            String.format(PARENT_CLASS_SOURCE, policyOne),
            String.format(MY_CLASS_SOURCE, policyTwo, operation),
        };
        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    private static Stream<Arguments> provideViolationTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] violationSharingPolicyCombos = {
            {"without sharing   ","                   ",},
            {"with sharing      "," without sharing   ",},
            {"without sharing   "," without sharing   ",},
            {"inherited sharing "," without sharing   ",},
            {"                  "," without sharing   ",},
            {"                  ","                   ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : violationSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim()));
                    }
                });

        return sb.build();
    }

    @MethodSource("provideWarningParentTests")
    @ParameterizedTest(name = "{displayName}: {0} in class with \"{1}\" with parent \"{2}\"")
    public void testInheritanceWarningParent(String operation, String policyOne, String policyTwo) {
        String[] sourceCode = {
            String.format(PARENT_CLASS_SOURCE, policyOne),
            String.format(MY_CLASS_SOURCE, policyTwo, operation),
        };
        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.PARENT, "ParentClass"));
    }

    private static Stream<Arguments> provideWarningParentTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] warningParentSharingPolicyCombos = {
            {"with sharing      ","                   ",},
            {"inherited sharing ","                   ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : warningParentSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim()));
                    }
                });

        return sb.build();
    }
}
