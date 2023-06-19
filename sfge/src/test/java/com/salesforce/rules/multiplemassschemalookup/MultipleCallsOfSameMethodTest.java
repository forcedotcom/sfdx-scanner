package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.apex.jorje.ASTConstants;
import org.junit.jupiter.api.Test;

public class MultipleCallsOfSameMethodTest extends BaseAvoidMultipleMassSchemaLookupTest {
    @Test
    public void testSimpleUnsafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       getObjectDescribes(objectList);\n"
                + "       getObjectDescribes(objectList);\n"
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
                        8,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        4,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes"),
                expect(
                        8,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        5,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes"));
    }

    @Test
    public void testDifferentPathsUnsafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       getObjectDescribes1(objectList);\n"
                + "       getObjectDescribes2(objectList);\n"
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
                        11,
                        "Schema.describeSObjects",
                        5,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"),
                expect(
                        11,
                        "Schema.describeSObjects",
                        8,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"));
    }

    @Test
    public void testDifferentPathsMultipleLevels() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       getObjectDescribes1(objectList);\n"
                + "       getObjectDescribes2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes1(String[] objectList) {\n"
                + "     return getObjectDescribes2(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes2(String[] objectList) {\n"
                + "     return getObjectDescribes3(objectList);\n"
                + "   }\n"
                + "   List<Schema.DescribeSObjectResult> getObjectDescribes3(String[] objectList) {\n"
                + "     return Schema.describeSObjects(objectList);\n"
                + "   }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        14,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        11,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes3"),
                expect(
                        14,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        8,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"),
                expect(
                        14,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        5,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"));
    }

    @Test
    public void testConditionalsSafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo(boolean b) {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       if (b) {\n"
                + "           getObjectDescribes1(objectList);\n"
                + "       } else {\n"
                + "           getObjectDescribes2(objectList);\n"
                + "       }\n"
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

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testConditionalsWithOneUnsafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo(boolean b) {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       if (b) {\n"
                + "           getObjectDescribes1(objectList);\n"
                + "           getObjectDescribes2(objectList);\n"
                + "       } else {\n"
                + "           getObjectDescribes2(objectList);\n"
                + "       }\n"
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
                        15,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        6,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"),
                expect(
                        15,
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        12,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes2"));
    }

    @Test
    public void testConditionalsWithOneExpensiveRepeat() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo(boolean b) {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n" +
                "       if (b) {\n" +
                "           getObjectDescribes(objectList);\n" +
                "           getObjectDescribes(objectList);\n" +
                "       } else {\n" +
                "           doNothing(objectList);\n" +
                "           doNothing(objectList);\n" +
                "       }\n" +
                "   }\n"
                + "   List<Schema.DescribeSObjectResult> doNothing(String[] objectList) {\n"
                + "     System.debug('hi');\n"
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
                        16,
                        "Schema.describeSObjects",
                        5,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes"),
                expect(
                        16,
                        "Schema.describeSObjects",
                        6,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "getObjectDescribes"));
    }

    @Test
    public void testRepeatsWithNoExpensiveCall() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo(boolean b) {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n" +
                "           doNothing(objectList);\n" +
                "           doNothing(objectList);\n" +
                "   }\n"
                + "   List<Schema.DescribeSObjectResult> doNothing(String[] objectList) {\n"
                + "     System.debug('hi');\n"
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
    public void testDifferentMethodsUnsafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       String[] objectList = new String[] {'Account','Contact'};\n"
                + "       getObjectDescribes1(objectList);\n"
                + "       getObjectDescribes2(objectList);\n"
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
                        MmslrUtil.METHOD_SCHEMA_DESCRIBE_SOBJECTS,
                        8,
                        "MyClass",
                        MmslrUtil.RepetitionType.MULTIPLE,
                        "Schema.describeSObjects"));
    }

    @Test
    public void testConstructorInvocationUnsafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "     Another a1 = new Another();\n"
                + "     Another a2 = new Another();\n"
                + "   }\n"
                + "}\n",
            "public class Another {\n"
                + " public Map<String,Schema.SObjectType> types;\n"
                + " Another() {\n"
                + "     types = Schema.getGlobalDescribe();\n"
                + " }\n"
                + "}\n"
        };
        // spotless:on

        assertViolations(
                RULE,
                sourceCode,
                expect(
                        4,
                        MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        3,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "Another"),
                expect(
                        4,
                        MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        MmslrUtil.RepetitionType.ANOTHER_PATH,
                        "Another"));
    }

    @Test
    public void testConstructorInvocationWithExternalLoop() {
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
                        MmslrUtil.METHOD_SCHEMA_GET_GLOBAL_DESCRIBE,
                        4,
                        "MyClass",
                        MmslrUtil.RepetitionType.LOOP,
                        ASTConstants.NodeType.FOR_EACH_STATEMENT));
    }

    @Test
    public void testConstructorInvocationSafe() {
        // spotless:off
        String sourceCode[] = {
            "public class MyClass {\n"
                + "   void foo() {\n"
                + "       Another[] anotherList = new Another[] {new Another(), new Another()};\n"
                + "     Another a1 = new Another();\n"
                + "     Another a2 = new Another();\n" +
                "       Map<String,Schema.SObjectType> types = Schema.getGlobalDescribe();\n"
                + "   }\n"
                + "}\n",
            "public class Another {\n"
                + " boolean b1;\n"
                + " Another() {\n"
                + "     b1 = true;\n"
                + " }\n"
                + "}\n"
        };
        // spotless:on

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testSameMethodInvokedSafe() {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "     anotherMethod();\n"
                    + "     anotherMethod();\n"
                    + "       Map<String,Schema.SObjectType> types = Schema.getGlobalDescribe();\n"
                    + "   }\n"
                    + "     void anotherMethod() {\n"
                    + "           System.debug('hi');\n"
                    + "       }\n"
                    + "}\n",
        };

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testSameMethodInvokedBeforeAndAfterSafe() {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "     anotherMethod();\n"
                    + "     Map<String,Schema.SObjectType> types = Schema.getGlobalDescribe();\n"
                    + "     anotherMethod();\n"
                    + "   }\n"
                    + "   void anotherMethod() {\n"
                    + "      System.debug('hi');\n"
                    + "   }\n"
                    + "}\n",
        };

        assertNoViolation(RULE, sourceCode);
    }

    @Test
    public void testSameMethodInvokedBeforeAndAfterSafe_twoLevel() {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void foo() {\n"
                    + "     anotherMethod();\n"
                    + "     yetAnotherMethod();\n"
                    + "     anotherMethod();\n"
                    + "   }\n"
                    + "   void anotherMethod() {\n"
                    + "      notExpensive();\n"
                    + "   }\n"
                    + "   void notExpensive() {\n"
                    + "      System.debug('hi');\n"
                    + "   }\n"
                    + "   void yetAnotherMethod() {\n"
                    + "      expensiveInvoker();\n"
                    + "   }\n"
                    + "   void expensiveInvoker() {\n"
                    + "     Map<String,Schema.SObjectType> types = Schema.getGlobalDescribe();\n"
                    + "   }\n"
                    + "}\n",
        };

        assertNoViolation(RULE, sourceCode);
    }
}
