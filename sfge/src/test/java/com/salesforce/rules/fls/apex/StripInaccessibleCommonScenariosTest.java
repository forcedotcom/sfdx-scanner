package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StripInaccessibleCommonScenariosTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testValidCase() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision accessDecision = Security.stripInaccessible(AccessType.CREATABLE, accounts);\n"
                        + "		List<Account> sanitizedData = accessDecision.getRecords();\n"
                        + "       insert sanitizedData;\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
