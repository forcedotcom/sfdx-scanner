package com.salesforce.rules.usewithsharingondatabaseoperation;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * tests MyClass instantiating and calling a method with a database operation method in Two, which
 * extends One. Also, a few misc. tests below.
 */
public class SharingPolicyComplexInheritanceTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // spotless:off
    private static final String ONE_SOURCE =
        "public %s class One {\n"
            + "void op() {\n"
            + "}\n"
        + "}\n";

    private static final String TWO_SOURCE =
        "public %s class Two extends One {\n"
            + "void op(Account a) {\n"
            + "   %s;"
            + "}\n"
        + "}\n";

    private static final String MY_CLASS_SOURCE =
        "public %s class " + MY_CLASS + " {\n"
        + "void foo(Account a) {\n"
        + "    Two t = new Two();\n"
        + "    t.op(a);\n"
        + "}\n"
    + "}\n";
    // spotless:on

    private static final int SINK_LINE = 3;

    // there are 4 * 4 * 4 = 64 possible combinations with three sharing policies

    @MethodSource("provideNoViolationTests")
    @ParameterizedTest
    public void testInheritanceNoViolation(
            String operation, String policyOne, String policyTwo, String policyThree) {
        String[] sourceCode = {
            String.format(ONE_SOURCE, policyOne),
            String.format(TWO_SOURCE, policyTwo, operation),
            String.format(MY_CLASS_SOURCE, policyThree)
        };
        assertNoViolation(RULE, sourceCode);
    }

    private static Stream<Arguments> provideNoViolationTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] noViolationSharingPolicyCombos = {
            // when Two (center column) has "inherited sharing", the calling class (rightmost
            // column) must be "with sharing" or "inherited sharing" (entrypoint OK)
            {"with sharing      "," inherited sharing "," inherited sharing ",},
            {"without sharing   "," inherited sharing "," inherited sharing ",},
            {"inherited sharing "," inherited sharing "," inherited sharing ",},
            {"                  "," inherited sharing "," inherited sharing ",},
            {"with sharing      "," inherited sharing "," with sharing      ",},
            {"without sharing   "," inherited sharing "," with sharing      ",},
            {"inherited sharing "," inherited sharing "," with sharing      ",},
            {"                  "," inherited sharing "," with sharing      ",},
            // when Two (center column) has "with sharing," anything goes
            {"with sharing      "," with sharing      "," inherited sharing ",},
            {"without sharing   "," with sharing      "," inherited sharing ",},
            {"inherited sharing "," with sharing      "," inherited sharing ",},
            {"                  "," with sharing      "," inherited sharing ",},
            {"with sharing      "," with sharing      "," without sharing   ",},
            {"without sharing   "," with sharing      "," without sharing   ",},
            {"inherited sharing "," with sharing      "," without sharing   ",},
            {"                  "," with sharing      "," without sharing   ",},
            {"with sharing      "," with sharing      "," with sharing      ",},
            {"without sharing   "," with sharing      "," with sharing      ",},
            {"inherited sharing "," with sharing      "," with sharing      ",},
            {"                  "," with sharing      "," with sharing      ",},
            {"with sharing      "," with sharing      ","                   ",},
            {"without sharing   "," with sharing      ","                   ",},
            {"inherited sharing "," with sharing      ","                   ",},
            {"                  "," with sharing      ","                   ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : noViolationSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim(),
                                        policies[2].trim()));
                    }
                });

        return sb.build();
    }

    @MethodSource("provideViolationTests")
    @ParameterizedTest
    public void testInheritanceViolation(
            String operation, String policyOne, String policyTwo, String policyThree) {
        String[] sourceCode = {
            String.format(ONE_SOURCE, policyOne),
            String.format(TWO_SOURCE, policyTwo, operation),
            String.format(MY_CLASS_SOURCE, policyThree)
        };
        assertViolations(RULE, sourceCode, expect(SINK_LINE));
    }

    private static Stream<Arguments> provideViolationTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] violationSharingPolicyCombos = {
            // when Two (center column) has no declaration, look to its parent, One (left column)
            {"without sharing   ","                   "," inherited sharing ",},
            {"without sharing   ","                   "," without sharing   ",},
            {"without sharing   ","                   "," with sharing      ",},
            {"without sharing   ","                   ","                   ",},
            // when Two (center) has no declaration and One (parent, left col) has "inherited
            // sharing", look to calling class (right col). running as "without" or omitted =
            // violation.
            {"inherited sharing ","                   "," without sharing   ",},
            {"inherited sharing ","                   ","                   ",},
            // when neither Two nor One (center, left cols) have declarations, look to calling
            // class (right col). no declaration or without sharing means violation
            {"                  ","                   ","                   ",},
            {"                  ","                   "," without sharing   ",},
            // when Two (center col) has "inherited sharing" and it inherits "without sharing" or no
            // declaration from its calling class (right col), this is a violation
            {"with sharing      "," inherited sharing "," without sharing   ",},
            {"without sharing   "," inherited sharing "," without sharing   ",},
            {"inherited sharing "," inherited sharing "," without sharing   ",},
            {"                  "," inherited sharing "," without sharing   ",},
            {"with sharing      "," inherited sharing ","                   ",},
            {"without sharing   "," inherited sharing ","                   ",},
            {"inherited sharing "," inherited sharing ","                   ",},
            {"                  "," inherited sharing ","                   ",},
            // when Two runs "without sharing", it's a violation no matter what
            {"with sharing      "," without sharing   "," inherited sharing ",},
            {"without sharing   "," without sharing   "," inherited sharing ",},
            {"inherited sharing "," without sharing   "," inherited sharing ",},
            {"                  "," without sharing   "," inherited sharing ",},
            {"with sharing      "," without sharing   "," without sharing   ",},
            {"without sharing   "," without sharing   "," without sharing   ",},
            {"inherited sharing "," without sharing   "," without sharing   ",},
            {"                  "," without sharing   "," without sharing   ",},
            {"with sharing      "," without sharing   "," with sharing      ",},
            {"without sharing   "," without sharing   "," with sharing      ",},
            {"inherited sharing "," without sharing   "," with sharing      ",},
            {"                  "," without sharing   "," with sharing      ",},
            {"with sharing      "," without sharing   ","                   ",},
            {"without sharing   "," without sharing   ","                   ",},
            {"inherited sharing "," without sharing   ","                   ",},
            {"                  "," without sharing   ","                   ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : violationSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim(),
                                        policies[2].trim()));
                    }
                });

        return sb.build();
    }

    @MethodSource("provideWarningParentTests")
    @ParameterizedTest
    public void testInheritanceWarningParent(
            String operation, String policyOne, String policyTwo, String policyThree) {
        String[] sourceCode = {
            String.format(ONE_SOURCE, policyOne),
            String.format(TWO_SOURCE, policyTwo, operation),
            String.format(MY_CLASS_SOURCE, policyThree)
        };
        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.PARENT, "One"));
    }

    private static Stream<Arguments> provideWarningParentTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] warningParentSharingPolicyCombos = {
            // when Two (center col) has no declaration, but it inherits "with sharing" from One
            // (left col), it runs as "with sharing," but has a warning message of implicit
            // inheritance from its parent.
            {"with sharing      ","                   "," inherited sharing ",},
            {"with sharing      ","                   "," without sharing   ",},
            {"with sharing      ","                   "," with sharing      ",},
            {"with sharing      ","                   ","                   ",},
            // when Two (center col) has no declaration and One (left col) has "inherited sharing,"
            // and the calling class (right col) has "with sharing" or "inherited sharing," it runs
            // as "with sharing," but has a warning message of implicit inheritance from its parent.
            {"inherited sharing ","                   "," with sharing      ",},
            {"inherited sharing ","                   "," inherited sharing ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : warningParentSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim(),
                                        policies[2].trim()));
                    }
                });

        return sb.build();
    }

    @MethodSource("provideWarningCallingTests")
    @ParameterizedTest
    public void testInheritanceWarningCalling(
            String operation, String policyOne, String policyTwo, String policyThree) {
        String[] sourceCode = {
            String.format(ONE_SOURCE, policyOne),
            String.format(TWO_SOURCE, policyTwo, operation),
            String.format(MY_CLASS_SOURCE, policyThree)
        };
        assertViolations(
                RULE,
                sourceCode,
                expectWarning(SINK_LINE, SharingPolicyUtil.InheritanceType.CALLING, MY_CLASS));
    }

    private static Stream<Arguments> provideWarningCallingTests() {
        Stream.Builder<Arguments> sb = Stream.builder();
        Stream<Arguments> databaseOperations = provideSelectDatabaseOperations();

        // spotless:off
        String[][] warningCallingSharingPolicyCombos = {
            // When neither Two nor One (center, left cols) have explicit sharing policies, but they
            // inherit a safe sharing policy from the calling class (right col), it is safe to run,
            // but this needs a warning message about implicit inheritance from the calling class.
            {"                  ","                   "," inherited sharing ",},
            {"                  ","                   "," with sharing      ",},
        };
        // spotless:on

        databaseOperations.forEach(
                operationArg -> {
                    for (String[] policies : warningCallingSharingPolicyCombos) {
                        sb.add(
                                Arguments.of(
                                        operationArg.get()[0],
                                        policies[0].trim(),
                                        policies[1].trim(),
                                        policies[2].trim()));
                    }
                });

        return sb.build();
    }

    @Test
    public void testChainOfInheritedInheritedSharing() {
        // spotless:off
        String[] sourceCode = {
            "public inherited sharing class A {\n" +
                "void go(Account a) {\n" +
                "   insert a;" +
                "}\n" +
            "}\n",
            "public inherited sharing class B extends A {\n" +
                "   void go(Account acc) {\n" +
                "       A a = new A();\n" +
                "       a.go(acc);" +
                "   }\n" +
                "}\n",
            "public inherited sharing class C extends B {\n" +
                "   void go(Account a) {\n" +
                "       A b = new B();\n" +
                "       b.go(a);" +
                "   }\n" +
            "}\n",

            "public without sharing class GrandparentClass {}\n",
            "public without sharing class ParentClass extends GrandparentClass {}\n",

            // A, B, and C are all "inherited sharing" and since MY_CLASS is the entrypoint,
            // this should run as "with sharing" and not be an issue
            "public inherited sharing class " + MY_CLASS + " extends ParentClass {" +
                "void foo(Account a) {\n" +
                "   C c = new C();\n" +
                "   c.go(a);" +
                "}\n" +
            "}\n",
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testChainInheritedSharingImplicitInheritanceInCallingClass() {
        // spotless:off
        String[] sourceCode = {
            "public inherited sharing class A {\n" +
                "void go(Account a) {\n" +
                "   insert a;" +
                "}\n" +
            "}\n",
            "public inherited sharing class B extends A {\n" +
                "   void go(Account acc) {\n" +
                "       A a = new A();\n" +
                "       a.go(acc);" +
                "   }\n" +
            "}\n",
            "public inherited sharing class C extends B {\n" +
                "   void go(Account a) {\n" +
                "       A b = new B();\n" +
                "       b.go(a);" +
                "   }\n" +
            "}\n",

            "public without sharing class GrandparentClass {}\n",
            "public with sharing class ParentClass extends GrandparentClass {}\n",

            // A, B, and C are all "inherited sharing" and since MY_CLASS is the entrypoint,
            // this should be a warning
            "public class " + MY_CLASS + " extends ParentClass {" +
                "void foo(Account a) {\n" +
                "   C c = new C();\n" +
                "   c.go(a);" +
                "}\n" +
            "}\n",
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expectWarning(3, SharingPolicyUtil.InheritanceType.PARENT, "ParentClass"));
    }
}
