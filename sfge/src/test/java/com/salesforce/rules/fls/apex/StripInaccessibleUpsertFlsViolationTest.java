package com.salesforce.rules.fls.apex;

import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StripInaccessibleUpsertFlsViolationTest extends BaseFlsTest {
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
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       upsert accounts;\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(7, FlsValidationType.INSERT, "Account").withField("Name").withField("phone"),
                expect(7, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("phone"));
    }

    @Test
    public void testSafe_SingleItem() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPSERTABLE, accounts);\n"
                        + "       upsert sd.getRecords();\n"
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
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       upsert accounts;\n"
                        + "       Security.stripInaccessible(AccessType.UPSERTABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(7, FlsValidationType.INSERT, "Account").withField("Name").withField("phone"),
                expect(7, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("phone"));
    }

    @Test
    public void testUnsafe_SingleType_UnmatchedAccessType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "       upsert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsValidationType.INSERT, "Account")
                        .withField(
                                SoqlParserUtil
                                        .UNKNOWN), // Field is unknown here because we don't know
                // which fields were filtered by READABLE check.
                expect(8, FlsValidationType.UPDATE, "Account").withField(SoqlParserUtil.UNKNOWN));
    }

    @Test
    public void testSafe_MultipleTypes_UnmatchedListType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> sobjects = new List<SObject>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       sobjects.add(a);\n"
                        + "       Contact c = new Contact(FirstName = 'foo');\n"
                        + "       sobjects.add(c);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPSERTABLE, sobjects);"
                        + "       upsert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_MultipleTypes_UnmatchedListType_ForLoop() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> sobjects = new List<SObject>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       sobjects.add(a);\n"
                        + "       Contact c = new Contact(FirstName = 'foo');\n"
                        + "       sobjects.add(c);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPSERTABLE, sobjects);"
                        + "       List<SObject> cleaned = sd.getRecords();\n"
                        + "		for (SObject item: cleaned) {\n"
                        + "			upsert item;\n"
                        + "		}\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_upsertFromQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		/* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "       List<Account> accounts = [SELECT Name FROM Account];\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPSERTABLE, accounts);"
                        + "       upsert sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
