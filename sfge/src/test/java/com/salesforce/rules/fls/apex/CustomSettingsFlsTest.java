package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CustomSettingsFlsTest extends BaseFlsTest {

    private final ApexFlsViolationRule rule;

    public CustomSettingsFlsTest() {
        rule = ApexFlsViolationRule.getInstance();
    }

    private static Stream<Arguments> provideCustomSettingFlsParam() {
        return Stream.of(
                Arguments.of("Insert", FlsConstants.FlsValidationType.INSERT),
                Arguments.of("Update", FlsConstants.FlsValidationType.UPDATE),
                Arguments.of("Delete", FlsConstants.FlsValidationType.DELETE));
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeSimple(String testName, FlsConstants.FlsValidationType validationType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       MySettings__c ms = MySettings__c.getOrgDefaults();\n"
                        + "       ms.MyBool__c = true;\n"
                        + "       "
                        + validationType.name()
                        + " ms;\n"
                        + "    }\n"
                        + "}\n";

        // By default, custom settings require no CRUD/FLS checks. So this is fine, and no
        // violations should be detected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testExcessiveCrudCheck(
            String testName, FlsConstants.FlsValidationType validationType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       MySettings__c ms = MySettings__c.getOrgDefaults();\n"
                        + "       ms.MyBool__c = true;\n"
                        + "		if (Schema.SObjectType.MySettings__c."
                        + validationType.checkMethod.first()
                        + "()) {\n"
                        + "       	"
                        + validationType.name()
                        + " ms;\n"
                        + "		}\n"
                        + "    }\n"
                        + "}\n";

        // By default, custom settings require no CRUD/FLS checks. So the CRUD check is unnecessary,
        // but not strictly unsafe.
        // Therefore, no violation is expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testExcessiveFlsCheck(
            String testName, FlsConstants.FlsValidationType validationType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       MySettings__c ms = MySettings__c.getOrgDefaults();\n"
                        + "       ms.MyBool__c = true;\n"
                        + "		if (Schema.SObjectType.MySettings__c.Fields.MyBool__c."
                        + validationType.checkMethod.first()
                        + "()) {\n"
                        + "       	"
                        + validationType.name()
                        + " ms;\n"
                        + "		}\n"
                        + "    }\n"
                        + "}\n";

        // By default, custom settings require no CRUD/FLS checks. So the FLS check is unnecessary,
        // but not strictly unsafe.
        // Therefore, no violation is expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafeValueFromGetAll(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "public void foo() {\n"
                    + "	String csName = 'hello';\n"
                    + "	Map<String, My_Custom_Settings__c> csMap = My_Custom_Settings__c.getAll();\n"
                    + "	"
                    + validationType.name()
                    + " csMap.get(csName);\n"
                    + "}\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So no violations are expected
        // here.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueFromGetAll(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "public void foo() {\n"
                    + "	String csName = 'hello';\n"
                    + "	Map<String, My_Custom_Settings__c> csMap = My_Custom_Settings__c.getAll();\n"
                    + "	if (Schema.SObjectType.My_Custom_Settings__c."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "		"
                    + validationType.name()
                    + " csMap.get(csName);\n"
                    + "	}\n"
                    + "}\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueFromNewInstance(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "public void foo() {\n"
                    + "	My_Custom_Settings__c.getOrgDefaults();\n"
                    + // to indicate that we are dealing with a custom setting
                    "	My_Custom_Settings__c cs = new My_Custom_Settings__c(name = 'Acme Inc.');\n"
                    + "	if (Schema.SObjectType.My_Custom_Settings__c."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "		"
                    + validationType.name()
                    + " cs;\n"
                    + "	}\n"
                    + "}\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueFromList(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	private void unusedMethod() {\n"
                    + "		My_Custom_Settings__c.getOrgDefaults();\n"
                    + "	}\n"
                    + "    public void foo(List<My_Custom_Settings__c> toInsert) {\n"
                    + "    	if (My_Custom_Settings__c.SObjectType.getDescribe()."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "    		"
                    + validationType.name()
                    + " toInsert;\n"
                    + "    	}\n"
                    + "    }\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueMethodParam(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	private void unusedMethod() {\n"
                    + "		Cust_Set__c.getOrgDefaults();\n"
                    + "	}\n"
                    + "    public static void foo(Cust_Set__c custSet) {\n"
                    + "    	if (Cust_Set__c.SObjectType.getDescribe()."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "    		"
                    + validationType.name()
                    + " custSet;\n"
                    + "    	}\n"
                    + "    }\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueFromSoql(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	private void unusedMethod() {\n"
                    + "		Cust_Set__c.getOrgDefaults();\n"
                    + "	}\n"
                    + "    public static void foo() {\n"
                    + "		/* sfge-disable-next-line ApexFlsViolationRule */\n"
                    + "		List<Cust_Set__c> custSet = [SELECT Name from Cust_Set__c];\n"
                    + "    	if (Cust_Set__c.SObjectType.getDescribe()."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "    		"
                    + validationType.name()
                    + " custSet;\n"
                    + "    	}\n"
                    + "    }\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideCustomSettingFlsParam")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeValueFromForLoop(
            String testName, FlsConstants.FlsValidationType validationType) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void foo() {\n"
                    + "		List<Cust_Set__c> custSets = Cust_Set__c.getAll();\n"
                    + "    	if (Cust_Set__c.SObjectType.getDescribe()."
                    + validationType.checkMethod.first()
                    + "()) {\n"
                    + "			for (Cust_Set__c custSet : custSets) {\n"
                    + "    			"
                    + validationType.name()
                    + " custSet;\n"
                    + "			}\n"
                    + "    	}\n"
                    + "    }\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS checks. So the check is excessive, but
        // not wrong. No violations
        // expected.
        assertNoViolation(rule, sourceCode);
    }
}
