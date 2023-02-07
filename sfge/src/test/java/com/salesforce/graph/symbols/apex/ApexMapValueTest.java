package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexMapValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testJSONDeserialize() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String jobId) {\n"
                        + "       MyCustomSetting__c c = MyCustomSetting__c.getOrgDefaults();\n"
                        + "       Map<String, String> status = (Map<String, String>) JSON.deserialize(c.My_Status__c, Map<String, String>.class);\n"
                        + "       System.debug(status);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexMapValue status = visitor.getSingletonResult();
        MatcherAssert.assertThat(status.isDeterminant(), equalTo(true));
    }

    @Test
    public void testInlineInitializer() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Map<String,User> m1 = new Map<String,User>{'a'=>new User(Id = 'a', ProfileId= 'b'), 'b'=>new User(Id='c', ProfileId='d')};\n"
                        + "		System.debug(m1.isEmpty());\n"
                        + "		System.debug(!m1.isEmpty());\n"
                        + "		System.debug(m1.size());\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexBooleanValue isEmpty = visitor.getResult(0);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(2);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testInlineInitializerIndeterminantValues() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething(String key, String value) {\n"
                        + "		Map<String,String> m1 = new Map<String,String>{key => value};\n"
                        + "		System.debug(m1.isEmpty());\n"
                        + "		System.debug(!m1.isEmpty());\n"
                        + "		System.debug(m1.size());\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexBooleanValue isEmpty = visitor.getResult(0);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(2);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    @Test
    public void testInlineInitializerWithMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Map<String,User> m1 = new Map<String,String>{getKey() => getValue()};\n"
                        + "		System.debug(m1);\n"
                        + "		System.debug(m1.isEmpty());\n"
                        + "		System.debug(!m1.isEmpty());\n"
                        + "		System.debug(m1.size());\n"
                        + "	}\n"
                        + "	public static string getKey() {\n"
                        + "		return 'key1';\n"
                        + "	}\n"
                        + "	public static string getValue() {\n"
                        + "		return 'value1';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexMapValue value = visitor.getResult(0);
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                (Map.Entry<ApexValue<?>, ApexValue<?>>) value.getValues().entrySet().toArray()[0];
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("key1"));
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("value1"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    @Test
    public void testInlineInitializerWithOtherMap() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Map<String,User> m1 = new Map<String,String>{getKey() => getValue()};\n"
                        + "		Map<String,User> m2 = new Map<String,String>(m1);\n"
                        + "		System.debug(m2);\n"
                        + "		System.debug(m2.isEmpty());\n"
                        + "		System.debug(!m2.isEmpty());\n"
                        + "		System.debug(m2.size());\n"
                        + "	}\n"
                        + "	public static string getKey() {\n"
                        + "		return 'key1';\n"
                        + "	}\n"
                        + "	public static string getValue() {\n"
                        + "		return 'value1';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexMapValue value = visitor.getResult(0);
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                (Map.Entry<ApexValue<?>, ApexValue<?>>) value.getValues().entrySet().toArray()[0];
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("key1"));
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("value1"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    @Test
    public void testInlineInitializerWithOtherMapFromMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Map<String,User> m1 = new Map<String,String>(getMap());\n"
                        + "		System.debug(m1);\n"
                        + "		System.debug(m1.isEmpty());\n"
                        + "		System.debug(!m1.isEmpty());\n"
                        + "		System.debug(m1.size());\n"
                        + "	}\n"
                        + "	public static Map<String,String> getMap() {\n"
                        + "		return new Map<String,String>{'key1' => 'value1'};\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexMapValue value = visitor.getResult(0);
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                (Map.Entry<ApexValue<?>, ApexValue<?>>) value.getValues().entrySet().toArray()[0];
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("key1"));
        MatcherAssert.assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("value1"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    @Test
    public void testValueOfIndeterminantMapIsIndeterminant() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Map<String,My_Custom_Setting__c> m1 = My_Custom_Setting__c.getAll();\n"
                        + "		System.debug(m1);\n"
                        + "		System.debug(m1.get('hello'));\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexMapValue value1 = visitor.getResult(0);
        MatcherAssert.assertThat(value1.isIndeterminant(), equalTo(true));

        ApexSingleValue value2 = visitor.getResult(1);
        MatcherAssert.assertThat(value2.isIndeterminant(), equalTo(true));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testClone(boolean shallowClone) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + " 		String s1 = 'Hello';\n"
                    + " 		String s2 = 'Goodbye';\n"
                    + " 		Map<String, String> values = new Map<String, String>{s1 => s2};\n"
                    + "       System.debug(s1);\n"
                    + "       System.debug(s2);\n"
                    + "       System.debug(values);\n"
                    + "       System.debug(values."
                    + (shallowClone ? "clone()" : "deepClone()")
                    + ");\n"
                    + "   }\n"
                    + "}\n"
        };

        final TestRunner.Result<SystemDebugAccumulator> result =
                TestRunner.get(g, sourceCode)
                        // Override the default symbol provider which normally uses a
                        // CloningSymbolProvider. This allows the test to
                        // use reference equality
                        .withSymbolProviderFunction(
                                (symbolVisitor) -> symbolVisitor.getSymbolProvider())
                        .walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        // Sanity check the values
        final ApexStringValue s1 = visitor.getResult(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(s1), equalTo("Hello"));
        final ApexStringValue s2 = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(s2), equalTo("Goodbye"));

        final ApexMapValue values = visitor.getResult(2);
        MatcherAssert.assertThat(values.getValues().entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> valuesEntry =
                (Map.Entry<ApexValue<?>, ApexValue<?>>) values.getValues().entrySet().toArray()[0];
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(valuesEntry.getKey()), equalTo("Hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(valuesEntry.getValue()), equalTo("Goodbye"));

        final ApexMapValue valuesClone = visitor.getResult(3);
        MatcherAssert.assertThat(valuesClone.getValues().entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> valuesCloneEntry =
                (Map.Entry<ApexValue<?>, ApexValue<?>>)
                        valuesClone.getValues().entrySet().toArray()[0];
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(valuesCloneEntry.getKey()), equalTo("Hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(valuesCloneEntry.getValue()), equalTo("Goodbye"));

        // The maps should be different
        MatcherAssert.assertThat(values != valuesClone, equalTo(true));

        // The keys and values in the maps should be the same object if it's a shallow clone,
        // different if a deep clone
        MatcherAssert.assertThat(
                valuesEntry.getKey() == valuesCloneEntry.getKey(), equalTo(shallowClone));
        MatcherAssert.assertThat(
                valuesEntry.getValue() == valuesCloneEntry.getValue(), equalTo(shallowClone));
    }

    @Disabled // TODO: Handle nested Maps and invocations of get() on the results of a get() on a
    // nested Map.
    @Test
    public void testNestedMap() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static String doSomething() {\n"
                    + "        String output = RecordTypeUtil.getAccountRecordTypeID();\n"
                    + "       System.debug(output);\n"
                    + "    }\n"
                    + "}",
            "public class RecordTypeUtil {\n"
                    + "    private static Map<String, Map<String, Id>> mapRecordTypes = new Map<String, Map<String, Id>>();\n"
                    + "    public static String getAccountRecordTypeID() {\n"
                    + "        String recTypeId = getRecordTypes('Account').get('Name');\n"
                    + "        return recTypeId;\n"
                    + "    }\n"
                    + "\n"
                    + "    public static Map<String, Id> getRecordTypes(String objectName) {\n"
                    + "       if (!mapRecordTypes.contains(objectName)) {\n"
                    + "           mapRecordTypes.put(objectName, 'hi');\n"
                    + "       }\n"
                    + "        return mapRecordTypes.get(objectName);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        ApexStringValue apexValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), Matchers.equalTo("hi"));
    }
}
