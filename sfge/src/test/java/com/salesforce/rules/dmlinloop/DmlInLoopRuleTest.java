package com.salesforce.rules.dmlinloop;

import com.salesforce.rules.DmlInLoopRule;
import com.salesforce.rules.OccurrenceInfo;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DmlInLoopRuleTest extends BasePathBasedRuleTest {

    private static final String MY_CLASS = "MyClass";

    protected static final DmlInLoopRule RULE = DmlInLoopRule.getInstance();

    /**
     * function to get the expected violation from some code and an loop OccurrenceInfo
     *
     * @param sinkLine the line on which the sink vertex occurs
     * @param occurrenceInfo the information about the loop
     * @return a ViolationWrapper.DmlInLoopInfoBuilder
     */
    protected ViolationWrapper.DmlInLoopInfoBuilder expect(
        int sinkLine, OccurrenceInfo occurrenceInfo) {
        return ViolationWrapper.DmlInLoopInfoBuilder.get(sinkLine, occurrenceInfo);
    }
    /*
    1. implement method checking
    2. fix top 2 tests for all combinations of DML and loop types
    3. loop and loop calls method -> still throws violation (makes this a path-based rule)
     */

    /**
     * tests basic loops with a SOQL statement.
     * corresponds to SOQLStatementVertex.
     * SELECT is the only type of query we can make in [this syntax].
     */
    @CsvSource({
        // technically the myList could be in the method call or query
        "ForEachStatement, for (Integer i : myList)",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSoqlStatementInLoop(String loopLabel, String loopStructure) {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + loopStructure
                    + " {\n"
                        + "Account myAcct = [SELECT Id, Name, BillingCity FROM Account WHERE Id = :i LIMIT 1];\n" // TODO LIMIT for one object
                    + "}\n"
                + "}\n"
            + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(6, new OccurrenceInfo(loopLabel, MY_CLASS, 4)));
    }


    /**
     * tests SOQL statements not in a loop
     */
    @CsvSource({
        "ForEachStatement, for (Integer i : myList)",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSoqlStatementNotInLoop(String loopLabel, String loopStructure) {
        String[] sourceCode = {
            "public class MyClass {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + "Account myAcct = \n "
                    + "[SELECT Id, Name, BillingCity FROM Account WHERE Id = :i];\n"
                    + loopStructure
                    + " {}\n"
                + "}\n"
            + "}\n"
        };

        assertNoViolation(RULE, sourceCode);
    }

    /**
     * tests basic loops with DML statements.
     * corresponds with DmlStatementVertex (DmlDeleteStatementVertex, DmlMergeStatementVertex, etc.)
     */
    @CsvSource({
        "ForEachStatement, for (Integer i : myList), delete",
        "ForEachStatement, for (Integer i : myList), insert",
        "ForEachStatement, for (Integer i : myList), merge newAcct",
        "ForEachStatement, for (Integer i : myList), undelete",
        "ForEachStatement, for (Integer i : myList), update",
        "ForEachStatement, for (Integer i : myList), upsert",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), delete",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), insert",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), merge newAcct",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), undelete",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), update",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), upsert",
        "WhileLoopStatement, while(true), delete",
        "WhileLoopStatement, while(true), insert",
        "WhileLoopStatement, while(true), merge newAcct",
        "WhileLoopStatement, while(true), undelete",
        "WhileLoopStatement, while(true), update",
        "WhileLoopStatement, while(true), upsert"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlStatementInLoop(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
        "public class MyClass {\n"
            + "void foo() {\n"
                + "List<Integer> myList = new Integer[] {3,5};\n"
                + "Account newAcct = new Account(name = 'Acme');\n"
                + "Account existing = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3];\n"
                + loopStructure
                    + " {\n"
                    + "try {\n"
                        + dmlStatement + " existing;\n"
                    + "} catch (DmlException e) { }\n"
                + "}\n"
            + "}\n"
        + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(8, new OccurrenceInfo(loopLabel, MY_CLASS, 6)));
    }


    @CsvSource({
        "ForEachStatement, for (Integer i : myList), delete",
        "ForEachStatement, for (Integer i : myList), insert",
        "ForEachStatement, for (Integer i : myList), merge newAcct",
        "ForEachStatement, for (Integer i : myList), undelete",
        "ForEachStatement, for (Integer i : myList), update",
        "ForEachStatement, for (Integer i : myList), upsert",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), delete",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), insert", // TODO replace i++ with
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), merge newAcct",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), undelete",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), update",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), upsert",
        "WhileLoopStatement, while(true), delete",
        "WhileLoopStatement, while(true), insert",
        "WhileLoopStatement, while(true), merge newAcct",
        "WhileLoopStatement, while(true), undelete",
        "WhileLoopStatement, while(true), update",
        "WhileLoopStatement, while(true), upsert"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlStatementNotInLoop(
        String loopLabel, String loopStructure, String dmlStatement) {
        String[] sourceCode = {
            "public class MyClass {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + "Account newAcct = new Account(name = 'Acme');\n"
                    + "Account existing = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3];\n"
                    + "try {\n"
                    + dmlStatement
                    + " existing;\n"
                    + "} catch (DmlException e) { }\n"
                    + loopStructure
                    + " { }\n"
                + "}\n"
            + "}\n"
        };

        assertNoViolation(RULE, sourceCode);
    }

    // TODO this test is failing, double check to see the cause. seems to be a problem with LoopDetectionVisitor maybe
    /**
     * The second part (post-colon) of a for-each loop declaration is only run once,
     * so this should be OK.
     */
    @Test
    public void testLoopInForEachDeclaration() {

        //spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "for (Account acc : [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3]) {\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        //spotless:off

        assertNoViolation(RULE, sourceCode);
    }

    /**
     * The termination clause of a for loop repeats, so this is a violation
     */
    @Test
    public void testForLoopTerminationStatement() {

        //spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "for (Integer i = 0; i < [SELECT age FROM accounts WHERE Id = 3 LIMIT 1]; i++) {\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        //spotless:on

        assertViolations(
            RULE,
            sourceCode,
            expect(
                3,
                new OccurrenceInfo(
                    "ForLoopStatement",
                    MY_CLASS,
                    3
                )
            )
        );
    }
    @CsvSource({
        "ForEachStatement, for (Integer i : myList), 'Account acc = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3 LIMIT 1];'",
        "ForEachStatement, for (Integer i : myList), delete one;",
        "ForEachStatement, for (Integer i : myList), insert one;",
        "ForEachStatement, for (Integer i : myList), merge one one;",
        "ForEachStatement, for (Integer i : myList), undelete one;",
        "ForEachStatement, for (Integer i : myList), update one;",
        "ForEachStatement, for (Integer i : myList), upsert one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), 'Account acc = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3 LIMIT 1];'",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), delete one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), insert one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), merge one one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), undelete one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), update one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), upsert one;",
        "WhileLoopStatement, while(true), 'Account acc = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3 LIMIT 1];'",
        "WhileLoopStatement, while(true), delete one;",
        "WhileLoopStatement, while(true), insert one;",
        "WhileLoopStatement, while(true), merge one one;",
        "WhileLoopStatement, while(true), undelete one;",
        "WhileLoopStatement, while(true), update one;",
        "WhileLoopStatement, while(true), upsert one;",
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testIndirectMethodCallSoql(String loopLabel, String loopStructure, String dmlStatement) {
        //spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "Account one = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 2 LIMIT 1]; \n " +
                    loopStructure + " {\n" +
                        "withDml(one);\n" +
                    "}\n" +
                "}\n" +
                "void withDml(Account one) {\n" +
                    dmlStatement + "\n" +
                "}\n" +
            "}\n"
        };
        //spotless:on

        assertViolations(
            RULE,
            sourceCode,
            expect(
                9,
                new OccurrenceInfo(
                    loopLabel,
                    MY_CLASS,
                    4
                )
            )
        );

    }
}
