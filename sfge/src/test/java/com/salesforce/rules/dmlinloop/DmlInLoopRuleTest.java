package com.salesforce.rules.dmlinloop;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.rules.DmlInLoopRule;
import com.salesforce.rules.OccurrenceInfo;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import org.junit.jupiter.api.Disabled;
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

    /**
     * tests basic loops with a SOQL statement. corresponds to SOQLStatementVertex. SELECT is the
     * only type of query we can make in [this syntax].
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
                    + loopStructure + " {\n"
                        + "Account myAcct = [SELECT Id, Name, BillingCity FROM Account WHERE Id = :i LIMIT 1];\n"
                    + "}\n"
                + "}\n"
            + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(5, new OccurrenceInfo(loopLabel, MY_CLASS, 4)));
    }

    /** SOQL statements not in a loop should not be violations */
    @CsvSource({
        "ForEachStatement, for (Integer i : myList)",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSoqlStatementNotInLoop(String loopLabel, String loopStructure) {
        // spotless:off
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
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    /**
     * tests basic loops with DML statements, corresponding with DmlStatementVertex
     * (DmlDeleteStatementVertex, DmlMergeStatementVertex, etc.). any of 6 possible DML statements
     * within a loop should be a violation.
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

    /** Any of 6 possible DML statements before or after a loop should not be a violation. */
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
    public void testDmlStatementNotInLoop(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
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
                    + "try {\n"
                        + dmlStatement
                    + " existing;\n"
                    + "} catch (DmlException e) { }\n"
                + "}\n"
            + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @CsvSource(delimiterString = " | ", value = {
        // technically the myList could be in the method call or query
        "ForEachStatement | for (Integer i : myList) | convertLead(null, true)",
        "ForEachStatement | for (Integer i : myList) | countQuery('SELECT count() FROM Account')",
        "ForEachStatement | for (Integer i : myList) | countQueryWithBinds('SELECT count() FROM Account', new Map<String, Object>{'name' => 'RAPTOR'})",
        "ForEachStatement | for (Integer i : myList) | delete(new Account(3, 10, 'Dave'), false)",
        "ForEachStatement | for (Integer i : myList) | deleteAsync(List.of(new Account(15, 1, 'Sree')), false)",
        "ForEachStatement | for (Integer i : myList) | deleteImmediate(new Account(9, 2, 'Min'))",
        "ForEachStatement | for (Integer i : myList) | emptyRecycleBin(new Account[]{new Account(10, 10, 'Jay')})",
        "ForEachStatement | for (Integer i : myList) | executeBatch(null)", // TODO
        "ForEachStatement | for (Integer i : myList) | getAsyncDeleteResult(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "ForEachStatement | for (Integer i : myList) | getAsyncLocator(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "ForEachStatement | for (Integer i : myList) | getAsyncSaveResult(Database.insert(new Account(10, 11, 'Kate')))",
        "ForEachStatement | for (Integer i : myList) | getDeleted('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "ForEachStatement | for (Integer i : myList) | getQueryLocator('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1')",
        "ForEachStatement | for (Integer i : myList) | getQueryLocatorWithBinds('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1', new Map<String, Object>{'name' => 'KENSINGTON'})",
        "ForEachStatement | for (Integer i : myList) | getUpdated('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "ForEachStatement | for (Integer i : myList) | insert(new Account(10, 11, 'Kate'))",
        "ForEachStatement | for (Integer i : myList) | insertAsync(new Account(0, 4, 'Raj'), nothing())",
        "ForEachStatement | for (Integer i : myList) | insertImmediate(new Account(8, 1, 'Mike'))",
        "ForEachStatement | for (Integer i : myList) | merge(new Account(10, 11, 'Ben'), new Account(10, 11, 'Benjamin'))",
        "ForEachStatement | for (Integer i : myList) | query('SELECT Id, Age, Name FROM Accounts WHERE Id = 100 LIMIT 1')",
        "ForEachStatement | for (Integer i : myList) | queryWithBinds('SELECT ID, Age, Name FROM Accounts WHERE Id = 100', new Map<String, Object>{'Name' => 'WOLVES'})",
        "ForEachStatement | for (Integer i : myList) | rollback(Database.setSavepoint())", // shortcut since rollback + setSavepoint used in sequence
        "ForEachStatement | for (Integer i : myList) | undelete(new Account(10, 'Rod', 53), false)",
        "ForEachStatement | for (Integer i : myList) | update(new Account(10, 'Ron', 53), false)",
        "ForEachStatement | for (Integer i : myList) | updateAsync(new Account(10, 'Roy', 53), false)",
        "ForEachStatement | for (Integer i : myList) | updateImmediate(new Account(10, 'Rob', 53), false)",
        "ForEachStatement | for (Integer i : myList) | upsert(new Account(10, 'Rot', 53), null, false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | convertLead(null, true)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | countQuery('SELECT count() FROM Account')",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | countQueryWithBinds('SELECT count() FROM Account', new Map<String, Object>{'name' => 'RAPTOR'})",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | delete(new Account(3, 10, 'Dave'), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | deleteAsync(List.of(new Account(15, 1, 'Sree')), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | deleteImmediate(new Account(9, 2, 'Min'))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | emptyRecycleBin(new Account[]{new Account(10, 10, 'Jay')})",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | executeBatch(null)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getAsyncDeleteResult(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getAsyncLocator(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getAsyncSaveResult(Database.insert(new Account(10, 11, 'Kate')))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getDeleted('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getQueryLocator('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1')",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getQueryLocatorWithBinds('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1', new Map<String, Object>{'name' => 'KENSINGTON'})",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | getUpdated('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | insert(new Account(10, 11, 'Kate'))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | insertAsync(new Account(0, 4, 'Raj'), nothing())",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | insertAsync(new Account(8, 1, 'Mike'))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | merge(new Account(10, 11, 'Ben'), new Account(10, 11, 'Benjamin'))",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | query('SELECT Id, Age, Name FROM Accounts WHERE Id = 100 LIMIT 1')",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | queryWithBinds('SELECT ID, Age, Name FROM Accounts WHERE Id = 100', new Map<String, Object>{'Name' => 'WOLVES'})",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | rollback(setSavepoint())",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | undelete(new Account(10, 'Rod', 53), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | update(new Account(10, 'Ron', 53), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | updateAsync(new Account(10, 'Roy', 53), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | updateImmediate(new Account(10, 'Rob', 53), false)",
        "ForLoopStatement | for (Integer i = 0; i < 2; i++) | upsert(new Account(10, 'Rot', 53), null, false)",
        "WhileLoopStatement | while(true) | convertLead(null, true)",
        "WhileLoopStatement | while(true) | countQuery('SELECT count() FROM Account')",
        "WhileLoopStatement | while(true) | countQueryWithBinds(TODO)",
        "WhileLoopStatement | while(true) | delete(new Account(3, 10, 'Dave'), false)",
        "WhileLoopStatement | while(true) | deleteAsync(List.of(new Account(15, 1, 'Sree')), false)",
        "WhileLoopStatement | while(true) | deleteImmediate(new Account(9, 2, 'Min'))",
        "WhileLoopStatement | while(true) | emptyRecycleBin(new Account[]{new Account(10, 10, 'Jay')})",
        "WhileLoopStatement | while(true) | executeBatch(null)",
        "WhileLoopStatement | while(true) | getAsyncDeleteResult(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "WhileLoopStatement | while(true) | getAsyncLocator(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
        "WhileLoopStatement | while(true) | getAsyncSaveResult(Database.insert(new Account(10, 11, 'Kate')))",
        "WhileLoopStatement | while(true) | getDeleted('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "WhileLoopStatement | while(true) | getQueryLocator('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1')",
        "WhileLoopStatement | while(true) | getQueryLocatorWithBinds('SELECT Id, Age FROM account WHERE Id = 3 LIMIT 1', new Map<String, Object>{'name' => 'KENSINGTON'})",
        "WhileLoopStatement | while(true) | getUpdated('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "WhileLoopStatement | while(true) | insert(new Account(10, 11, 'Kate'))",
        "WhileLoopStatement | while(true) | insertAsync(new Account(0, 4, 'Raj'), nothing())",
        "WhileLoopStatement | while(true) | insertAsync(new Account(8, 1, 'Mike'))",
        "WhileLoopStatement | while(true) | merge(new Account(10, 11, 'Ben'), new Account(10, 11, 'Benjamin'))",
        "WhileLoopStatement | while(true) | query('SELECT Id, Age, Name FROM Accounts WHERE Id = 100 LIMIT 1')",
        "WhileLoopStatement | while(true) | queryWithBinds('SELECT ID, Age, Name FROM Accounts WHERE Id = 100', new Map<String, Object>{'Name' => 'WOLVES'})",
        "WhileLoopStatement | while(true) | rollback(setSavepoint())",
        "WhileLoopStatement | while(true) | undelete(new Account(10, 'Rod', 53), false)",
        "WhileLoopStatement | while(true) | update(new Account(10, 'Ron', 53), false)",
        "WhileLoopStatement | while(true) | updateAsync(new Account(10, 'Roy', 53), false)",
        "WhileLoopStatement | while(true) | updateImmediate(new Account(10, 'Rob', 53), false)",
        "WhileLoopStatement | while(true) | upsert(new Account(10, 'Rot', 53), null, false)"
    })
    @ParameterizedTest(name = "{displayName}: {0}:{2}")
    public void testDatabaseMethodWithinLoop(String loopLabel, String loopStructure, String dbMethod) {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
            + "     void foo() {\n"
            + "         List<Integer> myList = new Integer[] {3,5};\n"
            + "         Account[] accs = new Account[]{new Account(3, 10)};\n"
            + "         " + loopStructure + " {\n"
            + "             Database." + dbMethod + ";\n"
            + "         }\n"
            + "     }\n"
            + "     void nothing() {}\n"
            + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(6, new OccurrenceInfo(loopLabel, MY_CLASS, 5)));
    }

    /**
     * The second part (post-colon) of a for-each loop declaration is only run once, so a SOQL query
     * within that part should not be a violation.
     */
    @Test
    public void testSoqlQueryWithinForEachLoopIsSafe() {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Account a: [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3]) {\n"
                + "           System.debug(a);\n"
                + "       }\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    /** The termination clause of a for loop repeats, which should be a violation */
    @Test
    public void testForLoopTerminationStatement() {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "for (Integer i = 0; i < [SELECT age FROM accounts WHERE Id = 3 LIMIT 1]; i++) {\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        3,
                        new OccurrenceInfo(ASTConstants.NodeType.FOR_LOOP_STATEMENT, MY_CLASS, 3)));
    }

    /** The increment clause of a for loop repeats, which should be a violation */
    @CsvSource({
        "delete a",
        "insert a",
        "merge a a",
        "undelete a",
        "update a",
        "upsert a",
        "'Account b = [SELECT Id, Age FROM accounts WHERE Id = 3]'"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopIncrementStatement(String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "private Integer i;\n" +
                "MyClass() {\n" +
                    "this.i = 0;\n" +
                "}\n" +

                "void incr() {\n" +
                    "this.i++;\n" +
                    "Account a = new Account();\n" +
                    dmlStatement + ";\n" +
                "}\n" +

                "void foo() {\n" +
                    "for (Integer i = 0; i < 4; incr()) {\n" +
                    "}\n" +
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
                                ASTConstants.NodeType.FOR_LOOP_STATEMENT, MY_CLASS, 12)));
    }

    /** Methods called within a loop containing DML are violations. */
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
    public void testIndirectMethodCallSoql(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
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
        // spotless:on

        assertViolations(RULE, sourceCode, expect(9, new OccurrenceInfo(loopLabel, MY_CLASS, 4)));
    }

    @CsvSource({
        "'Account s = [SELECT Id, Age FROM accounts WHERE Id = 3 LIMIT 1]'",
        "delete a",
        "insert a",
        "merge a a",
        "update a",
        "undelete a",
        "upsert a"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testLoopFromStaticBlockOk(String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
                + "void foo(String[] objectNames) {\n"
                    + "for (Integer i = 0; i < objectNames.size; i++) {"
                        + "AnotherClass.donothing();\n"
                    + "}\n"
                + "}\n"
                + "public class AnotherClass {\n"
                    + "static {\n"
                        + "Account a = new Account(3, 10);\n"
                        + dmlStatement + ";\n"
                    + "}\n"
                    + "static void doNothing() {} \n"
                + "}\n"
            + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList), ForEachStatement, for (String s : myList)",
        "ForEachStatement, for (String s : myList), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "ForEachStatement, for (String s : myList), WhileLoopStatement, while(true)",
        "ForLoopStatement, for (Integer i; i < s.size; i++), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "ForLoopStatement, for (Integer i; i < s.size; i++), ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; i++), WhileLoopStatement, while(true)",
        "WhileLoopStatement, while(true), ForEachStatement, for (String s : myList)",
        "WhileLoopStatement, while(true), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "WhileLoopStatement, while(true), WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNestedLoop(String outerLoopLabel, String outerLoopStructure, String innerLoopLabel, String innerLoopStructure) {

        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "List<String> myList = new String[] {'Account', 'Contact'};\n" +
                    outerLoopStructure + "{\n" +
                        innerLoopStructure + "{\n" +
                            "Account a = [SELECT Id, Name FROM account WHERE Id = 3 LIMIT 1];\n" +
                        "}\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        // spotless:on

        assertViolations(
            RULE,
            sourceCode,
            expect(6, new OccurrenceInfo(innerLoopLabel, MY_CLASS, 5)
            )
        );
    }
}
