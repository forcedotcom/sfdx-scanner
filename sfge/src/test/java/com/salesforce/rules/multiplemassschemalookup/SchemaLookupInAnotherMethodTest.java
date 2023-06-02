package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.apex.jorje.ASTConstants;
import org.junit.jupiter.api.Disabled;
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
                        RuleConstants.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        4,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
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

    @Test
    @Disabled // TODO: Handle tracking multiple calls to the same method
    public void testMultipleCallsToExternalSchemaMethod() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n" +
                "       getObjectDescribes(objectList);\n" +
                "       getObjectDescribes(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        5,
                        "getObjectDescribes",
                        1,
                        "MyClass",
                        RuleConstants.RepetitionType.MULTIPLE,
                        "Schema.describeSObjects"));
    }

    @Test
    @Disabled // TODO: Handle tracking multiple calls to the same method
    public void testMultipleCallsToExternalSchemaMethod_differentPaths() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n" +
                "       getObjectDescribes1(objectList);\n" +
                "       getObjectDescribes2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes1(String[] objectList) {\n"
                + "     return getObjectDescribes2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes2(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        5,
                        "getObjectDescribes",
                        1,
                        "MyClass",
                        RuleConstants.RepetitionType.MULTIPLE,
                        "Schema.describeSObjects"));
    }

    @Test
    public void testMultipleCallsToExternalSchemaMethod_differentMethods() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n" +
                "       getObjectDescribes1(objectList);\n" +
                "       getObjectDescribes2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes1(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes2(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        11,
                        RuleConstants.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        8,
                        "MyClass",
                        RuleConstants.RepetitionType.MULTIPLE,
                        "Schema.describeSObjects"));
    }

    @Test
    public void testNestedLookupFromForEachValue() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] strList = new String[] {'abc','def'};\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (Integer i = 0; i < strList.size; i++) {\n"
                + "         for (Schema.DescribeSObjectResult objDesc: getObjectDescribes(objectList)) {\n"
                + "             System.debug(objDesc.getLabel());\n"
                + "         }\n"
                + "       }\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        12,
                        RuleConstants.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        5,
                        "MyClass",
                        RuleConstants.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_LOOP_STATEMENT));
    }

    @Test
    public void testNestedLookupFromForEachValueWithEffectiveExclusion() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       for (String label: getLabelValues(objectList)) {\n"
                + "           System.debug(label);\n"
                + "       }\n"
                + "   }\n"
                + "   String[] getLabelValues(String[] objectList) {\n"
                + "       String[] labels = new String[]{};\n"
                + "       for (Schema.DescribeSObjectResult objDesc: getObjectDescribes(objectList)) {\n"
                + "           labels.add(objDesc.getLabel());\n"
                + "       }\n"
                + "       return labels;\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }
}
