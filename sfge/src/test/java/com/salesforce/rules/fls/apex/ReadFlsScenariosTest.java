package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ReadFlsScenariosTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testUnsafeWithoutAssignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        [SELECT Status__c from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("Status__c"));
    }

    @Test
    public void testUnsafeWithAssignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Status__c from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("Status__c"));
    }

    @Test
    public void testUnsafeWithMultipleFields() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Id, Name,Status__c from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("Status__c")
                        .withField("Name"));
    }

    @Test
    public void testSafeWithId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Id from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeWithWhereClause() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Id from Contact where Name = 'Acme Inc.'];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("Name"));
    }

    @Test
    public void testUnsafeWithWhereClause_whereClauseFieldNotFlsChecked() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.SObjectType.Contact.Fields.LastName.isAccessible()) {\n"
                        + "           Contact c = [SELECT LastName from Contact where FirstName = 'Acme Inc.'];\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testSafeWithWhereClause() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.SObjectType.Contact.Fields.LastName.isAccessible()) {\n"
                        + "           if (Schema.SObjectType.Contact.Fields.FirstName.isAccessible()) {\n"
                        + "               Contact c = [SELECT LastName from Contact where FirstName = 'Acme Inc.'];\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeWithGroupBy() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT LastName from Contact GROUP BY FirstName];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("LastName")
                        .withField("FirstName"));
    }

    @Test
    public void testSafeWithGroupBy() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        if (Schema.SObjectType.Contact.Fields.LastName.isAccessible()) {\n"
                        + "           if (Schema.SObjectType.Contact.Fields.FirstName.isAccessible()) {\n"
                        + "               Contact c = [SELECT LastName from Contact GROUP BY FirstName];\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafeWithBothChecks() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       if (Schema.SObjectType.Contact.Fields.FirstName.isAccessible()) {\n"
                        + "           Contact c = [SELECT FirstName from Contact GROUP BY FirstName];\n"
                        + "           Security.stripInaccessible(AccessType.READABLE, c);\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(4, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("FirstName"));
    }

    @Test
    public void testUnsafeWithLateSchemaCheck() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       Contact c = [SELECT FirstName from Contact GROUP BY FirstName];\n"
                        + "       if (Schema.SObjectType.Contact.Fields.FirstName.isAccessible()) {\n"
                        + "           return;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    // simple positive: query contains "with" phrases
    @CsvSource({"SECURITY_ENFORCED", "USER_MODE"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeWithModeClause(String mode) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + String.format(
                                "        Contact c = [SELECT Status__c from Contact with %s];\n",
                                mode)
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    // simple negative: Query contains "with system_mode", which is de-facto unsafe.
    @Test
    public void testUnsafeWithSystemModeClause() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT Status__c from Contact with SYSTEM_MODE];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withField("Status__c"));
    }

    @Test
    public void testUnsafeWithAllFields() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Contact c = [SELECT FIELDS(all) from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact").withAllFields());
    }

    @Test
    public void testSafeWithAllFields() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        List<Contact> contacts = [SELECT FIELDS(all) from Contact];\n"
                        + "        Security.stripInaccessible(AccessType.READABLE, contacts);\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Contact")
                        .withAllFields());
    }

    @Test
    public void testSafeWithCount() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Integer count = [SELECT COUNT() from Contact];\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeDatabaseQueryCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       List<Contact> contacts = Database.query(query);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testUnsafeDbQueryAsReturn() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Contact> contacts = getData();\n"
                        + "   }\n"
                        + "   public List<Contact> getData() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       return Database.query(query);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(7, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testSafeDbQueryAsReturn_schema() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       if (Schema.sObjectType.Contact.fields.FirstName.isAccessible()){\n"
                        + "           List<Contact> contacts = getData();\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public List<Contact> getData() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       return Database.query(query);\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafeDbQueryAsReturn_stripInacc() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Contact> contacts = getData();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, contacts);\n"
                        + "   }\n"
                        + "   public List<Contact> getData() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       return Database.query(query);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expectStripInaccWarning(8, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("FirstName"));
    }

    @Test
    public void testSafeSoqlAsReturn_stripInacc() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Contact> contacts = getData();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, contacts);\n"
                        + "   }\n"
                        + "   public List<Contact> getData() {\n"
                        + "       return [SELECT FirstName from Contact];\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expectStripInaccWarning(7, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("FirstName"));
    }

    @Test
    public void testUnsafeDbQueryAsForEach() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       for (Contact c : Database.query(query)) {\n"
                        + "           //do something\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testSafeDbQueryAsForEach() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String query = 'SELECT FirstName from Contact';\n"
                        + "       if (Schema.sObjectType.Contact.fields.FirstName.isAccessible()) {\n"
                        + "           for (Contact c : Database.query(query)) {\n"
                        + "               //do something\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeDbQueryAsMethodParam() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String queryStr = 'SELECT FirstName from Contact';\n"
                        + "       getData(Database.query(queryStr));\n"
                        + "   }\n"
                        + "   public void getData(List<Contact> contacts) {\n"
                        + "       //do something\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testSafeDbQueryAsMethodParam() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String queryStr = 'SELECT FirstName from Contact';\n"
                        + "       if (Schema.sObjectType.Contact.fields.FirstName.isAccessible()) {\n"
                        + "           getData(Database.query(queryStr));\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void getData(List<Contact> contacts) {\n"
                        + "       //do something\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeDbQueryAsNewMapInit() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String queryStr = 'SELECT FirstName from Contact';\n"
                        + "       Map<Id, SObject> resultsById = new Map<Id, SObject>(Database.query(queryStr));\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact").withField("FirstName"));
    }

    @Test
    public void testSafeDbQueryAsNewMapInit() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String queryStr = 'SELECT FirstName from Contact';\n"
                        + "       if (Schema.sObjectType.Contact.fields.FirstName.isAccessible()) {\n"
                        + "           Map<Id, SObject> resultsById = new Map<Id, SObject>(Database.query(queryStr));\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    /** NPSP test case from BGE_DataImportBatchEntry_CTRL#getOpportunitiesWithOppPayments */
    @CsvSource({"SECURITY_ENFORCED", "USER_MODE"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeWithModeClauseInOuterQuery(String mode) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        String queryStr = 'SELECT Id, ' +\n"
                        + "                'Name, ' +\n"
                        + "                'StageName, ' +\n"
                        + "                'Amount, ' +\n"
                        + "                    '(SELECT Id, ' +\n"
                        + "                    'Name, ' +\n"
                        + "                    'npe01__Scheduled_Date__c, ' +\n"
                        + "                    'npe01__Opportunity__r.Name, ' +\n"
                        + "                    'npe01__Opportunity__c, ' +\n"
                        + "                    'npe01__Payment_Amount__c,' +\n"
                        + "                    'npe01__Paid__c, ' +\n"
                        + "                    'npe01__Written_Off__c ' +\n"
                        + "                    'FROM npe01__OppPayment__r ' +\n"
                        + "                    'WHERE npe01__Written_Off__c = false) ' +\n"
                        + "                'FROM Opportunity ' +\n"
                        + "                'WHERE AccountId = :donorId ' +\n"
                        + "                'AND IsClosed = false ' +\n"
                        + String.format("                'WITH %s';\n", mode)
                        + "			Database.query(queryStr);"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testComplexWhereClause() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        [SELECT FirstName from Contact WHERE Id in (SELECT contact_id__c from Account where Name = 'Acme Inc.') and LastName = 'blah'];\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Contact")
                        .withField("FirstName")
                        .withField("LastName"),
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withField("contact_id__c")
                        .withField("Name"));
    }

    @Test
    public void testWithEscapeSingleQuotes() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       String queryStr = 'SELECT Name FROM Account';\n"
                        // TODO: this invocation is incorrect. The correct way would be:
                        //  String.escapeSingleQuotes(queryStr);
                        // Created a new work item to track this issue.
                        + "       Database.query(queryStr.escapeSingleQuotes());\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }
}
