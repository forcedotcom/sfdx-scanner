package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ListUpdateFlsViolationRuleTest extends BaseFlsTest {
    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of("DML", ApexFlsViolationRule.getInstance(), "update %s;\n"),
                Arguments.of(
                        "Database", ApexFlsViolationRule.getInstance(), "Database.update(%s);\n"),
                Arguments.of(
                        "Database with boolean",
                        ApexFlsViolationRule.getInstance(),
                        "Database.update(%s, false);\n"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_objectTypeDeclared_SingleItem(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_objectTypeDeclared_SingleItem(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Id = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_objectTypeDeclared_SingleItem_objProperty(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.Id = '001blahblah';\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(8, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_objectTypeDeclared_SingleItem_objProperty(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.Id = '001blahblah';\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_objectTypeDeclared_MultipleItems(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account acc1 = new Account(ID = '001blahblah1', Name = 'foo1');\n"
                        + "       Account acc2 = new Account(id = '001blahblah2', Name = 'foo2');\n"
                        + "       accounts.add(acc1);\n"
                        + "       accounts.add(acc2);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(8, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_objectTypeDeclared_MultipleItems(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account acc1 = new Account(ID = '001blahblah1', Name = 'foo1');\n"
                        + "       Account acc2 = new Account(id = '001blahblah2', Name = 'foo2');\n"
                        + "       accounts.add(acc1);\n"
                        + "       accounts.add(acc2);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_objectTypeDeclared_itemsFromQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		/* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "       List<Account> accounts = [SELECT Id, Name, Phone from Account];\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(5, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_objectTypeDeclared_missingItemsFromQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        List<Account> accounts = [SELECT Id, Name, Phone from Account];\n"
                        + "        if (Schema.SObjectType.Account.fields.Phone.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "        }\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_objectTypeDeclared_itemsFromQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        List<Account> accounts = [SELECT Id, Name from Account];\n"
                        + "        for (Account acc: accounts) {\n"
                        + "            acc.phone = '123-456-7890';\n"
                        + "        }\n"
                        + "        if (Schema.SObjectType.Account.fields.Phone.isUpdateable()) {\n"
                        + "           if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "           }\n"
                        + "        }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_noObjectType_SingleItem(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Id = '001blahblah', Name = 'foo1');\n"
                        + "       objects.add(acc1);\n"
                        + String.format(dmlFormat, "objects")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_noObjectType_SingleItem(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Id = '001blahblah', Name = 'foo1');\n"
                        + "       objects.add(acc1);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "objects")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_noObjectType_MultipleItems(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(ID = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       Contact con1 = new Contact(Id = '004abc000000', FirstName = 'foo');\n"
                        + "       objects.add(acc1);\n"
                        + "       objects.add(con1);\n"
                        + String.format(dmlFormat, "objects")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsValidationType.UPDATE, "Account").withField("Name"),
                expect(8, FlsValidationType.UPDATE, "Contact").withField("FirstName"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_noObjectType_MultipleItems(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(ID = '001blahblah', Name = 'Acme Inc.');\n"
                        + "       Contact con1 = new Contact(Id = '004abc000000', FirstName = 'foo');\n"
                        + "       objects.add(acc1);\n"
                        + "       objects.add(con1);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "           if (Schema.SObjectType.Contact.fields.FirstName.isUpdateable()) {\n"
                        + String.format(dmlFormat, "objects")
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_emptyList(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_emptyList(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(4, FlsValidationType.UPDATE, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_nullList(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = null;\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_nullList(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = null;\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(4, FlsValidationType.UPDATE, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    @Disabled // Issue with picking fields from a modified SOQL list
    public void testUnsafe_objectTypeDeclared_accessFieldsOutsideQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        List<Account> accounts = [SELECT Id, Name from Account];\n"
                        + "        for (Account acc: accounts) {\n"
                        + "            acc.phone = '123-456-7890';\n"
                        + "        }\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        // FIXME: phone field is ignored by the flow
        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("phone"));
    }
}
