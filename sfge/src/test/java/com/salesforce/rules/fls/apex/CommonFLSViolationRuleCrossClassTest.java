package com.salesforce.rules.fls.apex;

import static com.salesforce.TestUtil.USE_EXISTING_GRAPH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
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

public class CommonFLSViolationRuleCrossClassTest extends BaseFlsTest {
    // Creating parameters for tests. These parameters will be pass to class constructor in the same
    // order
    // they are defined in. Important to note that refactoring the constructor signature will need
    // manual changes
    // on the order of objects.
    // TODO: add DELETE operation. Will need to access isDeletable through object names
    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(
                        FlsValidationType.INSERT,
                        ApexFlsViolationRule.getInstance(),
                        "insert new Account(Name = 'Acme Inc.');\n",
                        "insert new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n"),
                getArguments(
                        FlsValidationType.READ,
                        ApexFlsViolationRule.getInstance(),
                        "Account acc = [SELECT Name FROM Account];\n",
                        "Account acc = [SELECT Name, Phone FROM Account];\n"),
                getArguments(
                        FlsValidationType.UPDATE,
                        ApexFlsViolationRule.getInstance(),
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account acc = [SELECT Id, Name from Account];\n"
                                + "acc.Name = 'Acme Inc.';\n"
                                + "update acc;\n",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account acc = [SELECT Id, Name from Account];\n"
                                + "acc.Name = 'Acme Inc.';\n"
                                + "acc.Phone = '415-555-1212';\n"
                                + "update acc;\n"));
    }

    private static Arguments getArguments(
            FlsConstants.FlsValidationType validationType,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        assertEquals(
                FlsConstants.AnalysisLevel.FIELD_LEVEL,
                validationType.analysisLevel,
                "Invalid entry. Only Field-Level validations can be run in this test suite");
        return Arguments.of(
                validationType,
                validationType.checkMethod.first(),
                rule,
                dmlOperationWithOneField,
                dmlOperationWithTwoFields);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionCrossClassStatic(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    public static void verifyOperation() {\n"
                    + "        if (!Schema.sObjectType.Account.fields.Name."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionCrossClassInstance(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass f = new FLSClass();\n"
                    + "        f.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!Schema.sObjectType.Account.fields.Name."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithDescribeMapInConstructorCrossClassInstance(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass f = new FLSClass();\n"
                    + "       f.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass() {\n"
                    + "       m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithDescribeParameterizedMapInConstructorCrossClassInstance(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void
            testDmlSurroundedByIfWithDescribeParameterizedMapInConstructorCrossMultipleClassInstances(
                    FlsValidationType validationType,
                    String validationCheck,
                    AbstractPathBasedRule rule,
                    String dmlOperationWithOneField,
                    String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    +
                    // Make sure that we can tell the difference between different instances
                    "       FLSClass a = new FLSClass('Account');\n"
                    + "       FLSClass c = new FLSClass('Contact');\n"
                    + "       a.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testFieldNamePassedToVerifyOperation(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass a = new FLSClass('Account');\n"
                    + "       a.verifyOperation('Name');\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation(String fieldName) {\n"
                    + "        if (!m.get(fieldName).getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void
            testDmlSurroundedByIfWithDescribeParameterizedMapInConstructorCrossMultipleClassInstancesIncorrect(
                    FlsValidationType validationType,
                    String validationCheck,
                    AbstractPathBasedRule rule,
                    String dmlOperationWithOneField,
                    String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    +
                    // Make sure that we can tell the difference between different instances
                    "       FLSClass a = new FLSClass('Account');\n"
                    + "       FLSClass c = new FLSClass('Contact');\n"
                    + "       c.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void
            testDmlSurroundedByIfWithDescribeParameterizedMapInConstructorCrossClassInstanceIncorrectObject(
                    FlsValidationType validationType,
                    String validationCheck,
                    AbstractPathBasedRule rule,
                    String dmlOperationWithOneField,
                    String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass f = new FLSClass('Contact');\n"
                    + "       f.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionCrossClassInstanceWithConstructorInfo(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass f = new FLSClass();\n"
                    + "        f.verifyOperation();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass() {\n"
                    + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSameMethodCalledMultipleTimes(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyOperation('Name');\n"
                    + "       f.verifyOperation('Phone');\n"
                    + dmlOperationWithTwoFields
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation(String fieldName) {\n"
                    + "        if (!m.get(fieldName).getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSameMethodCalledMultipleTimesIncorrectPhoneField(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyOperation('Name');\n"
                    + "       f.verifyOperation('Ph');\n"
                    + dmlOperationWithTwoFields
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyOperation(String fieldName) {\n"
                    + "        if (!m.get(fieldName).getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Phone"));
    }

    // TODO: @Test Support static methods
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void testDmlGuardedByExceptionCrossClassInstanceWithStaticConstructorInfo(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass f = new FLSClass();\n"
                    + "        f.verifyCreateable();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private static final Map<String,Schema.SObjectField> m;\n"
                    + "    static {\n"
                    + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionCrossClassInstanceWithIncorrectConstructorInfo(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass f = new FLSClass();\n"
                    + "        f.verifyCreateable();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass() {\n"
                    + "       m = Schema.SObjectType.Contact.fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        if (!m.get('Name').getDescribe()."
                    + validationCheck
                    + "()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlGuardedByExceptionCrossInnerClassStatic(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass.InnerClass.verifyCreateable();\n"
                    + dmlOperationWithOneField
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "   public class InnerClass {\n"
                    + "       public static void verifyCreateable() {\n"
                    + "           if (!Schema.sObjectType.Account.fields.Name."
                    + validationCheck
                    + "()) {\n"
                    + "               throw new MyException();\n"
                    + "           }\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n",
        };

        assertNoViolation(rule, sourceCode);
    }

    // This test finds no violations, because there is only a single constructor
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithMapInConstructorInOnlyConstructor(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass() {\n"
                        + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // This test finds violations because the constructor is not explicitly called and there are
    // multiple constructors
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithMapInConstructorInMultipleConstructors(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass(String a) {\n"
                        + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "    }\n"
                        + "    public MyClass(String a, String b) {\n"
                        + "       m = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                9,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    // This test finds no violations, because there is only a single constructor
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithDescribeMapInOnlyConstructor(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass() {\n"
                        + "       m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // This test finds violations because the constructor is not explicitly called and there are
    // multiple constructors
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithDescribeMapInMultipleConstructors(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass() {\n"
                        + "       m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                        + "    }\n"
                        + "    public MyClass(String s) {\n"
                        + "       m = Schema.getGlobalDescribe().get(s).getDescribe().fields.getMap();\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                9,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    /** The m varialbe is assigned conflicting values. The code can't determine if it is safe. */
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithAmbiguousMapInConstructor(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass(String a) {\n"
                        + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "    }\n"
                        + "    public MyClass(Integer a) {\n"
                        + "       m = Schema.SObjectType.Contact.fields.getMap();\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                9,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlSurroundedByIfWithAmbiguousNullMapInConstructor2(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final Map<String,Schema.SObjectField> m;\n"
                        + "    public MyClass(String a) {\n"
                        + "       m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "    }\n"
                        + "    public MyClass(Integer a) {\n"
                        + "       m = null;\n"
                        + "    }\n"
                        + "    public void foo() {\n"
                        + "        if (m.get('Name').getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + dmlOperationWithOneField
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        assertViolations(
                rule,
                USE_EXISTING_GRAPH,
                "foo",
                "MyClass",
                TestUtil.FIRST_FILE,
                9,
                expect(getMaximumLine(validationType), validationType, "Account")
                        .withField("Name"));
    }

    @Disabled
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void dmlAndCheckInDifferentClasses(
            FlsValidationType validationType,
            String validationCheck,
            AbstractPathBasedRule rule,
            String dmlOperationWithOneField,
            String dmlOperationWithTwoFields) {
        String sourceCode[] = {
            "public class DmlCheckEnforcer {\n"
                    + "    private String objectName;\n"
                    + "    private String fieldName;\n"
                    + "    \n"
                    + "    public DmlCheckEnforcer(String objName, String fName) {\n"
                    + "        this.objectName = objName;\n"
                    + "        this.fieldName = fName;\n"
                    + "    }\n"
                    + "\n"
                    + "    Schema.DescribeSObjectResult getObjectDescribe() {\n"
                    + "        return Schema.getGlobalDescribe().get(objectName).getDescribe();\n"
                    + "    }\n"
                    + "    \n"
                    + "    Schema.DescribeFieldResult getFieldDescribe() {\n"
                    + "        return getObjectDescribe().Fields.getMap().get(fieldName).getDescribe();\n"
                    + "    }\n"
                    + "\n"
                    + "    public boolean hasDmlAccess() {\n"
                    + "        return getFieldDescribe()."
                    + validationCheck
                    + "();\n"
                    + "    }\n"
                    + "}\n",
            "public class MyClass {\n"
                    + "    void foo() {\n"
                    + "        DmlCheckEnforcer dce = new DmlCheckEnforcer('Account', 'Name');\n"
                    + "        if (dce.hasDmlAccess()) {\n"
                    + dmlOperationWithOneField
                    + "        }\n"
                    + "    }\n"
                    + "}"
        };

        assertNoViolation(rule, sourceCode);
    }
}
