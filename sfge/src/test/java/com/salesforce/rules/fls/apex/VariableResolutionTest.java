package com.salesforce.rules.fls.apex;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.Test;

public class VariableResolutionTest extends BaseFlsTest {

    private final AbstractPathBasedRule rule = ApexFlsViolationRule.getInstance();

    /**
     * It's a violation to create an account with a Phone field after checking if the Name field is
     * createable. This tests that this.fieldName correctly resolves to the instance variable value,
     * "Name".
     */
    @Test
    public void testUnsafeInsertionBecauseOfInstanceVar() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public String fieldName = 'Name';\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(this.fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Phone = '867-5309');\n" +
                "   if (safe) {\n" +
                "       insert a;\n" +
                "   }" +
                "}\n" +
            "}\n";
        // spotless:on

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsConstants.FlsValidationType.INSERT, "Account").withField("Phone"));
    }

    /**
     * It's not a violation to create an account with a Name field after checking if the Name field
     * is createable. This tests that the reference to this.fieldName correctly resolves to the
     * instance variable value, "Name".
     */
    @Test
    public void testSafeInsertionBecauseOfInstanceVar() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public String fieldName = 'Name';\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(this.fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Name = 'Jenny');\n" +
                "   if (safe){\n" +
                "       insert a;\n" +
                "   }" +
                "}\n" +
            "}\n";
        // spotless:on

        assertNoViolation(rule, sourceCode);
    }

    /**
     * It's not a violation to create an account with a Phone field after checking if the Phone
     * field is createable. This tests that the reference to fieldName correctly resolves to the
     * scoped variable value, "Phone".
     */
    @Test
    public void testSafeInsertionBecauseOfScopeVar() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public String fieldName = 'Name';\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Phone = '867-5309');\n" +
                "   if (safe) {\n" +
                "       insert a;\n" +
                "   }" +
                "}\n" +
            "}\n";
        // spotless:on

        assertNoViolation(rule, sourceCode);
    }

    /**
     * It's a violation to create an account with a Name field after checking the Phone field is
     * createable. This tests that the reference to fieldName correctly resolves to the scoped
     * variable value, "Phone".
     */
    @Test
    public void testUnsafeInsertionBecauseOfScopeVar() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public String fieldName = 'Name';\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Name = 'Jenny');\n" +
                "   if (safe){\n" +
                "       insert a;\n" +
                "   }" +
                "}\n" +
            "}\n";
        // spotless:on

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"));
    }

    /**
     * It's not a violation to create an account with a Name field after checking if the field Name
     * is createable. This tests that the reference to this.fieldName correctly resolves to the
     * instance variable value, "Name".
     */
    @Test
    public void testInheritance_this() {
        // spotless:off
        String[] sourceCode = {
          "public class GrandParent {\n" +
              "public String fieldName = 'Phone';\n" +
          "}",
          "public class Parent extends GrandParent {\n" +
              "public String fieldName = 'Phone';\n" +
          "}",
          "public class MyClass extends Parent {\n" +
              "public String fieldName = 'Name';\n" +
              "void foo() {\n" +
              "   String fieldName = 'Phone';\n" +
              "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
              "                   .get(this.fieldName).getDescribe().isCreateable();\n" +
              "   Account a = new Account(Name = 'Jenny');\n" +
              "   if (safe) {\n" +
              "       insert a;\n" +
              "   }\n" +
              "}\n" +
          "}",
        };
        // spotless:on

        assertNoViolation(rule, sourceCode);
    }

    /**
     * It's not a violation to create an account with a Name field after checking if the field Name
     * is createable. This tests that the reference to this.fieldName correctly resolves to the
     * instance variable value, "Name" (which comes from the parent).
     */
    @Test
    public void testInheritance_thisFromParent() {
        // spotless:off
        String[] sourceCode = {
            "public class GrandParent {\n" +
                "public String fieldName = 'Phone';\n" +
            "}",
            "public class Parent extends GrandParent {\n" +
                "public String fieldName = 'Name';\n" +
            "}",
            "public class MyClass extends Parent {\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(this.fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Name = 'Jenny');\n" +
                "   if (safe) {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n" +
            "}",
        };
        // spotless:on

        assertNoViolation(rule, sourceCode);
    }

    /**
     * It's a violation to create an account with a Name field after checking if the field Phone is
     * createable. This tests that the reference to super.fieldName correctly resolves to the
     * instance variable's value (which is from a parent class).
     */
    @Test
    public void testInheritance_super() {
        // spotless:off
        String[] sourceCode = {
            "public class GrandParent {\n" +
                "public String fieldName = 'Phone';\n" +
                "}",
            "public class Parent extends GrandParent {\n" +
                "public String fieldName = 'Phone';\n" +
                "}",
            "public class MyClass extends Parent {\n" +
                "public String fieldName = 'Name';\n" +
                "void foo() {\n" +
                "   String fieldName = 'Phone';\n" +
                "   boolean safe = Account.SObjectType.getDescribe().fields.getMap()" +
                "                   .get(super.fieldName).getDescribe().isCreateable();\n" +
                "   Account a = new Account(Name = 'Jenny');\n" +
                "   if (safe) {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n" +
            "}",
        };
        // spotless:on

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"));
    }
}
