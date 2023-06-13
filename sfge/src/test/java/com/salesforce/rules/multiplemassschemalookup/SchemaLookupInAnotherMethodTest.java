package com.salesforce.rules.multiplemassschemalookup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SchemaLookupInAnotherMethodTest extends BaseAvoidMultipleMassSchemaLookupTest {

    @CsvSource({
        "ForEachStatement, for (String s : myList)",
        "ForLoopStatement, for (Integer i; i < s.size; s++)",
        "WhileLoopStatement, while(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testExternalInvocationFromLoop(String loopAstLabel, String loopStructure) {
        // spotless:off
        String sourceCode =
            "public class MyClass {\n" +
                "   void foo() {\n" +
                "       List<String> myList = new String[] {'Account','Contact'};\n" +
                "       " + loopStructure + " {\n" +
                "           getObjectDescribes(myList);\n" +
                "       }\n" +
                "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n";
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        9,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        4,
                        "MyClass",
                        MmslrUtil.RepetitionType.LOOP,
                        loopAstLabel));
    }

    @Test
    public void testMethodCallWithinForEachLoopIsSafe_Level1() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Schema.DescribeSObjectResult objDesc: getObjectDescribes(objectList)) {\n"
                + "           System.debug(objDesc.getLabel());\n"
                + "       }\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testMethodCallWithinForEachLoopIsSafe_Level2() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Schema.DescribeSObjectResult objDesc: getObjectDescribes(objectList)) {\n"
                + "           System.debug(objDesc.getLabel());\n"
                + "       }\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return getObjectDescribesLevel2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribesLevel2(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }
}
