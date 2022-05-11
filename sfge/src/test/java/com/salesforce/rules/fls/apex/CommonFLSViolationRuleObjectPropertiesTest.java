package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests FLS rules using SObjects */
public class CommonFLSViolationRuleObjectPropertiesTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    public static Stream<Arguments> input() {
        return Stream.of(
                getArguments(FlsValidationType.INSERT), getArguments(FlsValidationType.UPDATE));
    }

    private static Arguments getArguments(FlsValidationType validationType) {
        return Arguments.of(
                validationType, validationType.name(), validationType.checkMethod.first());
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testUnsafe(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       "
                        + operation
                        + " obj;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(5, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIf(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableObjectType(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       String objectType = 'Account';\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectType).newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableIncorrectObjectType(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       String objectType = 'Account';\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectType).newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       if (Schema.sObjectType.Contact.fields.FirstName."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(7, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableFieldName(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       String objectType = 'Account';\n"
                        + "       String fieldName = 'Name';\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectType).newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       if (Schema.sObjectType.Account.fields.Name."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableIncorrectFieldName(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       String objectType = 'Account';\n"
                        + "       String fieldName = 'Name';\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectType).newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       if (Schema.sObjectType.Account.fields.Phone."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(8, validationType, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableFieldNameAsParameter(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(String fieldName) {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       if (Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap().get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableFieldAndObjectNameAsParameter(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(String objectName, String fieldName) {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectName).newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       if (Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap().get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    /**
     * This test overwrites the fieldName value after it is passed to SObject#put. This means that
     * the #isCreateable check can't be guaranteed.
     */
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testDmlSurroundedByIfWithVariableFieldNameAsParameterIncorrect(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(String fieldName) {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       fieldName = 'Phone';\n"
                        + "       if (Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap().get(fieldName).getDescribe()."
                        + validationCheck
                        + "()) {\n"
                        + "            "
                        + operation
                        + " obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(7, validationType, "Account").withField("fieldName"));
    }

    /** Method name is used as the object name if the method isn't resolvable. */
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testIndeterminantObjectNameMethodAssignment(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       String objectName = getObjectName();\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectName).newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       "
                        + operation
                        + " obj;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, validationType, "getObjectName").withField("Name"));
    }

    /**
     * Method name is used as the object name if the method is resolvable, but the return value is
     * indeterminant
     */
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testIndeterminantObjectNameInlineMethod(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        +
                        // This is a bit contrived. #getOrganizationName exists in the stubs, but we
                        // don't know what it will return
                        "       SObject obj = Schema.getGlobalDescribe().get(UserInfo.getOrganizationName()).newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       "
                        + operation
                        + " obj;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(5, validationType, "UserInfo.getOrganizationName").withField("Name"));
    }

    /** A variable's name is used for the field name if the variable can't be resolved */
    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {1}")
    public void testIndeterminantFieldNameVariable(
            FlsValidationType validationType, String operation, String validationCheck) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(List<FieldDefinition> fds) {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "		String fieldName = null;\n"
                        + "		for(FieldDefinition fd : fds) {\n"
                        + "    		if (fd.DataType.contains('Something')) {\n"
                        + "    			fieldName = fd.QualifiedApiName;\n"
                        + "    		}\n"
                        + "    	}\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       "
                        + operation
                        + " obj;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(11, validationType, "Account").withField("fd.QualifiedApiName"));
    }
}
