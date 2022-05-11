package com.salesforce.rules.fls.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CheckBasedObjectLevelFlsViolationTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    // TODO: support UPSERT operation
    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(
                        FlsValidationType.DELETE,
                        "Account a = new Account(Name = 'Acme Inc.');\n" + "delete a;\n"),
                getArguments(
                        FlsValidationType.MERGE,
                        "Account a1 = new Account(Name = 'Acme Inc.');\n"
                                + "Account a2 = new Account(Name = 'Acme');\n"
                                + "merge a1 a2;\n"),
                getArguments(
                        FlsValidationType.UNDELETE,
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account a = [SELECT Id, Name FROM Account WHERE Name = 'Acme Inc.' ALL ROWS];\n"
                                + "undelete a;\n"));
    }

    private static Arguments getArguments(
            FlsConstants.FlsValidationType validationType, String dmlOperation) {
        assertEquals(
                FlsConstants.AnalysisLevel.OBJECT_LEVEL,
                validationType.analysisLevel,
                "Invalid entry. Only Object-Level validations can be run in this test suite");
        return Arguments.of(validationType, validationType.checkMethod.first(), dmlOperation);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testDirectValidatorMethod(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
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
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testFieldLevelCheckIsNotAccepted(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account.Fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                TestUtil.USE_EXISTING_GRAPH,
                expect(getLineWithDmlStatement(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testObjectFromGlobalDescribe(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        SObjectDescribe objdes = Schema.getGlobalDescribe().get('Account').getDescribe();\n"
                        + "        if (objDes."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSObjectDescribeVariation1(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Account.SObjectType.getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testDescribeMapIncorrectObjectName(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        SObjectDescribe objDes = Schema.getGlobalDescribe().get('Contact').getDescribe();\n"
                        + "        if (objDes."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                TestUtil.USE_EXISTING_GRAPH,
                expect(getLineWithDmlStatement(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testObjectFromGlobalDescribe_describeInIf(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        SObject obj = Schema.getGlobalDescribe().get('Account');\n"
                        + "        if (obj.getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Disabled // TODO: support cases when broken down pieces are not necessarily MethodExpressions
    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSObjectDescribeFromNonMethod(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Schema.DescribeSObjectResult sobjDescribe = Schema.SObjectType.Account;\n"
                        + "        if (fieldDescribe."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Disabled // TODO: this breaks because Schema.getGlobalDescribe() without get(SObjectType)
    // breaks norms that are typically used for creating new SObjects dynamically.
    // This specifically breaks down at
    // ApexSingleValue.getObjectTypeForSObjectCreation(ApexSingleValue.java:166)
    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testValidationCheckFullyBrokenDown(
            FlsValidationType validationType, String validationCheck, String dmlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String, SObjectType> globalDescribe = Schema.getGlobalDescribe();\n"
                        + "        SObjectType accType = globalDescribe.get('Account');\n"
                        + "        DescribeSObjectResult objResult = accType.getDescribe();\n"
                        + "        boolean dmlCheck = objResult."
                        + validationCheck
                        + "();"
                        + "        if (dmlCheck) {\n"
                        + dmlOperation
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    /**
     * This ensures that the correct object is identified when a DML operation occurs on an item in
     * an array
     */
    @Test
    public void testDeleteObjectFromArray() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(Id objId) {\n"
                        + "    	Account[] accountsToDelete;\n"
                        + "		/* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "    	accountsToDelete = [SELECT ID FROM Account WHERE ID=:objId];\n"
                        + "    	delete accountsToDelete[0];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(6, FlsValidationType.DELETE, "Account"));
    }
}
