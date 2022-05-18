package com.salesforce.rules.fls.apex;

import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StripInaccessibleUpdateFlsViolationTest extends BaseFlsTest {
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
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       update accounts;\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
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
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPDATABLE, accounts);\n"
                        + "		List<Account> sanitizedAcc = sd.getRecords();\n"
                        + "       update sanitizedAcc;\n"
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
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       update accounts;\n"
                        + "       Security.stripInaccessible(AccessType.UPDATABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
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
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "       update sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsValidationType.UPDATE, "Account")
                        .withField(
                                SoqlParserUtil
                                        .UNKNOWN)); // Field is unknown here because we don't know
        // which fields were filtered by READABLE check.
    }

    @Test
    public void testUnsafe_MultipleTypes_UnmatchedListType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       a.phone = '123-456-7890';\n"
                        + "       accounts.add(a);\n"
                        + "       List<Contact> contacts = new List<Contact>();\n"
                        + "       Contact c = new Contact(Id = '004blahblah', FirstName = 'foo');\n"
                        + "       contacts.add(c);\n"
                        + "       Security.stripInaccessible(AccessType.UPDATABLE, contacts);\n"
                        + "       update accounts;\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(11, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("phone"));
    }

    @Test
    public void testSafe_MultipleTypes_UnmatchedListType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> sobjects = new List<SObject>();\n"
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       sobjects.add(a);\n"
                        + "       Contact c = new Contact(Id = '004blahblah', FirstName = 'foo');\n"
                        + "       sobjects.add(c);\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.Updatable, sobjects);"
                        + "       update sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_updateFromQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "	 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "       List<Account> accounts = [SELECT Id, Name FROM Account];\n"
                        + "       SObjectAccessDecision sd = Security.stripInaccessible(AccessType.UPDATABLE, accounts);"
                        + "       update sd.getRecords();\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }
}
