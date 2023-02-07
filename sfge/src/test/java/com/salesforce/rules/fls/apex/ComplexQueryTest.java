package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @CsvSource({"SECURITY_ENFORCED", "USER_MODE"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeInnerQuery_WithMode(String mode) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + String.format(
                                "        [SELECT Id, Name, (SELECT FirstName from Contact) from Account WITH %s];\n",
                                mode)
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
