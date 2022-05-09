package com.salesforce.rules.fls.apex;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ListInsertFlsViolationRuleTest extends BaseFlsTest {
    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of(ApexFlsViolationRule.getInstance(), "DML", "insert %s;\n"),
                Arguments.of(
                        ApexFlsViolationRule.getInstance(), "Database", "Database.insert(%s);\n"),
                Arguments.of(
                        ApexFlsViolationRule.getInstance(),
                        "Database with boolean",
                        "Database.insert(%s, false);\n"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_objectTypeDeclared_SingleItem(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_objectTypeDeclared_SingleItem(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_objectTypeDeclared_SingleItem_objProperty(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(7, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_objectTypeDeclared_SingleItem_objProperty(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_objectTypeDeclared_MultipleItems(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       Account acc2 = new Account(Name = 'foo2');\n"
                        + "       accounts.add(acc1);\n"
                        + "       accounts.add(acc2);\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(8, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_objectTypeDeclared_MultipleItems(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       Account acc2 = new Account(Name = 'foo2');\n"
                        + "       accounts.add(acc1);\n"
                        + "       accounts.add(acc2);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_objectTypeDeclared_MultipleItemsFromQuery(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        List<Account> accounts = [SELECT Name, Phone from Account];\n"
                        + "        List<Account> newAccounts = new List<Account>();\n"
                        + "        for (Account acc: accounts) {\n"
                        + "            Account a = new Account(Name = acc.name, Phone = acc.phone);\n"
                        + "            newAccounts.add(a);\n"
                        + "        }\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(10, FlsValidationType.INSERT, "Account")
                        .withField("Name")
                        .withField("Phone"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_objectTypeDeclared_MultipleItemsFromQuery(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        List<Account> accounts = [SELECT Name, Phone from Account];\n"
                        + "        List<Account> newAccounts = new List<Account>();\n"
                        + "        for (Account acc: accounts) {\n"
                        + "            Account a = new Account(Name = acc.name, Phone = acc.phone);\n"
                        + "            newAccounts.add(a);\n"
                        + "        }\n"
                        + "        if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + "           if (Schema.SObjectType.Account.fields.Phone.isCreateable()) {\n"
                        + String.format(dmlFormat, "newAccounts")
                        + "           }\n"
                        + "        }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_noObjectType_SingleItem(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       objects.add(acc1);\n"
                        + String.format(dmlFormat, "objects")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_noObjectType_SingleItem(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       objects.add(acc1);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "objects")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_noObjectType_MultipleItems(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'Acme Inc.');\n"
                        + "       Contact con1 = new Contact(FirstName = 'foo');\n"
                        + "       objects.add(acc1);\n"
                        + "       objects.add(con1);\n"
                        + String.format(dmlFormat, "objects")
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsValidationType.INSERT, "Account").withField("Name"),
                expect(8, FlsValidationType.INSERT, "Contact").withField("FirstName"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_noObjectType_MultipleItems(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'Acme Inc.');\n"
                        + "       Contact con1 = new Contact(FirstName = 'foo');\n"
                        + "       objects.add(acc1);\n"
                        + "       objects.add(con1);\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + "           if (Schema.SObjectType.Contact.fields.FirstName.isCreateable()) {\n"
                        + String.format(dmlFormat, "objects")
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_emptyList(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_emptyList(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(4, FlsValidationType.INSERT, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafe_nullList(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = null;\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                        + String.format(dmlFormat, "accounts")
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_nullList(
            AbstractPathBasedRule rule, String testCategory, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = null;\n"
                        + String.format(dmlFormat, "accounts")
                        + "   }\n"
                        + "}\n";

        assertViolations(rule, sourceCode, expect(4, FlsValidationType.INSERT, "Account"));
    }
}
