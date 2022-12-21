package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApexSetValueTest {
    private GraphTraversalSource g;
    private static final String CLASS_NAME = "MyClass";

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testUnresolvedUserMethodReturnApexSetValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       Set<String> s1 = MyOtherClass.someMethod();\n"
                        + "       Set<String> s2 = new Set<String>();\n"
                        + "       System.debug(s1.size());\n"
                        + "       System.debug(s1.isEmpty());\n"
                        + "       System.debug(s1.equals(s2));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // s1.size()
        assertInteger(visitor, 5, true, false);
        // s1.isEmpty()
        assertBoolean(visitor, 6, true, false);
        // s1.equals(s2)
        assertBoolean(visitor, 7, true, false);
    }

    @Test
    public void testUnresolvedUserMethodParameterApexSetValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Set<String> s1) {\n"
                        + "       Set<String> s2 = new Set<String>();\n"
                        + "       System.debug(s1.size());\n"
                        + "       System.debug(s1.isEmpty());\n"
                        + "       System.debug(s1.equals(s2));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // s1.size()
        assertInteger(visitor, 4, true, false);
        // s1.isEmpty()
        assertBoolean(visitor, 5, true, false);
        // s1.equals(s2)
        assertBoolean(visitor, 6, true, false);
    }

    @Test
    public void testInlineInitializer() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(String jobId) {\n"
                    + "       Set<User> users = new Set<User>{(new User(Id = 'a', ProfileId = 'b'))};\n"
                    + "       System.debug(users.isEmpty());\n"
                    + "       System.debug(!users.isEmpty());\n"
                    + "       System.debug(users.size());\n"
                    + "       logUsers(users);\n"
                    + "    }\n"
                    + "    public static void logUsers(Set<User> u) {\n"
                    + "       System.debug(u.isEmpty());\n"
                    + "       System.debug(!u.isEmpty());\n"
                    + "       System.debug(u.size());\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(6)));

        ApexBooleanValue isEmpty;
        ApexBooleanValue isNotEmpty;
        ApexIntegerValue size;

        isEmpty = (ApexBooleanValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        isNotEmpty = (ApexBooleanValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        size = (ApexIntegerValue) visitor.getAllResults().get(2).get();
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));

        isEmpty = (ApexBooleanValue) visitor.getAllResults().get(3).get();
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        isNotEmpty = (ApexBooleanValue) visitor.getAllResults().get(4).get();
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        size = (ApexIntegerValue) visitor.getAllResults().get(5).get();
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    @Test
    public void testInlineInitializerWithMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Set<String> s1 = new Set<String>{getValue1(), getValue2()};\n"
                        + "		System.debug(s1);\n"
                        + "		System.debug(s1.isEmpty());\n"
                        + "		System.debug(!s1.isEmpty());\n"
                        + "		System.debug(s1.size());\n"
                        + "	}\n"
                        + "	public static string getValue1() {\n"
                        + "		return 'value1';\n"
                        + "	}\n"
                        + "	public static string getValue2() {\n"
                        + "		return 'value2';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexSetValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, containsInAnyOrder("value1", "value2"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testInlineInitializerWithOtherSet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Set<String> s1 = new Set<String>{'value1', 'value2'};\n"
                        + "		Set<String> s2 = new Set<String>(s1);\n"
                        + "		System.debug(s2);\n"
                        + "		System.debug(s2.isEmpty());\n"
                        + "		System.debug(!s2.isEmpty());\n"
                        + "		System.debug(s2.size());\n"
                        + "	}\n"
                        + "	public static string getValue1() {\n"
                        + "		return 'value1';\n"
                        + "	}\n"
                        + "	public static string getValue2() {\n"
                        + "		return 'value2';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexSetValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        // The ordering of a set is not guaranteed
        MatcherAssert.assertThat(values, containsInAnyOrder("value1", "value2"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testInlineInitializerWithOtherList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		List<String> l1 = new List<String>{'value1', 'value2'};\n"
                        + "		Set<String> s2 = new Set<String>(l1);\n"
                        + "		System.debug(s2);\n"
                        + "		System.debug(s2.isEmpty());\n"
                        + "		System.debug(!s2.isEmpty());\n"
                        + "		System.debug(s2.size());\n"
                        + "	}\n"
                        + "	public static string getValue1() {\n"
                        + "		return 'value1';\n"
                        + "	}\n"
                        + "	public static string getValue2() {\n"
                        + "		return 'value2';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexSetValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, containsInAnyOrder("value1", "value2"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testInlineInitializerWithOtherSetFromMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		Set<String> s1 = new Set<String>(getSet());\n"
                        + "		System.debug(s1);\n"
                        + "		System.debug(s1.isEmpty());\n"
                        + "		System.debug(!s1.isEmpty());\n"
                        + "		System.debug(s1.size());\n"
                        + "	}\n"
                        + "	public static Set<String> getSet() {\n"
                        + "		return new Set<String>{'value1', 'value2'};\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexSetValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, containsInAnyOrder("value1", "value2"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testClone() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + " 		String s1 = 'Hello';\n"
                    + " 		String s2 = 'Goodbye';\n"
                    + " 		Set<String> values = new Set<String>{s1, s2};\n"
                    + "       System.debug(s1);\n"
                    + "       System.debug(s2);\n"
                    + "       System.debug(values);\n"
                    + "       System.debug(values.clone());\n"
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

        final ApexSetValue values = visitor.getResult(2);
        MatcherAssert.assertThat(values.getValues(), hasSize(equalTo(2)));
        // Sort the values to guarantee consistent ordering
        final List<String> sortedValues =
                values.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .sorted()
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(sortedValues, contains("Goodbye", "Hello"));

        final ApexSetValue valuesClone = visitor.getResult(3);
        final List<String> sortedValuesClone =
                valuesClone.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .sorted()
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(sortedValuesClone, contains("Goodbye", "Hello"));

        // The sets should be different
        MatcherAssert.assertThat(values != valuesClone, equalTo(true));

        // The values in the sets should be the same object
        MatcherAssert.assertThat(sortedValues.get(0) == sortedValuesClone.get(0), equalTo(true));
        MatcherAssert.assertThat(sortedValues.get(1) == sortedValuesClone.get(1), equalTo(true));
    }

    void assertInteger(
            SystemDebugAccumulator visitor,
            Integer lineNumber,
            boolean isIndeterminant,
            boolean isPresent) {
        String className = CLASS_NAME;
        MatcherAssert.assertThat(visitor.getResults(className, lineNumber), hasSize(equalTo(1)));
        ApexIntegerValue value =
                (ApexIntegerValue) visitor.getResults(className, lineNumber).get(0).get();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(isIndeterminant));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(isPresent));
    }

    void assertBoolean(
            SystemDebugAccumulator visitor,
            Integer lineNumber,
            boolean isIndeterminant,
            boolean isPresent) {
        String className = CLASS_NAME;
        MatcherAssert.assertThat(visitor.getResults(className, lineNumber), hasSize(equalTo(1)));
        ApexBooleanValue value =
                (ApexBooleanValue) visitor.getResults(className, lineNumber).get(0).get();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(isIndeterminant));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(isPresent));
    }

    @Test
    public void testForEachWithSet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       Set<String> fieldsToCheck = new Set<String>{'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           System.debug(fieldToCheck);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values.isEmpty(), equalTo(false));
        MatcherAssert.assertThat(values, containsInAnyOrder("Name", "Phone"));
    }

    @Test
    public void testStdMethodCallOnForLoopVariableWithSet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		Set<Schema.SObjectField> fields = new Set<Schema.SObjectFields>{Schema.Account.fields.Name,Schema.Account.fields.Phone};\n"
                        + "		for (Schema.SObjectField myField: fields) {\n"
                        + "			System.debug(myField.getDescribe());\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> fieldNames =
                value.getForLoopValues().stream()
                        .map(
                                item ->
                                        TestUtil.apexValueToString(
                                                ((DescribeFieldResult) item).getFieldName()))
                        .collect(Collectors.toList());

        MatcherAssert.assertThat(fieldNames, containsInAnyOrder("Name", "Phone"));
    }

    @Test
    @Disabled // TODO: Handle method invocations on ApexClassInstanceValue
    public void testMethodCallOnForLoopVariableWithSet() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething() {\n"
                    + "		Set<Bean> beans = new Set<Bean>{new Bean('hi'),new Bean('hello')};\n"
                    + "		for (Bean bean: beans) {\n"
                    + "			String myValue = bean.getValue();\n"
                    + "			System.debug(myValue);\n"
                    + "		}\n"
                    + "	}\n"
                    + "}\n",
            "public class Bean {\n"
                    + "private String value;\n"
                    + "public Bean(String val1) {\n"
                    + "	this.value = val1;\n"
                    + "}\n"
                    + "public String getValue() {\n"
                    + "	return this.value;\n"
                    + "}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> valueList =
                value.getForLoopValues().stream()
                        .map(item -> TestUtil.apexValueToString(item))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, containsInAnyOrder("hi", "hello"));
    }
}
