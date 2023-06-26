package com.salesforce.rules.dmlinloop;

import com.salesforce.rules.DmlInLoopRule;
import com.salesforce.rules.OccurrenceInfo;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DmlInLoopRuleTest extends BasePathBasedRuleTest {

    private static final String MY_CLASS = "MyClass";

    protected static final DmlInLoopRule RULE = DmlInLoopRule.getInstance();

    /**
     * function to get the expected violation from some code and an loop OccurrenceInfo
     * @param sinkLine the line on which the sink vertex occurs
     * @param occurrenceInfo the information about the loop
     * @return a ViolationWrapper.DmlInLoopInfoBuilder
     */
    protected ViolationWrapper.DmlInLoopInfoBuilder expect(
        int sinkLine, OccurrenceInfo occurrenceInfo) {
        return ViolationWrapper.DmlInLoopInfoBuilder.get(sinkLine, occurrenceInfo);
    }

    @CsvSource({
        "ForEachStatement, for (Integer i : myList)",
        "ForLoopStatement, for (Integer i; i < 2; i++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSoqlStatementInLoop(String loopLabel, String loopStructure) {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "List<Integer> myList = new Integer[] {3,5};\n" +
                    loopStructure + " {\n" +
                            "Account myAcct = \n " +
                            "[SELECT Id, Name, BillingCity FROM Account WHERE Id = :i];\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };

        assertViolations(
            RULE,
            sourceCode,
            expect(
                6,
                new OccurrenceInfo(
                    loopLabel,
                    MY_CLASS,
                    4
                )
            )
        );
    }



    @CsvSource({
        "ForEachStatement, for (Integer i : myList), delete",
        "ForEachStatement, for (Integer i : myList), insert",
        "ForEachStatement, for (Integer i : myList), merge newAcct",
        "ForEachStatement, for (Integer i : myList), undelete",
        "ForEachStatement, for (Integer i : myList), update",
        "ForEachStatement, for (Integer i : myList), upsert",
        "ForLoopStatement, for (Integer i; i < 2; i++), delete",
        "ForLoopStatement, for (Integer i; i < 2; i++), insert",
        "ForLoopStatement, for (Integer i; i < 2; i++), merge newAcct",
        "ForLoopStatement, for (Integer i; i < 2; i++), undelete",
        "ForLoopStatement, for (Integer i; i < 2; i++), update",
        "ForLoopStatement, for (Integer i; i < 2; i++), upsert",
        "WhileLoopStatement, while(true), delete",
        "WhileLoopStatement, while(true), insert",
        "WhileLoopStatement, while(true), merge newAcct",
        "WhileLoopStatement, while(true), undelete",
        "WhileLoopStatement, while(true), update",
        "WhileLoopStatement, while(true), upsert"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlStatementInLoop(String loopLabel, String loopStructure, String dmlStatement) {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "List<Integer> myList = new Integer[] {3,5};\n" +
                    "Account newAcct = new Account(name = 'Acme');\n" +
                    "Account existing = [SELECT Id, Name, BillingCity FROM Accounts WHERE Id = 3];\n" +
                    loopStructure + " {\n" +
                        "try {\n" +
                            dmlStatement + " existing;\n" +
                        "} catch (DmlException e) { }\n" +
                "}\n" +
                "}\n" +
            "}\n"
        };

        assertViolations(
            RULE,
            sourceCode,
            expect(
                8,
                new OccurrenceInfo(
                    loopLabel,
                    MY_CLASS,
                    6
                )
            )
        );
    }


}
