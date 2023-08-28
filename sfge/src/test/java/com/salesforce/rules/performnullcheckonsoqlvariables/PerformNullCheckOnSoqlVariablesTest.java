package com.salesforce.rules.performnullcheckonsoqlvariables;

import com.salesforce.rules.PerformNullCheckOnSoqlVariables;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PerformNullCheckOnSoqlVariablesTest extends BasePathBasedRuleTest {

    private static final String MY_CLASS = "MyClass";

    protected static final PerformNullCheckOnSoqlVariables RULE =
            PerformNullCheckOnSoqlVariables.getInstance();

    protected ViolationWrapper.SoqlNullViolationBuilder expect(int line, String varname) {
        return new ViolationWrapper.SoqlNullViolationBuilder(line, varname);
    }

    @Test
    public void testBasicNullString() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
            "   void foo() {\n" +
            "       String nullString = null;\n" +
            "       Account a = [SELECT Name FROM Account WHERE Name =: nullString LIMIT 1];\n" +
            "   }\n" +
            "}\n";
        //  spotless:on

        assertViolations(RULE, sourceCode, expect(4, "nullString"));
    }

    /**
     * Since the entrypoint takes a parameter, this should be a violation due to indeterminate
     * value.
     */
    @Test
    public void testIndeterminateParameter() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
            "   void foo(String parameterName) {\n" +
            "       Account a = [SELECT Name FROM Account WHERE Name =: parameterName LIMIT 1];\n" +
            "   }\n" +
            "}\n";
        //  spotless:on

        assertViolations(RULE, sourceCode, expect(3, "parameterName"));
    }

    @ValueSource(
            strings = {
                "nullInstanceVariable",
                "this.nullInstanceVariable",
                "emptyString",
                "methodParameterVariable",
                "methodScopeAccount.Name",
                "accountWithNullName.Name",
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullCheckOk(String varName) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   String nullInstanceVariable = null;\n" +
                "   String emptyString = '';\n" +
                "   Account accountWithNullName = new Account(name = null);\n" +
                "   void foo(String methodParameterVariable) {\n" +
                "       Account methodScopeAccount = [SELECT Name FROM Account WHERE Name = 'Jane' LIMIT 1];\n" +
                "       if (" + varName + " != null) {\n" +
                "           Account a = [SELECT Name FROM Account WHERE Name =: " + varName + " LIMIT 1];\n" +
                "       }\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @ValueSource(
            strings = {
                "nullInstanceVariable",
                "this.nullInstanceVariable",
                "emptyString",
                "methodParameterVariable",
                "methodScopeAccount.Name",
                "accountWithNullName.Name",
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullCheckElseOk(String varName) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   String nullInstanceVariable = null;\n" +
                "   String emptyString = '';\n" +
                "   Account accountWithNullName = new Account(name = null);\n" +
                "   void foo(String methodParameterVariable) {\n" +
                "       Account methodScopeAccount = [SELECT Name FROM Account WHERE Name = 'Jane' LIMIT 1];\n" +
                "       if (" + varName + " == null) {\n" +
                "           return;\n" +
                "       } else {\n" +
                "           Account a = [SELECT Name FROM Account WHERE Name =: " + varName + " LIMIT 1];\n" +
                "       }\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @ValueSource(
            strings = {
                "nullInstanceVariable",
                "this.nullInstanceVariable",
                "emptyString",
                "methodParameterVariable",
                "methodScopeAccount.Name",
                "accountWithNullName.Name",
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testReturnIfNullOk(String varName) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   String nullInstanceVariable = null;\n" +
                "   String emptyString = '';\n" +
                "   Account accountWithNullName = new Account(name = null);\n" +
                "   void foo(String methodParameterVariable) {\n" +
                "       Account methodScopeAccount = [SELECT Name FROM Account WHERE Name = 'Jane' LIMIT 1];\n" +
                "       if (" + varName + " == null) {\n" +
                "           return;\n" +
                "       }\n" +
                "       Account a = [SELECT Name FROM Account WHERE Name =: " + varName + " LIMIT 1];\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @ValueSource(
            strings = {
                "nullInstanceVariable",
                "this.nullInstanceVariable",
                "methodParameterVariable",
                "methodScopeAccount.Name",
                "accountWithNullName.Name",
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNoNullCheck(String varName) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   String nullInstanceVariable = null;\n" +
                "   Account accountWithNullName = new Account(name = null);\n" +
                "   void foo(String methodParameterVariable) {\n" +
                "       Account methodScopeAccount = [SELECT Name FROM Account WHERE Name = 'Jane' LIMIT 1];\n" +
                "       Account a = [SELECT Name FROM Account WHERE Name =: " + varName + " LIMIT 1];\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertViolations(RULE, sourceCode, expect(6, varName.replace("this.", "")));
    }

    /**
     * These should all be violations. Though they are indeterminate, they are definitely null
     * within the if statement.
     */
    @ValueSource(
            strings = {
                "methodParameterVariable",
                "methodScopeAccount.Name",
                "classScopeAccount.Name"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testPositiveNullConstrainedIndeterminate(String varName) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   Account classScopeAccount = [SELECT Name FROM Account WHERE Name = '' LIMIT 1];\n" +
                "   void foo(String methodParameterVariable) {\n" +
                "       Account methodScopeAccount = [SELECT Name FROM Account WHERE Name = 'Jane' LIMIT 1];\n" +
                "       if (" + varName + " == null) {\n" +
                "           Account a = [SELECT Name FROM Account WHERE Name =: " + varName + " LIMIT 1];\n" +
                "       }\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertViolations(RULE, sourceCode, expect(6, varName));
    }

    /**
     * Even though this code runs a SOQL query only when emptyString is null, emptyString is never
     * actually null since it is explicitly defined as an empty string. Therefore, there are no
     * paths where the soql expression runs, and there are no violations.
     *
     * <p>Note: an empty string is not equal to null in Apex.
     */
    @Test
    public void testNullVariableInsideSoqlStatementThatNeverRuns() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   void foo() {\n" +
                "       String emptyString = '';\n" +
                "       if (emptyString == null) {\n" +
                "           Account a = [SELECT Name, BillingState FROM Account WHERE Name =: emptyString LIMIT 1];\n" +
                "       }\n" +
                "   }\n" +
                "}\n";
        //  spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    /**
     * Tests correctly resolving a scope variable when a scope and instance variable have the same
     * name. Note: there is no need to test referencing static methods via "this.varName" because
     * static methods cannot reference static properties via "this." in Apex.
     */
    @Test
    public void testScopeAndInstanceVarSameName_resolveToScopeVar() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
            "   String nullString = 'ACTUALLY THERE IS TEXT!';\n" +
            "   void foo() {\n" +
            "       String nullString = null;\n" +
            "       Account a = [SELECT Name FROM Account WHERE Name =: nullString LIMIT 1];\n" +
            "   }\n" +
            "}\n";
        // spotless:on

        assertViolations(RULE, sourceCode, expect(5, "nullString"));
    }

    @Test
    public void testScopeAndInstanceVarSameName_resolveToInstanceVar() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
            "   String nullString = 'ACTUALLY THERE IS TEXT!';\n" +
            "   void foo() {\n" +
            "       String nullString = null;\n" +
            "       Account a = [SELECT Name FROM Account WHERE Name =: this.nullString LIMIT 1];\n" +
            "   }\n" +
            "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testReassignVariable() {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
                "   void foo(String parameterName) {\n" +
                "       parameterName = null;\n" +
                "       Account a = [SELECT Name FROM Account WHERE Name =: parameterName LIMIT 1];\n" +
                "   }\n" +
                "}\n";
        // spotless:on

        assertViolations(RULE, sourceCode, expect(4, "parameterName"));
    }

    /**
     * Test null variables in the LIMIT and OFFSET clauses. // TODO: consider moving to {@link
     * com.salesforce.rules.ApexNullPointerExceptionRule}. Technically, these throw a Null Pointer
     * Exception in Apex.
     *
     * <p>NOTE: this will break when W-13876363 is fixed. This is acceptable and should not prevent
     * us from fixing that bug. Afterwards, this rule might need to be updated. Currently, all
     * VariableExpression children of a BindExpression (contained in a SoqlExpressionVertex) are
     * checked for null values, including those in the LIMIT/OFFSET clauses. This rule may need to
     * be updated to parse the SOQL statement and check only the variables contained in the WHERE
     * clause.
     */
    @ValueSource(strings = {"LIMIT", "OFFSET"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullVariableInLimitAndOffset(String command) {
        // spotless:off
        String sourceCode =
            "public class " + MY_CLASS + " {\n" +
            "   void foo() {\n" +
            "       Integer i;\n" +
            "       Account[] accs = [SELECT Name FROM Account " + command + " :i];\n" +
            "   }\n" +
            "}\n";
        // spotless:on

        assertViolations(RULE, sourceCode, expect(4, "i"));
    }

    /**
     * Both cases of the if statement lead to the use of a non-null-checked variable in the soql
     * expression. Though two paths lead to the same sink, only one violation should be thrown.
     */
    @Test
    public void testMultiplePathsSameSink_twoBadPaths() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public void doOperation(Integer i) {\n" +
                "   Account b = [SELECT Name FROM Account LIMIT :i];\n" +
                "}\n" +
                "public void foo(Integer i, Boolean maybe) {\n" +
                "   if (maybe) {\n" +
                "       doOperation(i);\n" +
                "   } else {\n" +
                "       doOperation(i);\n" +
                "   }\n" +
                "}\n" +
            "}";
        // spotless:on
        assertViolations(RULE, sourceCode, expect(3, "i"));
    }

    /**
     * Only one path leads to the use of a non-null-checked variable in the soql expression, even
     * though both do the SOQL query. We'd expect one violation.
     */
    @Test
    public void testMultiplePathsSameVertex_oneBadPath() {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "public void doOperation(Integer i) {\n" +
                "   Account b = [SELECT Name FROM Account LIMIT :i];\n" +
                "}\n" +
                "public void foo(Integer i, Boolean maybe) {\n" +
                "   if (i == null) {\n" +
                "       doOperation(i);\n" +
                "   } else {\n" +
                "       doOperation(i);\n" +
                "   }\n" +
                "}\n" +
            "}";
        // spotless:on
        assertViolations(RULE, sourceCode, expect(3, "i"));
    }
}
