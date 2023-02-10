package com.salesforce.rules.fls.apex;

import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CheckBasedFlsComplexBooleansTest extends BaseFlsTest {
    public static Stream<Arguments> multipleFieldsInput() {
        return input(
                "Account a = [SELECT Name, Phone FROM Account];\n",
                "insert new Account(Name = 'Acme inc.', Phone = '867-5309');\n",
                "Account a = new Account();"
                        + "a.Name = 'Acme inc';"
                        + "a.Phone = '867-5309';"
                        + "update a;\n");
    }

    public static Stream<Arguments> singleFieldInput() {
        return input(
                "Account a = [SELECT Name FROM Account];\n",
                "insert new Account(Name = 'Acme inc.');\n",
                "Account a = new Account();" + "a.Name = 'Acme inc';" + "update a;\n");
    }

    public static Stream<Arguments> input(
            String readAction, String insertAction, String updateAction) {
        return Stream.of(
                getArguments(
                        FlsValidationType.READ, ApexFlsViolationRule.getInstance(), readAction),
                getArguments(
                        FlsValidationType.INSERT, ApexFlsViolationRule.getInstance(), insertAction),
                getArguments(
                        FlsValidationType.UPDATE,
                        ApexFlsViolationRule.getInstance(),
                        updateAction));
    }

    private static Arguments getArguments(
            FlsConstants.FlsValidationType validationType,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        return Arguments.of(validationType, validationType.checkMethod.first(), rule, dmlOperation);
    }

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    // ========== NO BOOLEANS ==========
    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testPartialValidation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // Only one of the fields is being validated, so a violation should occur.
        assertViolations(
                rule,
                sourceCode,
                // Inside the condition, only one field is being validated, so the other field
                // should be part of a violation.
                expect(4, validationType, "Account").withField("Phone"),
                // Outside the condition, neither field is being validated, so they should both be
                // in the violation.
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    // ========== AND-EXPRESSIONS ==========

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleProperAndedValidation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // The only violation should be the one outside the condition, since the AND requires both
        // conditions to be satisfied.
        assertViolations(
                rule,
                sourceCode,
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testAndedValidationOfWrongFields(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Fax."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                // Within the condition, we're validating 'Fax' when we should be validating
                // 'Phone'.
                expect(4, validationType, "Account").withField("Phone"),
                // Outside the condition, we're not validating anything.
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegatedFullValidation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!(Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";
        // The condition is a proper validation that's being negated, so there should be a violation
        // within the condition
        // and nothing else.
        assertViolations(
                rule,
                sourceCode,
                expect(4, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithTwiceNegatedAnd(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!!(Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // The condition is doubly-negated, so validation is happening. Therefore there should be a
        // validation outside it.
        assertViolations(
                rule,
                sourceCode,
                expect(7, validationType, "Account").withField("Name").withField("Phone"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithTriplyNegatedAnd(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!!!(Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // The triply-negated AND is the same as a negated AND, so there should be a violation
        // within the condition and
        // not one outside it.
        assertViolations(
                rule,
                sourceCode,
                expect(4, validationType, "Account").withField("Name").withField("Phone"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithInnerNegation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // One of the checks is being negated, so validation isn't happening.
        assertViolations(
                rule,
                sourceCode,
                // Within the condition, one check is being negated, so that should be part of a
                // violation.
                expect(4, validationType, "Account").withField("Name"),
                // Outside the condition, no assumptions can be made about CRUD, so both fields
                // should be part of the violation.
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithInnerDoubleNegation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // The double-negative cancels, so there should only be a violation for outside of the
        // condition.
        assertViolations(
                rule,
                sourceCode,
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithInnerSingleAndDoubleNegation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && !!Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // One of the checks is being negated, so validation isn't happening.
        assertViolations(
                rule,
                sourceCode,
                // Within the condition, one check is negated, so that should be part of a
                // violation.
                expect(4, validationType, "Account").withField("Name"),
                // Outside the condition, neither check is happening, so both should be part of a
                // violation.
                expect(7, validationType, "Account").withField("Name").withField("Phone"));
    }

    // ========== OR-EXPRESSIONS ==========

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInsufficientOr(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() || Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                // Both operations should have violations for all involved fields, since the OR only
                // requires one condition
                // to be satisfied.
                expect(4, validationType, "Account").withField("Phone").withField("Name"),
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegatedOr(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!(Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() || Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                // Both operations should have violations for all involved fields, since the
                // negation doesn't change the overall
                // satisfiability of the OR.
                expect(4, validationType, "Account").withField("Phone").withField("Name"),
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegativeValidationWithDeMorgansLaw(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!Schema.SObjectTYpe.Account.fields.Name."
                        + validationCheck
                        + "() || !Schema.SObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // By DeMorgan's Law, (!X || !Y) == !(X && Y), so proper validation is happening outside of
        // the condition, even
        // if it doesn't look like it. But inside the condition there should still be a violation.
        assertViolations(
                rule,
                sourceCode,
                expect(4, validationType, "Account").withField("Name").withField("Phone"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testPositiveValidationWithDeMorgansLaw(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (!(!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() || !Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // By DeMorgan's Law, !(!X || !Y) == X && Y, so validation is happening within the condition
        // even if it doesn't
        // look like it. But outside the condition, there should still be a violation.
        assertViolations(
                rule,
                sourceCode,
                expect(7, validationType, "Account").withField("Name").withField("Phone"));
    }

    // ========== MIXED OR'S AND AND'S ==========

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInsufficientOrWithinAnd(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "() || Schema.sObjectType.Account.fields.Fax."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                // The nested OR renders the validation of 'Phone' insufficient, so there should be
                // a violation within the
                // condition  for that.
                expect(4, validationType, "Account").withField("Phone"),
                // Outside of the condition, nothing's validated.
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFullValidationWithinInsufficientOr(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Fax."
                        + validationCheck
                        + "() || (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                // The nested OR renders all validation insufficient, so there should be a violation
                // within the condition
                // for that.
                expect(4, validationType, "Account").withField("Phone").withField("Name"),
                // Outside of the condition, nothing's validated.
                expect(7, validationType, "Account").withField("Phone").withField("Name"));
    }

    @MethodSource("multipleFieldsInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegatedValidationWithExtraneousOr(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		if (Schema.sObjectType.Account.fields.Fax."
                        + validationCheck
                        + "() || !(Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "() && Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "())) {\n"
                        + dmlOperation
                        + "			return;\n"
                        + "		}\n"
                        + dmlOperation
                        + "	}\n"
                        + "}\n";

        // The negated validation means that the outer operation is safe, but the extraneous OR
        // renders the inner operation
        // unsafe entirely.
        assertViolations(
                rule,
                sourceCode,
                expect(4, validationType, "Account").withField("Phone").withField("Name"));
    }

    // ========== EQUALITY/INEQUALITY TESTS ==========

    @MethodSource("singleFieldInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationEqualsTrue(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name." + validationCheck + "() == true) {\n"
                        + "            " + dmlOperation
                        + "            return;\n"
                        + "        }\n"
                        + "        " + dmlOperation
                        + "    }\n"
                        + "}";
        // spotless:on
        assertViolations(rule, sourceCode, expect(7, validationType, "Account").withField("Name"));
    }

    @MethodSource("singleFieldInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationEqualsFalse(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name." + validationCheck + "() == false) {\n"
                        + "            " + dmlOperation
                        + "            return;\n"
                        + "        }\n"
                        + "        " + dmlOperation
                        + "    }\n"
                        + "}";
        // spotless:on
        assertViolations(rule, sourceCode, expect(4, validationType, "Account").withField("Name"));
    }

    @MethodSource("singleFieldInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationNotEqualsTrue(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name." + validationCheck + "() != true) {\n"
                        + "            " + dmlOperation
                        + "            return;\n"
                        + "        }\n"
                        + "        " + dmlOperation
                        + "    }\n"
                        + "}";
        // spotless:on
        assertViolations(rule, sourceCode, expect(4, validationType, "Account").withField("Name"));
    }

    @MethodSource("singleFieldInput")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationNotEqualsFalse(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name." + validationCheck + "() != false) {\n"
                        + "            " + dmlOperation
                        + "            return;\n"
                        + "        }\n"
                        + "        " + dmlOperation
                        + "    }\n"
                        + "}";
        // spotless:on
        assertViolations(rule, sourceCode, expect(7, validationType, "Account").withField("Name"));
    }
}
