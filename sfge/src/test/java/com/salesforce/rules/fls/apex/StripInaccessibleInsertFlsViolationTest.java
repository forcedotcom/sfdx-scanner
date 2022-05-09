package com.salesforce.rules.fls.apex;

import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StripInaccessibleInsertFlsViolationTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testUnsafe_SingleItem() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       insert accounts;\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testSafe_DoubleCheckForSingleItem() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, accounts);"
                        + "       SObjectAccessDecision sd1 = Security.stripInaccessible(AccessType.CREATABLE, accounts);"
                        + "       insert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_DoubleCheckForSingleItem_reverse() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, accounts);"
                        + "       SObjectAccessDecision sd1 = Security.stripInaccessible(AccessType.CREATABLE, accounts);"
                        + "       insert sd1.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafe_checkAfterDml() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       insert accounts;\n"
                        + "       Security.stripInaccessible(AccessType.CREATABLE, accounts);"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testUnsafe_SingleType_UnmatchedAccessType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.READABLE, accounts);"
                        + "       insert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(6, FlsValidationType.INSERT, "Account")
                        .withField(
                                SoqlParserUtil
                                        .UNKNOWN)); // Field is unknown here because we don't know
        // which fields were filtered by READABLE check.
    }

    @Test
    public void testUnsafe_SingleType_multipleItems_onlyOneChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = new List<Account>();\n"
                        + "       Account a1 = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts1.add(a1);\n"
                        + "       List<Account> accounts2 = new List<Account>();\n"
                        + "       Account a2 = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts2.add(a2);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, accounts1);\n"
                        + "       insert sd.getRecords();\n"
                        + "       insert accounts2;\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(11, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testSafe_SingleType_multipleItems_bothChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = new List<Account>();\n"
                        + "       Account a1 = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts1.add(a1);\n"
                        + "       List<Account> accounts2 = new List<Account>();\n"
                        + "       Account a2 = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts2.add(a2);\n"
                        + "       SObjectAccessDecision sd1 = Security.stripInaccessible(AccessType.CREATABLE, accounts1);"
                        + "       SObjectAccessDecision sd2 = Security.stripInaccessible(AccessType.CREATABLE, accounts2);"
                        + "       insert sd1.getRecords();\n"
                        + "       insert sd2.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_accessTypeFromSymbol() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       AccessType accessType = AccessType.CREATABLE;\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(accessType, accounts);\n"
                        + "       insert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_accessCheckInAnotherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       List<Account> cleaned = cleanFields(accounts);\n"
                        + "       insert cleaned;\n"
                        + "   }\n"
                        + "   public List<Account> cleanFields(List<Account> accounts) {\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, accounts);\n"
                        + "		return sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_accessCheckAndDmlInDifferentMethods() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       List<Account> cleaned = cleanFields(accounts);\n"
                        + "       insertAcc(cleaned);\n"
                        + "   }\n"
                        + "   public void cleanFields(List<Account> accounts) {\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, accounts);\n"
                        + "		return sd.getRecords();\n"
                        + "   }\n"
                        + "   public void insertAcc(List<Account> accounts) {\n"
                        + "       insert accounts;\n"
                        + "   }"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
