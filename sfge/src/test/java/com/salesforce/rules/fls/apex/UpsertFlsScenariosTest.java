package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpsertFlsScenariosTest extends BaseFlsTest {
    private ApexFlsViolationRule rule;

    @BeforeEach
    public void setup() {
        super.setup();
        this.rule = ApexFlsViolationRule.getInstance();
    }

    @Test
    public void testUnsafe_simple() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        upsert a;\n"
                        + "    }\n"
                        + "}\n";

        // Upsert validation requires an Insert validation and an Update validation
        assertViolations(
                rule,
                sourceCode,
                expect(5, FlsValidationType.INSERT, "Account").withField("Name"),
                expect(5, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @Test
    public void testSafe_simple_order1() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Name.isCreateable()) {\n"
                        + "               upsert a;\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testSafe_simple_order2() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isCreateable()) {\n"
                        + "           if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "               upsert a;\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafe_missingCreatableCheck() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isUpdateable()) {\n"
                        + "           upsert a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        // Only a single violation is expected since Update check exists
        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testUnsafe_missingUpdateableCheck() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account a = new Account(Name = 'ABC');\n"
                        + "        a.Name = 'Acme Inc.';\n"
                        + "        if (Schema.sObjectType.Account.fields.Name.isCreateable()) {\n"
                        + "           upsert a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        // Only one violation is expected since Create check exists
        assertViolations(
                rule, sourceCode, expect(6, FlsValidationType.UPDATE, "Account").withField("Name"));
    }

    @Test
    public void testUpdateFromObjectWithId() {
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
}
