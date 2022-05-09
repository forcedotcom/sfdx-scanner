package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ComplexQueryTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testUnsafeInnerQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        [SELECT Id, Name, (SELECT FirstName from Contact) from Account];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account").withField("Name"),
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testUnsafeInnerQuery_SecurityEnforced() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        [SELECT Id, Name, (SELECT FirstName from Contact WITH SECURITY_ENFORCED) from Account];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @Test
    public void testSafeInnerQuery_SecurityEnforced() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        [SELECT Id, Name, (SELECT FirstName from Contact) from Account WITH SECURITY_ENFORCED];\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
