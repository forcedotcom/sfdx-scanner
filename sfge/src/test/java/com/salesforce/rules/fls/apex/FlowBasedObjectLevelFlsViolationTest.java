package com.salesforce.rules.fls.apex;

import static com.salesforce.TestUtil.USE_EXISTING_GRAPH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FlowBasedObjectLevelFlsViolationTest extends BaseFlsTest {

    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(
                        "DELETE",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.DELETE,
                        "Account a = new Account(Id = '001abc000000001', Name = 'Acme Inc.');\n"
                                + "delete a;\n",
                        "Contact c = new Contact(Id = '004ghi000000002', Name = 'Foo');\n"
                                + "delete c;\n"),
                getArguments(
                        "MERGE",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.MERGE,
                        "Account a1 = new Account(Name = 'Acme Inc.');\n"
                                + "Account a2 = new Account(Name = 'Acme');\n"
                                + "merge a1 a2;\n",
                        "Contact c1 = new Contact(FirstName = 'Foo');\n"
                                + "Contact c2 = new Contact(FirstName = 'Bar');\n"
                                + "merge c1 c2;\n"),
                getArguments(
                        "UNDELETE",
                        ApexFlsViolationRule.getInstance(),
                        FlsValidationType.UNDELETE,
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account a = [SELECT Id, Name FROM Account WHERE Name = 'Acme Inc' ALL ROWS];\n"
                                + "undelete a;\n",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Contact c = [SELECT Id, FirstName FROM Contact WHERE FirstName = 'Foo' ALL ROWS];\n"
                                + "undelete c;\n"));
    }

    private static Arguments getArguments(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        assertEquals(
                AnalysisLevel.OBJECT_LEVEL,
                validationType.analysisLevel,
                "Invalid entry. Only Object-Level validations can be run in this test suite");
        return Arguments.of(
                operationName,
                rule,
                validationType,
                validationType.checkMethod.first(),
                dmlOperationLine,
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithSimpleCheck(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
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
    public void testNegativeIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNegativeNegativeIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!!Schema.sObjectType.Account."
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
    public void testWithNoOpIf(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationOnIncorrectObject(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Contact."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationFromAMethod(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       if (verifyDml()) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "       return Schema.sObjectType.Account."
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
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       if (!verifyDml()) {\n"
                        + dmlOperationLine
                        + "       }\n"
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "       return Schema.sObjectType.Account."
                        + validationCheck
                        + "();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testValidationWithEarlyReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
    public void testValidationWithEarlyReturn_incorrect(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGuardedByException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
    public void testValidationWithMultipleObjects(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Contact."
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
    public void testDmlGuardedByExceptionInOtherMethodWithException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (Schema.sObjectType.Account."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherMethodWithReturn(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void foo() {\n"
                    + "        verifyDml();\n"
                    + dmlOperationLine
                    + "    }\n"
                    + "    public void verifyDml() {\n"
                    + "        if (Schema.sObjectType.Account."
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
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml();\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml() {\n"
                        + "        if (!Schema.sObjectType.Account."
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
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionInOtherParameterizedMethodWithException(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        verifyDml('Account');\n"
                        + dmlOperationLine
                        + "    }\n"
                        + "    public void verifyDml(String objectName) {\n"
                        + "        if (!Schema.getGlobalDescribe().get(objectName).getDescribe()."
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
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testValidationWithEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact'};\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "           if (!Schema.getGlobalDescribe().get(sObjectToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testValidationEarlyReturnForEach_withExtraSObjects(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact', 'Foo'};\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "           if (!Schema.getGlobalDescribe().get(sObjectToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testOverwrittenValueEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact'};\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "           sObjectCheck = 'Foo';\n"
                        + // This would be a bug in the user's code
                        "           if (!Schema.getGlobalDescribe().get(sObjectToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testValidationIncorrectEarlyReturnForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact'};\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "           if (Schema.getGlobalDescribe().get(sObjectToCheck).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "               return;\n"
                        + "           }\n"
                        + "       }\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEach(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact'};\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "           verifyDml(sobjectToCheck);\n"
                        + "       }\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "    public void verifyDml(String objectName) {\n"
                        + "        if (!Schema.getGlobalDescribe().get(objectName).getDescribe()."
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
    @Disabled // FlsValidationWrapper should handle situation with multiple objects. Will match with
    // handling List of type SObject
    public void testDmlGuardedByExceptionInOtherParameterizedMethodForEachAsParameter(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String [] sObjectsToCheck = new String [] {'Account', 'Contact'};\n"
                        + "       verifyDml(sobjectsToCheck);\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "   }\n"
                        + "    public void verifyDml(String [] sobjectsToCheck) {\n"
                        + "       for (String sObjectToCheck : sObjectsToCheck) {\n"
                        + "        if (!Schema.getGlobalDescribe().get(sObjectToCheck).getDescribe()."
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
    public void testWithNestedCheckBothPositive(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Contact."
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
    public void testWithNestedPositiveFollowedByNegativeSafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "           if (!Schema.sObjectType.Contact."
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
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "           if (!Schema.sObjectType.Contact."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Contact"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testWithNestedNegativeFollowedByPositiveSafe(
            String operationName,
            AbstractPathBasedRule rule,
            FlsValidationType validationType,
            String validationCheck,
            String dmlOperationLine,
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Contact."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Account."
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
            String dmlOperationAnotherObj) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (!Schema.sObjectType.Account."
                        + validationCheck
                        + "()) {\n"
                        + "           if (Schema.sObjectType.Contact."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationLine
                        + dmlOperationAnotherObj
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMinimumLine(validationType), validationType, "Account"));
    }
}
