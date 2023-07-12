package com.salesforce.rules.usewithsharingondatabaseoperation;

import org.junit.jupiter.api.Test;

/**
 * tests to make sure the rule can resolve a MethodCallExpression vertex to a MethodVertex (and its
 * UserClass vertex) with different types of syntax.
 */
public class SharingPolicyResolveMethodVertexTest
        extends BaseUseWithSharingOnDatabaseOperationTest {

    @Test
    public void testPaths() {
        // spotless:off
        String[] sourceCode = {
            "public with sharing class One {\n" +
                "void operation(Account a) {\n" +
                "}\n" +
            "}\n",
            "public class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   One one = new One();\n" +
                "   one.operation(a);\n" +
                "   insert a;\n" +
                "}\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(RULE, sourceCode, expect(5));
    }

    @Test
    public void testBasicCall() {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   B b = new B();\n" +
                "   b.doit(a);\n" +
                "}\n" +
                "class B {\n" +
                "   void doit(Account a) {\n" +
                "       insert a;" +
                "   }\n" +
                "}\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(RULE, sourceCode, expect(8));
    }

    @Test
    public void testThisDotMethod() {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   this.thing(a);\n" +
                "}\n" +
                "void thing(Account a) {\n" +
                "   insert a;\n" +
                "}\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(RULE, sourceCode, expect(6));
    }

    @Test
    public void testMethodInSameClass() {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   thing(a);\n" +
                "}\n" +
                "void thing(Account a) {\n" +
                "   insert a;\n" +
                "}\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(RULE, sourceCode, expect(6));
    }

    @Test
    public void testChainedResolution() {
        // spotless:off
        String[] sourceCode = {
            "public class One {\n" +
                "void operation(Account a) {\n" +
                "   insert a;\n" +
                "}\n" +
            "}\n",
            "public class " + MY_CLASS + " {\n" +
                "void foo(Account a) {\n" +
                "   new One().operation(a);\n" +
                "}\n" +
            "}\n",
        };
        // spotless:on
        assertViolations(RULE, sourceCode, expect(3));
    }
}
