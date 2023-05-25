package com.salesforce.rules.multiplemassschemalookup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SchemaLookupInAnotherMethodTest extends BaseAvoidMultipleMassSchemaLookupTest {
    @Test
    @Disabled // TODO: Address Schema calls that happen through indirection
    public void testMethodCallWithinForEachLoopIsSafe() {
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
    @Disabled // TODO: Address Schema calls that happen through indirection
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
}
