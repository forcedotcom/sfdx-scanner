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

    protected ViolationWrapper.DmlInLoopInfoBuider expect(
        int sinkLine, String dmlTypeLabel, OccurrenceInfo occurrenceInfo) {
        return ViolationWrapper.DmlInLoopInfoBuider.get(sinkLine, dmlTypeLabel, occurrenceInfo);
    }

    @CsvSource({
        "ForEachStatement, for (Integer i : myList)",
        "ForLoopStatement, for (Integer i; i < 2; i++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void DmlInLoopOne(String loopLabel, String loopStructure) {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "List<Integer> myList = new Integer[] {3,5};\n" +
                    loopStructure + " {\n" +
                            "Account myAcct = \n " +
                            "[SELECT Id, Name, BillingCity FROM Account WHERE Id = :i];" +
                    "}\n" +
                "}\n" +
            "}\n"
        };

        assertViolations(
            RULE,
            sourceCode,
            expect(
                6,
                "SoqlExpression",
                new OccurrenceInfo(
                    loopLabel,
                    MY_CLASS,
                    4
                )
            )
        );
    }


}
