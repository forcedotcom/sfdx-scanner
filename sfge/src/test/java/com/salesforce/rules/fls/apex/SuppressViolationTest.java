package com.salesforce.rules.fls.apex;

import com.salesforce.ArgumentsUtil;
import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SuppressViolationTest extends BaseFlsTest {
    private final AbstractPathBasedRule readRule = ApexFlsViolationRule.getInstance();

    public static Stream<Arguments> sfdcDisableDirectives() {
        List<Arguments> arguments = new ArrayList<>();

        for (String directive :
                Arrays.asList(
                        "sfge-disable",
                        "sfge-disable ApexFlsViolationRule",
                        "sfge-disable ApexFlsViolationRule, OtherRule")) {
            arguments.add(Arguments.of("/* " + directive + " */"));
            arguments.add(Arguments.of("// " + directive));
        }

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    public static Stream<Arguments> sfdcDisableNextLineDirectives() {
        List<Arguments> arguments = new ArrayList<>();

        for (String directive :
                Arrays.asList(
                        "sfge-disable-next-line",
                        "sfge-disable-next-line ApexFlsViolationRule",
                        "sfge-disable-next-line ApexFlsViolationRule, OtherRule")) {
            arguments.add(Arguments.of("/* " + directive + " */"));
            arguments.add(Arguments.of("// " + directive));
        }

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    public static Stream<Arguments> sfdcDisableStackDirectives() {
        List<Arguments> arguments = new ArrayList<>();

        for (String directive :
                Arrays.asList(
                        "sfge-disable-stack",
                        "sfge-disable-stack ApexFlsViolationRule",
                        "sfge-disable-stack ApexFlsViolationRule, OtherRule")) {
            arguments.add(Arguments.of("/* " + directive + " */"));
            arguments.add(Arguments.of("// " + directive));
        }

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource(value = "sfdcDisableDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testClassApexCrudViolation(String directive) {
        String[] sourceCode = {
            directive
                    + "\n"
                    + "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };
        assertNoViolation(readRule, sourceCode);
    }

    @MethodSource(value = "sfdcDisableDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testClassAdditional(String directive) {
        String[] sourceCode = {
            "@SuppressWarnings('SFGE.Doc')\n"
                    + directive
                    + "\n"
                    + "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };
        assertNoViolation(readRule, sourceCode);
    }

    @Test
    public void testClassUnrelatedOtherRule() {
        String[] sourceCode = {
            "/* sfge-disable OtherRule */\n"
                    + "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertViolations(
                readRule,
                sourceCode,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                3,
                expect(4, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @Test
    public void testClassUnrelatedOther() {
        String[] sourceCode = {
            "/* sfge-unrelated OtherRule */\n"
                    + "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertViolations(
                readRule,
                sourceCode,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                3,
                expect(4, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @MethodSource(value = "sfdcDisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testMethod(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + directive
                    + "\n"
                    + "   public static void foo() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertNoViolation(readRule, sourceCode);
    }

    @MethodSource(value = "sfdcDisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testMethodAnnotationIsPopped(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       execSoql1();\n"
                    + "       execSoql2();\n"
                    + "   }\n"
                    + directive
                    + "\n"
                    + // This annotation should only last for the single method call
                    "   public static void execSoql1() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "   public static void execSoql2() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertViolations(
                readRule,
                sourceCode,
                expect(11, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @MethodSource(value = "sfdcDisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testInheritMethodStackDirective(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + directive
                    + "\n"
                    + "   public static void foo() {\n"
                    + "       execSoql();\n"
                    + "   }\n"
                    + "   public static void execSoql() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertNoViolation(readRule, sourceCode);
    }

    @MethodSource(value = "sfdcDisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testInheritMethodCallExpressionStackDirective(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + directive
                    + "\n"
                    + "       execSoql();\n"
                    + "   }\n"
                    + "   public static void execSoql() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertNoViolation(readRule, sourceCode);
    }

    @MethodSource(value = "sfdcDisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testCallExpressionStackDirectiveOnlyAppliesToNextLine(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + directive
                    + "\n"
                    + "       execSoql();\n"
                    + "       execSoql();\n"
                    + // This one will throw a violation
                    "   }\n"
                    + "   public static void execSoql() {\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertViolations(
                readRule,
                sourceCode,
                expect(8, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @MethodSource(value = "sfdcDisableNextLineDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testInline(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "		"
                    + directive
                    + "\n"
                    + "       List<Account> accounts = [SELECT Id, Name FROM ACCOUNT];\n"
                    + "   }\n"
                    + "}"
        };

        assertNoViolation(readRule, sourceCode);
    }
}
