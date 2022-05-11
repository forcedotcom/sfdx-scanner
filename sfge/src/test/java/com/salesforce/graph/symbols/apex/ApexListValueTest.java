package com.salesforce.graph.symbols.apex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.ApexValueMatchers;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexListValueTest {
    private GraphTraversalSource g;
    private static final String CLASS_NAME = "MyClass";

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testUnresolvedUserMethodReturnApexListValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<String> l1 = MyOtherClass.someMethod();\n"
                        + "       List<String> l2 = new List<String>();\n"
                        + "       System.debug(l1.size());\n"
                        + "       System.debug(l1.isEmpty());\n"
                        + "       System.debug(l1.equals(l2));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // l1.size()
        assertInteger(visitor, 5, true, false);
        // l1.isEmpty()
        assertBoolean(visitor, 6, true, false);
        // l1.equals(l2)
        assertBoolean(visitor, 7, true, false);
    }

    @Test
    public void testUnresolvedUserMethodParameterApexListValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(List<String> l1) {\n"
                        + "       List<String> l2 = new List<String>();\n"
                        + "       System.debug(l1.size());\n"
                        + "       System.debug(l1.isEmpty());\n"
                        + "       System.debug(l1.equals(l2));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // l1.size()
        assertInteger(visitor, 4, true, false);
        // l1.isEmpty()
        assertBoolean(visitor, 5, true, false);
        // l1.equals(l2)
        assertBoolean(visitor, 6, true, false);
    }

    @Test
    public void testInlineInitializer() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(String jobId) {\n"
                    + "       List<User> users = new List<User>{(new User(Id = 'a', ProfileId = 'b'))};\n"
                    + "       System.debug(users);\n"
                    + "       System.debug(users.isEmpty());\n"
                    + "       System.debug(!users.isEmpty());\n"
                    + "       System.debug(users.size());\n"
                    + "       logUsers(users);\n"
                    + "    }\n"
                    + "    public static void logUsers(List<User> u) {\n"
                    + "       System.debug(u.isEmpty());\n"
                    + "       System.debug(!u.isEmpty());\n"
                    + "       System.debug(u.size());\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(7)));

        ApexBooleanValue isEmpty;
        ApexBooleanValue isNotEmpty;
        ApexIntegerValue size;

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.getValues(), hasSize(equalTo(1)));

        isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));

        isEmpty = visitor.getResult(4);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        isNotEmpty = visitor.getResult(5);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        size = visitor.getResult(6);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(1));
    }

    /** Verify that the list returned from a SOQL query is indeterminant */
    @Test
    public void testSoqlExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String jobId) {\n"
                        + "       List<MyObject__c> objects = [SELECT Id, Name, Field__c FROM MyObject__c];\n"
                        + "       System.debug(objects);\n"
                        + "       System.debug(objects.isEmpty());\n"
                        + "       System.debug(objects.size());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexListValue objects = (ApexListValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(objects.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(objects.isDeterminant(), equalTo(false));

        ApexBooleanValue isEmpty = (ApexBooleanValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(isEmpty.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(isEmpty.isDeterminant(), equalTo(false));
        MatcherAssert.assertThat(isEmpty.getValue().isPresent(), equalTo(false));

        ApexIntegerValue size = (ApexIntegerValue) visitor.getAllResults().get(2).get();
        MatcherAssert.assertThat(size.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(size.isDeterminant(), equalTo(false));
        MatcherAssert.assertThat(size.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testInlineInitializerWithMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		List<String> l1 = new List<String>{getValue1(), getValue2()};\n"
                        + "		System.debug(l1);\n"
                        + "		System.debug(l1.isEmpty());\n"
                        + "		System.debug(!l1.isEmpty());\n"
                        + "		System.debug(l1.size());\n"
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

        ApexListValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("value1", "value2"));

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
                        + "		List<String> l2 = new List<String>(l1);\n"
                        + "		System.debug(l2);\n"
                        + "		System.debug(l2.isEmpty());\n"
                        + "		System.debug(!l2.isEmpty());\n"
                        + "		System.debug(l2.size());\n"
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

        ApexListValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("value1", "value2"));

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
                        + "		List<String> l2 = new List<String>(s1);\n"
                        + "		System.debug(l2);\n"
                        + "		System.debug(l2.isEmpty());\n"
                        + "		System.debug(!l2.isEmpty());\n"
                        + "		System.debug(l2.size());\n"
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

        ApexListValue value = visitor.getResult(0);
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
    public void testInlineInitializerWithOtherListFromMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		List<String> l1 = new List<String>(getList());\n"
                        + "		System.debug(l1);\n"
                        + "		System.debug(l1.isEmpty());\n"
                        + "		System.debug(!l1.isEmpty());\n"
                        + "		System.debug(l1.size());\n"
                        + "	}\n"
                        + "	public static List<String> getList() {\n"
                        + "		return new List<String>{'value1', 'value2'};\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexListValue value = visitor.getResult(0);
        List<String> values =
                value.getValues().stream()
                        .map(v -> TestUtil.apexValueToString(v))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("value1", "value2"));

        ApexBooleanValue isEmpty = visitor.getResult(1);
        MatcherAssert.assertThat(isEmpty.getValue().get(), equalTo(false));

        ApexBooleanValue isNotEmpty = visitor.getResult(2);
        MatcherAssert.assertThat(isNotEmpty.getValue().get(), equalTo(true));

        ApexIntegerValue size = visitor.getResult(3);
        MatcherAssert.assertThat(size.getValue().get(), equalTo(2));
    }

    @Test
    public void testListInForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		System.debug(acc);\n"
                    + "		for (Account a : acc) {\n"
                    + "			System.debug(a);\n"
                    + "		}\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexSingleValue apexSingleValue;

        final ApexListValue listValue = visitor.getResult(0);
        assertThat(listValue.getValues(), hasSize(1));
        apexSingleValue = (ApexSingleValue) listValue.get(0);
        MatcherAssert.assertThat(apexSingleValue.getDefiningType().get(), equalTo("Account"));

        final ApexForLoopValue forLoopValue = visitor.getResult(1);
        assertThat(forLoopValue.getForLoopValues(), hasSize(equalTo(1)));
        apexSingleValue = (ApexSingleValue) forLoopValue.getForLoopValues().get(0);
        MatcherAssert.assertThat(apexSingleValue.getDefiningType().get(), equalTo("Account"));

        final Map<ApexValue<?>, ApexValue<?>> properties = apexSingleValue.getApexValueProperties();
        MatcherAssert.assertThat(properties, aMapWithSize(1));
        final ApexStringValue propertyKey = (ApexStringValue) properties.keySet().iterator().next();
        MatcherAssert.assertThat(propertyKey.getValue().get(), equalTo("Name"));
    }

    @Test
    @Disabled // TODO: Fix forloop's handling of individual values in list
    public void testCopyOfDeterminantList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		List<Account> input = new List<Account>(new Account(Name='Acme Inc.'));\n"
                        + "		executeInsert(input);\n"
                        + "    }\n"
                        + "    public void executeInsert(List<Account> input) {\n"
                        + "		List<Account> toInsert = new List<Account>();\n"
                        + "		for (Account acc: input) {\n"
                        + "			toInsert.add(acc);\n"
                        + "		}\n"
                        + "		System.debug(toInsert);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue copyListValue = visitor.getSingletonResult();
        final List<ApexValue<?>> listItems = copyListValue.getValues();
        MatcherAssert.assertThat(listItems, hasSize(1));

        final ApexSingleValue apexSingleValue = (ApexSingleValue) listItems.get(0);
        MatcherAssert.assertThat(apexSingleValue.getDefiningType().get(), equalTo("Account"));

        final Map<ApexValue<?>, ApexValue<?>> properties = apexSingleValue.getApexValueProperties();
        MatcherAssert.assertThat(properties, aMapWithSize(1));
        final ApexStringValue propertyKey = (ApexStringValue) properties.keySet().iterator().next();
        MatcherAssert.assertThat(propertyKey.getValue().get(), equalTo("Name"));
    }

    @Test
    public void testListWithApexSingleValues() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + " 	private static final List<SelectOption> options =\n"
                    + "   	new List<SelectOption> {\n"
                    + "       	new SelectOption('', '--Select One--'),\n"
                    + "           new SelectOption('opt1', 'Option 1')\n"
                    + "      	};\n"
                    + "   public static void doSomething() {\n"
                    + "       System.debug(options);\n"
                    + "   }\n"
                    + "}\n"
        };

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValues(), hasSize(equalTo(2)));

        for (int i = 0; i < value.getValues().size(); i++) {
            final ApexSingleValue apexSingleValue = (ApexSingleValue) value.getValues().get(i);
            MatcherAssert.assertThat(
                    "Index=" + i, apexSingleValue, ApexValueMatchers.typeEqualTo("SelectOption"));
        }
    }

    /** Check the list type and the type for the objects that it contains */
    @Test
    public void testListInitializedWithSoqlHasCorrectTypes() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(Id objId) {\n"
                        + "    	MyObject__c[] objsToDelete;\n"
                        + "    	objsToDelete = [SELECT ID FROM MyObject__c WHERE ID=:objId];\n"
                        + "    	System.debug(objsToDelete);\n"
                        + "    }\n"
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, ApexValueMatchers.typeEqualTo("List<MyObject__c>"));
        MatcherAssert.assertThat(
                value.getListType().get().getCanonicalType(), equalTo("MyObject__c"));
    }

    /** Get and remove have the same behavior if the list is indeterminant */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveIndeterminantListReturnsIndeterminantValue(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(List<MyObject__c> objs) {\n"
                        + "    	System.debug(objs);\n"
                        + "    	System.debug(objs."
                        + methodName
                        + "(0));\n"
                        + "    }\n"
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isDeterminant(), equalTo(false));

        final ApexSingleValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value, ApexValueMatchers.typeEqualTo("MyObject__c"));
    }

    /** Get and remove have the same behavior if the parameter is indeterminant */
    @ValueSource(strings = {"get", "remove"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testGetOrRemoveIndeterminantIndexReturnsIndeterminantValue(String methodName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(Integer i) {\n"
                        + "		List<MyObject__c> objs = new List<MyObject__c>();\n"
                        + "		objs.add(new MyObject__c());\n"
                        + "    	System.debug(objs);\n"
                        + "    	System.debug(objs."
                        + methodName
                        + "(i));\n"
                        + "    }\n"
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isDeterminant(), equalTo(true));
        // The method should not mutate the array
        MatcherAssert.assertThat(listValue.getValues(), hasSize(equalTo(1)));

        final ApexSingleValue value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value, ApexValueMatchers.typeEqualTo("MyObject__c"));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testClone(boolean shallowClone) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + " 		String s1 = 'Hello';\n"
                    + " 		String s2 = 'Goodbye';\n"
                    + " 		List<String> values = new List<String>{s1, s2};\n"
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

        final ApexListValue values = visitor.getResult(2);
        MatcherAssert.assertThat(values.getValues(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(values.getValues().get(0)), equalTo("Hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(values.getValues().get(1)), equalTo("Goodbye"));

        final ApexListValue valuesClone = visitor.getResult(3);
        MatcherAssert.assertThat(valuesClone.getValues(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(TestUtil.apexValueToString(valuesClone.get(0)), equalTo("Hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(valuesClone.get(1)), equalTo("Goodbye"));

        // The lists should be different
        MatcherAssert.assertThat(values != valuesClone, equalTo(true));

        // The values in the lists should be the same object if it's a shallow clone, different if a
        // deep clone
        MatcherAssert.assertThat(
                values.getValues().get(0) == valuesClone.getValues().get(0), equalTo(shallowClone));
        MatcherAssert.assertThat(
                values.getValues().get(1) == valuesClone.getValues().get(1), equalTo(shallowClone));
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
}
