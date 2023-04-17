package com.salesforce.rules.getglobaldescribe;

import com.salesforce.rules.GetGlobalDescribeViolationRule;
import com.salesforce.testutils.BasePathBasedRuleTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

//TODO: break down test, add updated assertions
public class GetGlobalDescribeViolationRuleTest extends BasePathBasedRuleTest {

    private static final GetGlobalDescribeViolationRule RULE = GetGlobalDescribeViolationRule.getInstance();

    @CsvSource({
        "ForEach, for (String s : myList)",
        "For, for (Integer i; i < s.size; s++)",
        "While, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleGgdInLoop(String testName, String loopStructure) {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
            "   void foo() {\n" +
            "       List<String> myList = new String[] {'Account','Contact'};\n" +
            "       " + loopStructure + " {\n" +
            "           Schema.getGlobalDescribe();\n" +
            "       }\n" +
            "   }\n" +
            "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @CsvSource({
        "ForEach, for (String s : myList)",
        "For, for (Integer i; i < s.size; s++)",
        "While, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGgdInLoopInMethodCall(String testName, String loopStructure) {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       List<String> myList = new String[] {'Account','Contact'};\n" +
                "       " + loopStructure + " {\n" +
                "           getMoreInfo();\n" +
                "       }\n" +
                "   }\n" +
                "   void getMoreInfo() {\n" +
                "           Schema.getGlobalDescribe();\n" +
                "   }\n" +
                "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

}
