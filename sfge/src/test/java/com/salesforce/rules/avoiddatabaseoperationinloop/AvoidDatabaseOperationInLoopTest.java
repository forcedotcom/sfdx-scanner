package com.salesforce.rules.avoiddatabaseoperationinloop;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.rules.AvoidDatabaseOperationInLoop;
import com.salesforce.rules.ops.OccurrenceInfo;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AvoidDatabaseOperationInLoopTest extends BasePathBasedRuleTest {

    private static final String MY_CLASS = "MyClass";

    protected static final AvoidDatabaseOperationInLoop RULE =
            AvoidDatabaseOperationInLoop.getInstance();

    /**
     * function to get the expected violation from some code and an loop OccurrenceInfo
     *
     * @param sinkLine the line on which the sink vertex occurs
     * @param occurrenceInfo the information about the loop
     * @return a {@link ViolationWrapper.AvoidDatabaseInLoopInfoBuilder}
     */
    protected ViolationWrapper.AvoidDatabaseInLoopInfoBuilder expect(
            int sinkLine, OccurrenceInfo occurrenceInfo) {
        return ViolationWrapper.AvoidDatabaseInLoopInfoBuilder.get(sinkLine, occurrenceInfo);
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
            "public class " + MY_CLASS + " {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + loopStructure + " {\n"
                        + "Account myAcct = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = :i LIMIT 1];\n"
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
            "public class " + MY_CLASS + " {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + "Account myAcct = \n "
                    + "[SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = :i];\n"
                    + loopStructure + " {}\n"
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
    @ParameterizedTest(name = "{displayName}: {2} within {0}")
    public void testDmlStatementInLoop(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
        "public class " + MY_CLASS + " {\n"
            + "void foo() {\n"
                + "List<Integer> myList = new Integer[] {3,5};\n"
                + "Account newAcct = new Account(name = 'Acme');\n"
                + "Account existing = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3];\n"
                + loopStructure + " {\n"
                    + dmlStatement + " existing;\n"
                + "}\n"
            + "}\n"
        + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(7, new OccurrenceInfo(loopLabel, MY_CLASS, 6)));
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
    @ParameterizedTest(name = "{displayName}: {2} outside of {0}")
    public void testDmlStatementNotInLoop(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n"
                + "void foo() {\n"
                    + "List<Integer> myList = new Integer[] {3,5};\n"
                    + "Account newAcct = new Account(name = 'Acme');\n"
                    + "Account existing = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3];\n"
                    + dmlStatement  + " existing;\n"
                    + loopStructure + " { }\n"
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

    @MethodSource("getDatabaseClassMethodsInLoopsTests")
    @ParameterizedTest(name = "{displayName}: Database.{2} within {0}")
    public void testDatabaseMethodWithinLoop(
            String loopLabel, String loopStructure, String dbMethod) {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n"
            + "     void foo(List<Integer> myList) {\n"
            + "         Account[] accs = new Account[]{new Account(3, 10)};\n"
            + "         " + loopStructure + " {\n"
            + "             Database." + dbMethod + ";\n"
            + "         }\n"
            + "     }\n"
            + "     void nothing() {}\n"
            + "}\n"
        };
        // spotless:on

        assertViolations(RULE, sourceCode, expect(5, new OccurrenceInfo(loopLabel, MY_CLASS, 4)));
    }

    /** helper method to provide inputs for the above test */
    private static Stream<Arguments> getDatabaseClassMethodsInLoopsTests() {
        // spotless:off
        String[] loopStructures = {
            "for (Integer i : myList)",
            "for (Integer i = 0; i < 2; i++)",
            "while(true)"
        };

        String[] loopLabels = {
            "ForEachStatement",
            "ForLoopStatement",
            "WhileLoopStatement",
        };
        // spotless:on

        String[] databaseMethods = {
            "convertLead(null, true)",
            "countQuery('SELECT count() FROM Account')",
            "countQueryWithBinds('SELECT count() FROM Account', new Map<String, Object>{'name' => 'RAPTOR'})",
            "delete(new Account(3, 10, 'Dave'), false)",
            "deleteAsync(List.of(new Account(15, 1, 'Sree')), false)",
            "deleteImmediate(new Account(9, 2, 'Min'))",
            "emptyRecycleBin(new Account[]{new Account(10, 10, 'Jay')})",
            "executeBatch(null)",
            "getAsyncDeleteResult(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
            "getAsyncLocator(Database.deleteAsync(List.of(new Account(15, 1, 'Sree')), false))",
            "getAsyncSaveResult(Database.insert(new Account(10, 11, 'Kate')))",
            "getDeleted('account__c', Datetime.now().addHours(-1), Datetime.now())",
            "getQueryLocator('SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1')",
            "getQueryLocatorWithBinds('SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1', new Map<String, Object>{'name' => 'KENSINGTON'})",
            "getUpdated('account__c', Datetime.now().addHours(-1), Datetime.now())",
            "insert(new Account(10, 11, 'Kate'))",
            "insertAsync(new Account(0, 4, 'Raj'), nothing())",
            "insertImmediate(new Account(8, 1, 'Mike'))",
            "merge(new Account(10, 11, 'Ben'), new Account(10, 11, 'Benjamin'))",
            "query('SELECT Id, NumberOfEmployees, Name FROM Account WHERE NumberOfEmployees = 100 LIMIT 1')",
            "queryWithBinds('SELECT ID, NumberOfEmployees, Name FROM Account WHERE NumberOfEmployees = 100', new Map<String, Object>{'Name' => 'WOLVES'})",
            "rollback(Database.setSavepoint())",
            "undelete(new Account(10, 'Rod', 53), false)",
            "update(new Account(10, 'Ron', 53), false)",
            "updateAsync(new Account(10, 'Roy', 53), false)",
            "updateImmediate(new Account(10, 'Rob', 53), false)",
            "upsert(new Account(10, 'Rot', 53), null, false)"
        };

        Stream.Builder<Arguments> argsBuilder = Stream.builder();

        for (int i = 0; i < loopStructures.length; i++) {
            for (String method : databaseMethods) {
                argsBuilder.add(Arguments.of(loopLabels[i], loopStructures[i], method));
            }
        }

        return argsBuilder.build();
    }

    /**
     * The second part (post-colon) of a for-each loop declaration is only run once, so a SOQL query
     * within that part should not be a violation.
     */
    @Test
    public void testSoqlQueryWithinForEachLoopIsSafe() {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Account a: [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3]) {\n"
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
            "public class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                    "for (Integer i = 0; i < [SELECT NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1]; i++) {\n" +
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
    @ValueSource(
            strings = {
                "delete a",
                "insert a",
                "merge a a",
                "undelete a",
                "update a",
                "upsert a",
                "Account b = [SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3]"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopIncrementStatement(String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "private Integer i;\n" +

                MY_CLASS + "() {\n" +
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
        "ForEachStatement, for (Integer i : myList), 'Account acc = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3 LIMIT 1];'",
        "ForEachStatement, for (Integer i : myList), delete one;",
        "ForEachStatement, for (Integer i : myList), insert one;",
        "ForEachStatement, for (Integer i : myList), merge one one;",
        "ForEachStatement, for (Integer i : myList), undelete one;",
        "ForEachStatement, for (Integer i : myList), update one;",
        "ForEachStatement, for (Integer i : myList), upsert one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), 'Account acc = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3 LIMIT 1];'",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), delete one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), insert one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), merge one one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), undelete one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), update one;",
        "ForLoopStatement, for (Integer i = 0; i < 2; i++), upsert one;",
        "WhileLoopStatement, while(true), 'Account acc = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 3 LIMIT 1];'",
        "WhileLoopStatement, while(true), delete one;",
        "WhileLoopStatement, while(true), insert one;",
        "WhileLoopStatement, while(true), merge one one;",
        "WhileLoopStatement, while(true), undelete one;",
        "WhileLoopStatement, while(true), update one;",
        "WhileLoopStatement, while(true), upsert one;",
    })
    @ParameterizedTest(name = "{displayName}: {2} in method called in {0}")
    public void testIndirectMethodCallSoql(
            String loopLabel, String loopStructure, String dmlStatement) {
        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                    "Account one = [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Account WHERE NumberOfEmployees = 2 LIMIT 1]; \n " +
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

    @ValueSource(
            strings = {
                "Account s = [SELECT Id, NumberOfEmployees FROM Account WHERE NumberOfEmployees = 3 LIMIT 1]",
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
            "public class " + MY_CLASS + " {\n"
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
        "ForEachStatement, for (String s : myList), ForLoopStatement, for (Integer i; i < myList.size; i++)",
        "ForEachStatement, for (String s : myList), WhileLoopStatement, while(true)",
        "ForLoopStatement, for (Integer i; i < myList.size; i++), ForLoopStatement, for (Integer i; i < myList.size; i++)",
        "ForLoopStatement, for (Integer i; i < myList.size; i++), ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < myList.size; i++), WhileLoopStatement, while(true)",
        "WhileLoopStatement, while(true), ForEachStatement, for (String s : myList)",
        "WhileLoopStatement, while(true), ForLoopStatement, for (Integer i; i < myList.size; i++)",
        "WhileLoopStatement, while(true), WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0} outer loop and {2} inner loop")
    public void testNestedLoop(
            String outerLoopLabel,
            String outerLoopStructure,
            String innerLoopLabel,
            String innerLoopStructure) {

        // spotless:off
        String[] sourceCode = {
            "public class " + MY_CLASS + " {\n" +
                "void foo() {\n" +
                    "List<String> myList = new String[] {'Account', 'Contact'};\n" +
                    outerLoopStructure + "{\n" +
                        innerLoopStructure + "{\n" +
                            "Account a = [SELECT Id, NumberOfEmployees, Name FROM account WHERE NumberOfEmployees = 3 LIMIT 1];\n" +
                        "}\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        // spotless:on

        assertViolations(
                RULE, sourceCode, expect(6, new OccurrenceInfo(innerLoopLabel, MY_CLASS, 5)));
    }
}
