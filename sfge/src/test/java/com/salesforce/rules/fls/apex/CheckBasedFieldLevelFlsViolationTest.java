package com.salesforce.rules.fls.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CheckBasedFieldLevelFlsViolationTest extends BaseFlsTest {
    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(
                        FlsValidationType.READ,
                        ApexFlsViolationRule.getInstance(),
                        "Account a = [SELECT Name from Account];\n"),
                getArguments(
                        FlsValidationType.INSERT,
                        ApexFlsViolationRule.getInstance(),
                        "insert new Account(Name = 'Acme Inc.');\n"),
                getArguments(
                        FlsValidationType.UPDATE,
                        ApexFlsViolationRule.getInstance(),
                        "Account a = new Account();" + "a.Name = 'Acme Inc.';" + "update a;\n"));
    }

    private static Arguments getArguments(
            FlsConstants.FlsValidationType validationType,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        assertEquals(
                FlsConstants.AnalysisLevel.FIELD_LEVEL,
                validationType.analysisLevel,
                "Invalid entry. Only Field-Level validations can be run in this test suite");
        return Arguments.of(validationType, validationType.checkMethod.first(), rule, dmlOperation);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDirectValidatorMethod(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectLevelCheckIsntAccepted(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertNoViolation(rule, sourceCode);
//        assertViolations(rule, sourceCode, expect(4, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDescribeFromFieldValidator(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Account.Name.getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidatorMethodThroughObjectDescribeMap(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.SObjectType.Account.fields.getMap().get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidatorMethodThroughObjectDescribeAndFieldDescribeMap(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap().get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSObjectDescribeVariation_withFieldMap(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Account.SObjectType.getDescribe().fields.getMap().get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSObjectDescribeFieldDescribe(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Account.fields.Name.getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFieldMapFromVariable(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // TODO: Multiple variables
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGlobalDescribeObjectAndFieldMapVariable(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testObjectAndFieldFromMap(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        SObject obj = Schema.getGlobalDescribe().get('Account');\n"
                        + "        Map<String,Schema.SObjectField> m = obj.getDescribe().fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDescribeMapIncorrectObjectName(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.getGlobalDescribe().get('Contact').getDescribe().fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(5, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDescribeMapIncorrectFieldName(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                        + "        if (m.get('Phone').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(5, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: support values broken into smaller parts
    public void testValidationBrokenIntoMultipleVariables(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        String fieldName = 'Status';\n"
                        + "        String fullFieldName = fieldName + '__c';\n"
                        + "        SObject obj = Schema.getGlobalDescribe().get('Account');\n"
                        + "        Map<String,Schema.SObjectField> m = obj.getDescribe().getMap();\n"
                        + "        if (m.get(fullFieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: support cases when broken down pieces are not necessarily MethodExpressions
    public void testObjectAndFieldDescribe(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Schema.DescribeSObjectResult sobjDescribe = Schema.SObjectType.Account;\n"
                        + "        Schema.DescribeFieldResult fieldDescribe = sobjDescribe.fields.getMap().get('Name').getDescribe();\n"
                        + "        if (fieldDescribe."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // breaks norms that are typically used for creating new SObjects dynamically.
    // This specifically breaks down at
    // ApexSingleValue.getObjectTypeForSObjectCreation(ApexSingleValue.java:166)
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: this breaks because Schema.getGlobalDescribe() without get(SObjectType)
    public void testValidationCheckFullyBrokenDown(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String, SObjectType> globalDescribe = Schema.getGlobalDescribe();\n"
                        + "        SObjectType accType = globalDescribe.get('Account');\n"
                        + "        DescribeSObjectResult objResult = accType.getDescribe();\n"
                        + "        Map<String, SObjectField> fieldMap = objResult.fields.getMap();\n"
                        + "        SObjectField field = fieldMap.get('Name');\n"
                        + "        DescribeFieldResult fieldDescribe = field.getDescribe();\n"
                        + "        boolean dmlCheck = fieldDescribe."
                        + validationCheck
                        + "();"
                        + "        if (dmlCheck) {\n"
                        + "            "
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // breaks norms that are typically used for creating new SObjects dynamically.
    // This specifically breaks down at
    // ApexSingleValue.getObjectTypeForSObjectCreation(ApexSingleValue.java:166)
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationCheckPartiallyBrokenDown(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        DescribeSObjectResult objResult = Schema.getGlobalDescribe().get('Account').getDescribe();\n"
                        + "        Map<String, SObjectField> fieldMap = objResult.fields.getMap();\n"
                        + "        SObjectField field = fieldMap.get('Name');\n"
                        + "        DescribeFieldResult fieldDescribe = field.getDescribe();\n"
                        + "        boolean dmlCheck = fieldDescribe."
                        + validationCheck
                        + "();"
                        + "        if (dmlCheck) {\n"
                        + "            "
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValidationCheckAtFieldFromList(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		Account acc = new Account(Name = 'Acme Inc.');\n"
                        + "		List<Schema.SobjectField> fields = new List<Schema.SobjectField>{Schema.Account.fields.Name};\n"
                        + "		if (fields.get(0).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValidationCheckAtField_multiple(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        SObjectField field = Schema.Account.fields.Name;\n"
                        + "        DescribeFieldResult fieldDescribe = field.getDescribe();\n"
                        + "        boolean dmlCheck = fieldDescribe."
                        + validationCheck
                        + "();"
                        + "        if (dmlCheck) {\n"
                        + "            "
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
