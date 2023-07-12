package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.api.Test;

public class SharingPolicyMiscTests extends BaseUseWithSharingOnDatabaseOperationTest {

    @Test
    public void testBasicImplicitInheritanceFromParentWarning() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   OtherClass sub = new OtherClass();\n" +
                "   sub.doDbOp(a);\n" +
                "}\n" +
                "}\n",
            "public class OtherClass extends " + MY_CLASS + " {\n" +
                "   void doDbOp(Account a) {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expectWarning(3, SharingPolicyUtil.InheritanceType.PARENT, MY_CLASS));
    }

    @Test
    public void testEmptyMethod() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing abstract class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                "} \n" +
                "}\n",
        };

        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testNoMethodParams() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                "   " + SOQL_STATEMENTS[0] + ";\n" +
                "} \n" +
                "}\n",
        };

        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    /*
       The below test cases are the edge cases where more than one warning could be considered
       "correct" when walking the decision tree. These tests make sure those edge cases
       display the message we want them to.
    */

    /**
     * B neither declares its own sharing model nor inherits from A, it uses myClass' sharing model
     * which is "with sharing" so it warns of implicitly inheriting from MyClass. MyClass is where
     * the call happens, and B is where the DatabaseOperation happens. That's what we care about.
     */
    @Test
    public void testClassImplicitlyInheritsFromParentImplicitlyInheritsFromCallingClassParent() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing class ParentClass {\n" +
                "   public void foo(Account a) {\n" +
                "       A sub = new A();\n" +
                "       a.doDbOp(a);\n" +
                "   }\n" +
                "}\n",

            "public class " + MY_CLASS + " extends ParentClass {\n" +
                "   public void foo(Account a) {\n" +
                "       B sub = new B();\n" +
                "       sub.doDbOp(a);\n" +
                "   }\n" +
                "}\n",

            "public class A { }\n",

            "public class B extends A {\n" +
                "   public void doDbOp(Account a) {\n" +
                "       insert a;\n" +
                "   }\n" +
            "}\n",

            /*

             */

        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expectWarning(3, SharingPolicyUtil.InheritanceType.CALLING, "MyClass"));
    }

    /**
     * B implicitly inherits a sharing model from A, so it warns of implicitly inheriting from A.
     */
    @Test
    public void testParentExplicitlyInheritsFromCallingClassParent() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing class ParentClass {\n" +
                "   public void foo(Account a) {\n" +
                "       A sub = new A();\n" +
                "       a.doDbOp(a);\n" +
                "}\n" +
            "}\n",

            "public class " + MY_CLASS + " extends ParentClass {\n" +
                "   public void foo(Account a) {\n" +
                "       B sub = new B();\n" +
                "       sub.doDbOp(a);\n" +
                "   }\n" +
                "}\n",

            "public inherited sharing class A { }\n",

            "public class B extends A {\n" +
                "   public void doDbOp(Account a) {\n" +
                "       insert a;\n" +
                "   }\n" +
                "}\n",

        };
        // spotless:on

        assertViolations(
                RULE, sourceCode, expectWarning(3, SharingPolicyUtil.InheritanceType.PARENT, "A"));
    }

    @Test
    public void testParentImplicitlyInheritsFromCallingClassParent() {
        // confirmed this compiles
        // spotless:off
        String[] sourceCode = {
            "public with sharing class MyParentClass {\n" +
                "void foo(Account a) {\n" +
                "   A sub = new A();\n" +
                "   sub.doDbOp(a);\n" +
                "}\n" +
                "}\n",
            "public class " + MY_CLASS + " extends MyParentClass {\n" +
                "void foo(Account a) {\n" +
                "   B sub = new B();\n" +
                "   sub.doDbOp(a);\n" +
                "}\n" +
                "}\n",
            "public class A {\n" +
                "   public void doDbOp(Account a) {\n" +
                "   }\n" +
                "}\n",
            "public class B extends A {\n" +
                "   public void doDbOp(Account a) {\n" +
                "       insert a;\n" +
                "   }\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(
                RULE,
                sourceCode,
                expectWarning(3, SharingPolicyUtil.InheritanceType.CALLING, MY_CLASS));
    }
}
