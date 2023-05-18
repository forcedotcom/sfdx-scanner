package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.rules.MultipleMassSchemaLookupRule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// TODO: Breakdown to more test suites
public class MultipleMassSchemaLookupRuleTest extends BaseAvoidMultipleMassSchemaLookupTest {

    private static final MultipleMassSchemaLookupRule RULE =
            MultipleMassSchemaLookupRule.getInstance();

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSimpleGgdInLoop(String loopAstLabel, String loopStructure) {
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

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        5,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        loopAstLabel));
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: Only surrounding loop should be counted as a violation.
    public void testSimpleGgdOutsideLoop(String loopAstLabel, String loopStructure) {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       List<String> myList = new String[] {'Account','Contact'};\n" +
                "       " + loopStructure + " {\n" +
                "           String s = 'abc';\n" +
                "       }\n" +
                "       Schema.getGlobalDescribe();\n" +
                "   }\n" +
                "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGgdInLoopInMethodCall(String loopAstLabel, String loopStructure) {
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

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        9,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        loopAstLabel));
    }

    @CsvSource({
        "ForEachStatement, for (String s1 : myList), for (String s2 : myList)",
        "ForLoopStatement, for (Integer i; i < myList.size; s++), for (Integer j; j < myList.size; s++)",
        "WhileLoopStatement, while(true), while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testLoopWithinLoop(
            String loopAstLabel, String loopStructure1, String loopStructure2) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n" +
                "   void foo(String[] myList) {\n" +
                    "   " + loopStructure1 + " {\n" +
                    "       " + loopStructure2 + " {\n" +
                    "           Schema.getGlobalDescribe();\n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n" +
                "}\n";
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        5,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        loopAstLabel));
    }

    @CsvSource({
        "GGD followed by GGD, Schema.getGlobalDescribe, Schema.getGlobalDescribe",
        "GGD followed by DSO, Schema.getGlobalDescribe, Schema.describeSObjects",
        "DSO followed by DSO, Schema.describeSObjects, Schema.describeSObjects",
        "DSO followed by GGD, Schema.describeSObjects, Schema.getGlobalDescribe"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testMultipleInvocations(String testName, String firstCall, String secondCall) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   void foo() {\n"
                        + "       " + firstCall + "();\n"
                        + "       " + secondCall + "();\n"
                        + "   }\n"
                        + "}\n";
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        4,
                        secondCall,
                        3,
                        "MyClass",
                        RuleConstants.RepetitionType.MULTIPLE,
                        firstCall));
    }

    @Test
    public void testForEachLoopInClassInstance() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "       Another[] anotherList = new Another[] {new Another()};\n"
                    + "       for (Another an : anotherList) {\n"
                    + "           an.exec();\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Another {\n"
                    + "void exec() {\n"
                    + "   Schema.getGlobalDescribe();\n"
                    + "}\n"
                    + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        3,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_EACH_STATEMENT));
    }

    /** TODO: Handle path expansion from array[index].methodCall() */
    @Test
    @Disabled
    public void testForLoopInClassInstance() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "       Another[] anotherList = new Another[] {new Another()};\n"
                    + "       for (Integer i = 0 ; i < anotherList.size; i++) {\n"
                    + "           anotherList[i].exec();\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Another {\n"
                    + "void exec() {\n"
                    + "   Schema.getGlobalDescribe();\n"
                    + "}\n"
                    + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        3,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        3,
                        "Another",
                        RuleConstants.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_LOOP_STATEMENT));
    }

    @Test
    public void testForLoopConditionalOnClassInstance() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "       Another[] anotherList = new Another[] {new Another(false), new Another(false)};\n"
                    + "       for (Another an : anotherList) {\n"
                    + "           an.exec();\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Another {\n"
                    + "boolean shouldCheck;\n"
                    + "Another(boolean b) {\n"
                    + "   shouldCheck = b;\n"
                    + "}\n"
                    + "void exec() {\n"
                    + "   if (shouldCheck) {\n"
                    + "       Schema.getGlobalDescribe();\n"
                    + "   }\n"
                    + "}\n"
                    + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        8,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_EACH_STATEMENT));
    }

    @Test // TODO: Check if this is a false positive. Static block should get invoked only once
    public void testLoopFromStaticBlock() {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   void foo(String[] objectNames) {\n"
                    + "       for (Integer i = 0; i < objectNames.size; i++) {\n"
                    + "           AnotherClass.doNothing();\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class AnotherClass {\n"
                    + "   static {\n"
                    + "       Schema.getGlobalDescribe();\n"
                    + "   }\n"
                    + "   static void doNothing() {\n"
                    + "   // do nothing \n"
                    + "   }\n"
                    + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        3,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        3,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_LOOP_STATEMENT));
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testLoopAfterGgdShouldNotGetViolation(String loopAstLabel, String loopStructure) {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n"
                        + "   void foo() {\n"
                        + "       Schema.getGlobalDescribe();\n"
                        + "       " + loopStructure + " {\n"
                        + "           List<String> myList = new String[] {'Account','Contact'};\n"
                        + "           System.debug('hi');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testLoopBeforeAndAroundGgd(String loopAstLabel, String loopStructure) {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       List<String> myList = new String[] {'Account','Contact'};\n"
                + "       " + loopStructure + " {\n"
                + "           System.debug('hi');\n"
                + "       }\n"
                + "       " + loopStructure + " {\n"
                + "         Schema.getGlobalDescribe();\n"
                + "       }\n"
                + "   }\n"
                + "}\n";
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        8,
                        RuleConstants.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        7,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        loopAstLabel));
    }
}
