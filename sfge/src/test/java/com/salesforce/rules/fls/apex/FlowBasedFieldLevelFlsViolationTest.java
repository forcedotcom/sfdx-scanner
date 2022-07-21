package com.salesforce.rules.fls.apex;

import static com.salesforce.TestUtil.USE_EXISTING_GRAPH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FlowBasedFieldLevelFlsViolationTest extends BaseFlsTest {

    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(
                        "INSERT_KeyValue_Type1",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.INSERT,
                        "insert new Account(Name = 'Acme Inc.');\n",
                        "insert new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n",
                        "insert new Contact(FirstName = 'Foo');\n"),
                getArguments(
                        "INSERT_KeyValue_Type2",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.INSERT,
                        "Account a = new Account();\n" + "a.name = 'Acme Inc.'\n;" + "insert a;\n",
                        "Account a = new Account();\n"
                                + "a.name = 'Acme Inc.';\n"
                                + "a.phone = '415-555-1212';\n"
                                + "insert a;\n",
                        "Contact c = new Contact();\n" + "c.FirstName = 'Foo';\n" + "insert c;\n"),
                getArguments(
                        "INSERT_KeyValue_Type3",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.INSERT,
                        "Account a = new Account(Name = 'Acme Inc.');\n" + "insert a;\n",
                        "Account a = new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n"
                                + "insert a;\n",
                        "Contact c = new Contact(FirstName = 'Foo');\n" + "insert c;\n"),
                getArguments(
                        "UPDATE",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.UPDATE,
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account acc = [SELECT Id, Name from Account];\n"
                                + "acc.Name = 'Acme Inc.';\n"
                                + "update acc;\n",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account acc = [SELECT Id, Name from Account];\n"
                                + "acc.Name = 'Acme Inc.';\n"
                                + "acc.Phone = '415-555-1212';\n"
                                + "update acc;\n",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Contact c = [SELECT Id, FirstName from Contact];\n"
                                + "c.FirstName = 'Foo';\n"
                                + "update c;\n"),
                getArguments(
                        "UPDATE_KeyValueCombo",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.UPDATE,
                        "Account acc = new Account(Name = 'Acme Inc.');\n"
                                + "acc.Name = 'Acme Inc.2';\n"
                                + "update acc;\n",
                        "Account acc = new Account(Name = 'Acme Inc.');\n"
                                + "acc.Name = 'Acme Inc.2';\n"
                                + "acc.Phone = '415-555-1212';\n"
                                + "update acc;\n",
                        "Contact c = new Contact(FirstName = 'Foo');\n"
                                + "c.FirstName = 'Foo2';\n"
                                + "update c;\n"),
                getArguments(
                        "READ",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.READ,
                        "Account acc = [SELECT Name FROM Account];\n",
                        "Account acc = [SELECT Name, Phone FROM Account];\n",
                        "Contact c = [SELECT FirstName FROM Contact];\n"));
    }

    private static Arguments getArguments(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        assertEquals(
                AnalysisLevel.FIELD_LEVEL,
                validationType.analysisLevel,
                "Invalid entry. Only Field-Level validations can be run in this test suite");
        return Arguments.of(
                operationName,
                rule,
                validationType,
                validationType.checkMethod.first(),
                dmlOperationLine,
                dmlOperationWithTwoFields,
                dmlOperationAnotherObj);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSimpleCheck(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleCheckAndTrue(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() && true) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        // Appending "&& true" didn't change the satisfiability of the condition, so no violations
        // should have occurred.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleCheckOrTrue(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() || true) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        // Appending "|| true" means the condition is always satisfied, so a violation should occur.
        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void testSimpleCheckOrFalse(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "() || false) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        // Appending "|| false" didn't change the satisfiability of the condition, so no violations
        // should have occurred.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationOnIncorrectObject(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Contact.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationOnIncorrectField(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationOnNonExistentField(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Foo."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSimpleNegationCheck(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testMultipleFieldValidation(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + "               "
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testMultipleFieldValidation_missingField(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "               "
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationFromAMethod(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       if (verifyDml()) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "    }\n"
                        + "    public boolean verifyDml() {\n"
                        + "       return Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "();\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationFromAMethod_incorrectCheck(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       if (!verifyDml()) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "       return Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationWithEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (!Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationEarlyReturnForEach_withExtraFields(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone', 'Foo'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (!Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverwrittenValueEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           fieldToCheck = 'Foo';\n"
                        + // This would be a bug in the user's code
                        "           if (!Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationIncorrectEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithIncorrectFieldsEarlyReturnFromForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Foo'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (!Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithEarlyReturnWithMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithIncorrectEarlyReturnWithMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMultipleMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           if (m.get('Phone').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithEarlyReturnWithMultipleMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + "        if (!m.get('Phone').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationWithTwoFields
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMultipleObjects(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> a = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        Map<String,Schema.SObjectField> c = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "        if (a.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           if (c.get('FirstName').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMultipleIncorrectObjectsSwapped(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> a = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        Map<String,Schema.SObjectField> c = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "        if (a.get('FirstName').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           if (c.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        // Both inserts are unsafe
        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account").withField("Name"),
                expect(getMaximumLine(validationType), validationType, "Contact")
                        .withField("FirstName"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMultipleIncorrectObjectsCopyPaste(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> a = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        Map<String,Schema.SObjectField> c = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "        if (a.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           if (c.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        // Only the Contact insert is unsafe
        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Contact")
                        .withField("FirstName"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithIncorrectMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithMultipleIncorrectMap(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               "
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegativeIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegativeNegativeIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithEarlyReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIfWithIncorrectEarlyReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherMethodWithException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherMethodWithIncorrectException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherMethodWithReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void foo() {\n"
                    + "        verifyDml();\n"
                    + dmlOperationLine
                    + "    }\n"
                    + "    public void verifyDml() {\n"
                    + "        if (Schema.sObjectType.Account.fields.Name."
                    + validationCheck
                    + "()) {\n"
                    + "           return;\n"
                    + "        }\n"
                    + "        throw new MyException();\n"
                    + "    }\n"
                    + "}\n"
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherMethodWithIncorrectReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + "        throw new MyException();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGuardedByExceptionInOtherParameterizedMethodWithException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml('Name');\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           verifyDml(fieldToCheck);\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEachAsParameter(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fields = new String [] {'Name', 'Phone'};\n"
                        + "       verifyDml(fields);\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "    public void verifyDml(String [] fieldsToCheck) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (!m.get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               throw new MyException();\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEachAsInlineParameter(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       verifyDml(new String [] {'Name', 'Phone'});\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "    public void verifyDml(String [] fieldsToCheck) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (!m.get(fieldToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               throw new MyException();\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // TODO: Test array of variables

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEachIncorrectFields(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           verifyDml(fieldToCheck);\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEachIncorrectException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           verifyDml(fieldToCheck);\n"
                        + "       }\n"
                        + "       "
                        + dmlOperationWithTwoFields
                        + "   }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodWithIncorrectException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml('Name');\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodWithIncorrectField(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml('Phone');\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByIncorrectException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           throw new MyException();\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlWithNoOpIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           System.debug('Not safe');\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedCheckBothPositive(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedPositiveFollowedByNegativeSafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           if (!Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedPositiveFollowedByNegativeUnsafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "           if (!Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedNegativeFollowedByPositiveSafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedNegativeFollowedByPositiveUnsafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithTwoFields
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafeObjectLevelCheckWithException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "			throw new Exception();\n"
                        + "        }\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: Handle method invocation on forloop values
    public void testListOfSObjectFields_singleLevel(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        List<Schema.SObjectField> fields = new List<Schema.SObjectFields>{Schema.Account.fields.Name,Schema.Account.fields.Phone};\n"
                        + "        checkValidation(fields);\n"
                        + "    "
                        + dmlOperationWithTwoFields
                        + "    }\n"
                        + "	public void checkValidation(List<Schema.SObjectField> fields) {\n"
                        + "		for (Schema.SObjectField field: fields) {\n"
                        + "			DescribeFieldResult fieldResult = field.getDescribe();\n"
                        + "			if (!fieldResult."
                        + validationCheck
                        + "()) {\n"
                        + "				throw new AccessException();\n"
                        + "			}\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: Handle method invocation on forloop values
    public void testListOfSObjectFields(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationWithTwoFields,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        List<Schema.SObjectField> fields = new List<Schema.SObjectFields>{Schema.Account.fields.Name,Schema.Account.fields.Phone};\n"
                        + "        checkValidation(fields);\n"
                        + "    "
                        + dmlOperationWithTwoFields
                        + "    }\n"
                        + "	public void checkValidation(List<Schema.SObjectField> fields) {\n"
                        + "		for (Schema.SObjectField field: fields) {\n"
                        + "			if (!field.getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "				throw new AccessException();\n"
                        + "			}\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testCopyOfIndeterminantSObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(List<Account> input) {\n"
                        + "		List<Account> toInsert = new List<Account>();\n"
                        + "		for (Account acc: input) {\n"
                        + "			toInsert.add(acc);\n"
                        + "		}\n"
                        + "		insert toInsert;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(7, FlsValidationType.INSERT, "Account").withField(SoqlParserUtil.UNKNOWN));
    }

    @Test
    public void testCopyOfDeterminantSObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		List<Account> input = new List<Account>(new Account(Name='Acme Inc.'));\n"
                        + "		executeInsert(input);\n"
                        + "    }\n"
                        + "    public void executeInsert(List<Account> input) {\n"
                        + "		List<Account> toInsert = new List<Account>();\n"
                        + "		for (Account acc: input) {\n"
                        + "			toInsert.add(acc);\n"
                        + "		}\n"
                        + "		insert toInsert;\n"
                        + "    }\n"
                        + "}\n";

        // TODO: Currently, we don't transfer properties when looked up through
        //   single values in forloop value list. Until that's fixed, this would
        //   return fields as "Unknown".
        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(11, FlsValidationType.INSERT, "Account").withField(SoqlParserUtil.UNKNOWN));
    }
}
