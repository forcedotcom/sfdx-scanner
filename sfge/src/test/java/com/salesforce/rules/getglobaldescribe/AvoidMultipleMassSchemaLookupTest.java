package com.salesforce.rules.getglobaldescribe;

import com.salesforce.rules.AvoidMultipleMassSchemaLookup;
import com.salesforce.testutils.BasePathBasedRuleTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

//TODO: break down test, add updated assertions
public class AvoidMultipleMassSchemaLookupTest extends BasePathBasedRuleTest {

    private static final AvoidMultipleMassSchemaLookup RULE = AvoidMultipleMassSchemaLookup.getInstance();

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

    @Test
    public void testLoopWithinLoop() {
        // spotless:off
        String sourceCode =
                "public class MyClass {\n" +
                "   void foo(String[] objectNames) {\n" +
                    "   for (Integer i = 0; i < objectNames.size; i++) {\n" +
                    "       for (Integer j = 0; j < objectNames.size; j++) {\n" +
                    "           Schema.getGlobalDescribe();\n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n" +
                "}\n";
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testMultipleInvocations() {
        String sourceCode =
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       Schema.getGlobalDescribe();\n" +
                "       Schema.getGlobalDescribe();\n" +
                "   }\n" +
                "}\n";

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testForEachLoopInClassInstance() {
        String sourceCode[] = {
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       Another[] anotherList = new Another[] {new Another()};\n" +
                "       for (Another an : anotherList) {\n" +
                "           an.exec();\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Another {\n" +
                "void exec() {\n" +
                "   Schema.getGlobalDescribe();\n" +
                "}\n" +
                "}\n"
        };

        assertNoViolation(RULE, sourceCode);

    }

    @Test
    public void testForLoopInClassInstance() {
        String sourceCode[] = {
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       Another[] anotherList = new Another[] {new Another()};\n" +
                "       for (Integer i = 0 ; i < anotherList.size; i++) {\n" +
                "           anotherList[i].exec();\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Another {\n" +
                "void exec() {\n" +
                "   Schema.getGlobalDescribe();\n" +
                "}\n" +
                "}\n"
        };

        assertNoViolation(RULE, sourceCode);

    }

    @Test
    public void testForLoopConditionalOnClassInstance() {
        String sourceCode[] = {
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       Another[] anotherList = new Another[] {new Another(false), new Another(false)};\n" +
                "       for (Another an : anotherList) {\n" +
                "           an.exec();\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Another {\n" +
                "boolean shouldCheck;\n" +
                "Another(boolean b) {\n" +
                "   shouldCheck = b;\n" +
                "}\n" +
                "void exec() {\n" +
                "   if (shouldCheck) {\n" +
                "       Schema.getGlobalDescribe();\n" +
                "   }\n" +
                "}\n" +
                "}\n"
        };

        assertNoViolation(RULE, sourceCode);
    }

    @Test // TODO: Check if this is a false positive. Static block should get invoked only once
    public void testLoopFromStaticBlock() {
        String[] sourceCode =
            {
                "public class MyClass {\n" +
                    "   void foo(String[] objectNames) {\n" +
                    "       for (Integer i = 0; i < objectNames.size; i++) {\n" +
                    "           AnotherClass.doNothing();\n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n",
                "public class AnotherClass {\n" +
                    "   static {\n" +
                    "       Schema.getGlobalDescribe();\n" +
                    "   }\n" +
                    "   static void doNothing() {\n" +
                    "   // do nothing \n" +
                    "   }\n" +
                "}\n"
            };

        assertNoViolation(RULE, sourceCode);
    }


}
