package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateFlsScenariosTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testUpdateFromQueryWithId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account a = [SELECT Id, Name FROM Account];\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "            a.Name = 'Acme Inc.';\n"
                        + "            update a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUpdateFromQueryWithIdWithViolation() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account a = [SELECT Id, Name FROM Account];\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        update a;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @Test
    public void testUpdateFromQueryWithAdditionalKeyValueField() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account a = [SELECT Id, Name FROM Account];\n"
                        + "        a.Description = 'Some description';\n"
                        + "        update a;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(6, FlsValidationType.UPDATE, "Account")
                        .withField("Name")
                        .withField("Description"));
    }

    @Test
    public void testUpdateFromObjectWithId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Id = '001abc000000001', Name = 'ABC');\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "            a.Name = 'Acme Inc.';\n"
                        + "            update a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUpdateFromObjectWithoutId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "            a.Name = 'Acme Inc.';\n"
                        + "            update a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUpdateFromObjectWithOnlyId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Id = '001abc000000001');\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "            a.Name = 'New';\n"
                        + "            update a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUpdateFromObjectCheckOnlyOneField() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Id = '001abc000000001');\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "            a.Name = 'Acme Inc.';\n"
                        + "            a.Description = 'Some description';\n"
                        + "            update a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(7, FlsValidationType.UPDATE, "Account").withField("Description"));
    }
}
