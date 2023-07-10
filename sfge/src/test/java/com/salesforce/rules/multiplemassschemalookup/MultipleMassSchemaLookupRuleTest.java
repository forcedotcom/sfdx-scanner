package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.apex.jorje.ASTConstants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// TODO: Breakdown to more test suites
public class MultipleMassSchemaLookupRuleTest extends BaseAvoidMultipleMassSchemaLookupTest {

    @CsvSource(
            delimiterString = " | ",
            value = {
                "ForEachStatement | for (String s : myList)",
                "ForLoopStatement | for (Integer i; i < s.size; s++)",
                "WhileLoopStatement | while(true)",
                "ForEachStatement | for (Account a: [SELECT Id, Name, NumberOfEmployees, BillingCity FROM Accounts WHERE NumberOfEmployees = 30])"
            })
    @ParameterizedTest(name = "{displayName}: {0}:{1}")
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(loopAstLabel, MY_CLASS, 4));
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(loopAstLabel, MY_CLASS, 4));
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(loopAstLabel, MY_CLASS, 4));
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
                expect(4, secondCall, MmslrUtil.RepetitionType.PRECEDED_BY)
                        .withOccurrence(firstCall, MY_CLASS, 3));
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(ASTConstants.NodeType.FOR_EACH_STATEMENT, MY_CLASS, 4));
    }

    @Test
    public void testMethodCallWithinForEachLoopIsSafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Schema.DescribeSObjectResult objDesc: Schema.describeSObjects(objectList)) {\n"
                + "           System.debug(objDesc.getLabel());\n"
                + "       }\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(ASTConstants.NodeType.FOR_LOOP_STATEMENT, "Another", 3));
    }

    @Test
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

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testLoopFromStaticBlock_nestedLoop() {
        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n"
                + "   void foo(String[] objectNames) {\n"
                + "       for (Integer i = 0; i < objectNames.size; i++) {\n"
                + "           FirstClass fc = new FirstClass();\n"
                + "           fc.redirect(i);\n"
                + "       }\n"
                + "   }\n"
                + "}\n",
            "public class FirstClass {\n" +
                "   void redirect(Integer lim) {\n" +
                "       for (Integer i = 0; i < lim; i++) {\n" +
                "           AnotherClass.doNothing();\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
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

        assertNoViolation(RULE, sourceCode);
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
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(loopAstLabel, MY_CLASS, 7));
    }

    @CsvSource({
        "ForEachStatement, for (String s : myList), ForEachStatement, for (String s : myList)",
        "ForEachStatement, for (String s : myList), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "ForEachStatement, for (String s : myList), WhileLoopStatement, while(true)",
        "ForLoopStatement, for (Integer i; i < s.size; i++), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "ForLoopStatement, for (Integer i; i < s.size; i++), ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size(); i++), WhileLoopStatement, while(true)",
        "WhileLoopStatement, while(true), ForEachStatement, for (String s : myList)",
        "WhileLoopStatement, while(true), ForLoopStatement, for (Integer i; i < s.size; i++)",
        "WhileLoopStatement, while(true), WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNestedLoop(
            String outerLoopLabel,
            String outerLoopStructure,
            String innerLoopLabel,
            String innerLoopStructure) {

        // spotless:off
        String[] sourceCode = {
            "public class MyClass {\n" +
                "void foo() {\n" +
                    "List<String> myList = new String[] {'Account', 'Contact'};\n" +
                    outerLoopStructure + "{\n" +
                        innerLoopStructure + "{\n" +
                            "Schema.getGlobalDescribe();\n" +
                        "}\n" +
                    "}\n" +
                "}\n" +
            "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                                6,
                                MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                                MmslrUtil.RepetitionType.LOOP)
                        .withOccurrence(innerLoopLabel, MY_CLASS, 5));
    }
}
