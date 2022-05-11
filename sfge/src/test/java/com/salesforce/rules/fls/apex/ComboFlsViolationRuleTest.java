package com.salesforce.rules.fls.apex;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.Test;

// TODO: Modify flow to run both rules at once
public class ComboFlsViolationRuleTest extends BaseFlsTest {

    private final AbstractPathBasedRule rule = ApexFlsViolationRule.getInstance();

    @Test
    public void testReadAndUpdate_onlyUpdateChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Status__c FROM Contact];\n"
                        + "        if (Schema.sObjectType.Contact.fields.Status__c.isUpdateable()) {\n"
                        + "            c.Status__c = 'New';\n"
                        + "            update c;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("Status__c"));
    }

    @Test
    public void testReadAndUpdate_bothChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Contact.fields.Status__c.isAccessible()) {\n"
                        + "            Contact c = [SELECT Status__c FROM Contact];\n"
                        + "            if (Schema.sObjectType.Contact.fields.Status__c.isUpdateable()) {\n"
                        + "               c.Status__c = 'New';\n"
                        + "               update c;\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testReadAndUpdate_onlyReadChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.sObjectType.Contact.fields.Status__c.isAccessible()) {\n"
                        + "            Contact c = [SELECT Status__c FROM Contact];\n"
                        + "            c.Status__c = 'New';\n"
                        + "            update c;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(6, FlsConstants.FlsValidationType.UPDATE, "Contact").withField("Status__c"));
    }
}
