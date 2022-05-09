package com.salesforce.graph.symbols;

import static com.salesforce.graph.symbols.apex.ApexStringValueFactory.UNRESOLVED_ARGUMENT_PREFIX;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.apex.AbstractApexMapValue;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexDecimalValue;
import com.salesforce.graph.symbols.apex.ApexDoubleValue;
import com.salesforce.graph.symbols.apex.ApexGlobalDescribeMapValue;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexMapValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ObjectPropertiesHolder;
import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.ApexValueMatchers;
import com.salesforce.matchers.TestRunnerMatcher;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testListAddVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "        List<Account> accounts = new List<Account>();\n"
                        + "        Account a1 = new Account();\n"
                        + "        a1.Name = 'Acme Inc. 1';\n"
                        + "        Account a2 = new Account();\n"
                        + "        a2.Name = 'Acme Inc. 2';\n"
                        + "        a2.Phone = '415-555-1212';\n"
                        + "        accounts.add(a1);\n"
                        + "        accounts.add(a2);\n"
                        + "        insert accounts;\n"
                        + "    }\n"
                        + "}\n";

        assertAccount1AndAccount2(sourceCode);
    }

    @Test
    public void testListAddMethodReturnValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       accounts.add(getAccount1());\n"
                        + "       accounts.add(getAccount2());\n"
                        + "       insert accounts;\n"
                        + "    }\n"
                        + "    public static Account getAccount1() {\n"
                        + "       Account a = new Account();\n"
                        + "       a.Name = 'Acme Inc. 1';\n"
                        + "       return a;\n"
                        + "    }\n"
                        + "    public static Account getAccount2() {\n"
                        + "       Account a = new Account();\n"
                        + "       a.Name = 'Acme Inc. 2';\n"
                        + "       a.Phone = '415-555-1212';\n"
                        + "       return a;\n"
                        + "    }\n"
                        + "}\n";

        assertAccount1AndAccount2(sourceCode);
    }

    @Test
    public void testListAddNewKeyValueObjectExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       accounts.add(new Account(Name = 'Acme Inc. 1'));\n"
                        + "       accounts.add(new Account(Name = 'Acme Inc. 2', Phone = '415-555-1212'));\n"
                        + "       insert accounts;\n"
                        + "    }\n"
                        + "}\n";

        assertAccount1AndAccount2(sourceCode);
    }

    @Test
    public void testListAddNewKeyValueObjectExpressionWithMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       accounts.add(new Account(Name = 'Acme Inc. 1'));\n"
                        + "       accounts.add(new Account(Name = 'Acme Inc. 2', Phone = getPhone()));\n"
                        + "       insert accounts;\n"
                        + "    }\n"
                        + "    public static String getPhone() {\n"
                        + "       return '415-555-1212';\n"
                        + "    }\n"
                        + "}\n";

        assertAccount1AndAccount2(sourceCode);
    }

    @Test
    public void testListAddString() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<String> names = new List<String>();\n"
                        + "       names.add('Acme Inc. 1');\n"
                        + "       names.add('Acme Inc. 2');\n"
                        + "       System.debug(names);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> apexValue;
        apexValue = visitor.getSingletonResult();

        ApexListValue apexListValue = (ApexListValue) apexValue;
        MatcherAssert.assertThat(apexListValue.getValues(), hasSize(equalTo(2)));
        apexValue = apexListValue.getValues().get(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Acme Inc. 1"));
        apexValue = apexListValue.getValues().get(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Acme Inc. 2"));
    }

    @Test
    public void testListAddAll() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<String> list1 = new List<String>();\n"
                        + "       list1.add('Item1');\n"
                        + "       list1.add('Item2');\n"
                        + "       List<String> list2 = new List<String>();\n"
                        + "       list2.add('Item3');\n"
                        + "       list2.add('Item4');\n"
                        + "       list1.addAll(list2);\n"
                        + "       System.debug(list1);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue apexListValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexListValue.getValues(), hasSize(equalTo(4)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexListValue.getValues().get(0)), equalTo("Item1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexListValue.getValues().get(1)), equalTo("Item2"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexListValue.getValues().get(2)), equalTo("Item3"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexListValue.getValues().get(3)), equalTo("Item4"));
    }

    @Test
    public void testCustomObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       MyObject__c obj = new MyObject__c();\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexSingleValue.getStatus(), equalTo(ValueStatus.INITIALIZED));
        MatcherAssert.assertThat(apexSingleValue.isDeterminant(), equalTo(true));
    }

    @Test
    public void testSoqlAssignedToList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       List<Account> accounts = [SELECT Id, Name from Account];\n"
                        + "       System.debug(accounts);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue apexListValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexListValue.getValues(), hasSize(equalTo(1)));

        ApexSoqlValue apexSoqlValue = (ApexSoqlValue) apexListValue.getValues().get(0);
        MatcherAssert.assertThat(
                apexSoqlValue.getDefiningType().get(), equalToIgnoringCase("Account"));

        TreeSet<String> fields = getImpliedProperties(apexSoqlValue);
        MatcherAssert.assertThat(fields, hasSize(equalTo(2)));
        MatcherAssert.assertThat(fields, containsInAnyOrder("Id", "Name"));
    }

    @Test
    public void testMapLiteralKey() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        values.put('key1', 'value1');\n"
                        + "        System.debug(values);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexMapValue apexMapValue = visitor.getSingletonResult();
        Map<ApexValue<?>, ApexValue<?>> mapValues = apexMapValue.getValues();
        MatcherAssert.assertThat(mapValues.entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry = mapValues.entrySet().iterator().next();

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getKey()), equalToIgnoringCase("key1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getValue()), equalToIgnoringCase("value1"));
    }

    /**
     * Apex Maps are case sensitive. This test ensures that keys that differ by case result in two
     * entries.
     */
    @Test
    public void testMapLiteralKeyCaseSensitive() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        values.put('key1', 'value1');\n"
                        + "        values.put('Key1', 'Value1');\n"
                        + "        System.debug(values);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexMapValue apexMapValue = visitor.getSingletonResult();
        Map<ApexValue<?>, ApexValue<?>> mapValues = apexMapValue.getValues();
        MatcherAssert.assertThat(mapValues.entrySet(), hasSize(equalTo(2)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry = mapValues.entrySet().iterator().next();

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getKey()), equalToIgnoringCase("key1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getValue()), equalToIgnoringCase("value1"));
    }

    /**
     * Apex Maps are case sensitive. This test ensures that keys that differ by case result in two
     * entries and that the two entries can be retrieved successfully.
     */
    @Test
    public void testMapLiteralKeyCaseSensitiveGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        values.put('key1', 'value1');\n"
                        + "        values.put('Key1', 'Value1');\n"
                        + "        String s;\n"
                        + "        s = values.get('key1');\n"
                        + "        System.debug(s);\n"
                        + "        s = values.get('Key1');\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexValue<?> apexValue;
        apexValue = visitor.getResult(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("value1"));

        apexValue = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Value1"));
    }

    @Test
    public void testMapVariableKey() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        String key = 'key1';\n"
                        + "        values.put(key, 'value1');\n"
                        + "        System.debug(values);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexMapValue apexMapValue = visitor.getSingletonResult();
        Map<ApexValue<?>, ApexValue<?>> mapValues = apexMapValue.getValues();
        MatcherAssert.assertThat(mapValues.entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry = mapValues.entrySet().iterator().next();

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getKey()), equalToIgnoringCase("key1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(entry.getValue()), equalToIgnoringCase("value1"));
    }

    @Test
    public void testAssignmentFromMapWithLiteralPutLiteralGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        values.put('key1', 'value1');\n"
                        + "        String s = values.get('key1');\n"
                        + "        s = values.get('key1');\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalToIgnoringCase("value1"));
    }

    @Test
    public void testAssignmentFromMapWithVariablePutLiteralGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        String key = 'key1';\n"
                        + "        values.put(key, 'value1');\n"
                        + "        String s;\n"
                        + "        s = values.get('key1');\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalToIgnoringCase("value1"));
    }

    @Test
    public void testAssignmentFromMapWithVariablePutVariableGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        String key = 'key1';\n"
                        + "        values.put(key, 'value1');\n"
                        + "        String s;\n"
                        + "        s = values.get(key);\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalToIgnoringCase("value1"));
    }

    @Test
    public void testAssignmentFromMapWithVariablePutLiteralGetVariableGet() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        Map<String, String> values = new Map<String, String>();\n"
                        + "        String key = 'key1';\n"
                        + "        values.put(key, 'value1');\n"
                        + "        String s;\n"
                        + "        s = values.get('key1');\n"
                        + "        System.debug(s);\n"
                        + "        s = values.get(key);\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexStringValue apexStringValue;

        apexStringValue = visitor.getResult(0);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalToIgnoringCase("value1"));

        apexStringValue = visitor.getResult(1);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalToIgnoringCase("value1"));
    }

    @Test
    public void testString() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("Hello"));
    }

    @Test
    public void testStringToLowerCase() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       System.debug(s);\n"
                    + "       s = s.toLowerCase();\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexValue<?> apexValue;
        apexValue = visitor.getResult(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Hello"));

        apexValue = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("hello"));
    }

    @Test
    public void testLiteralBoolean() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Boolean x = True;\n"
                    + "       Boolean y = False;\n"
                    + "       System.debug(x);\n"
                    + "       System.debug(y);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexBooleanValue apexBooleanValue;

        apexBooleanValue = visitor.getResult(0);
        MatcherAssert.assertThat(apexBooleanValue.getValue().get(), equalTo(true));

        apexBooleanValue = visitor.getResult(1);
        MatcherAssert.assertThat(apexBooleanValue.getValue().get(), equalTo(false));
    }

    @Test
    public void testStringSubStringBefore() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String s = 'Hello.There';\n"
                    + "       System.debug(s);\n"
                    + "       s = s.subStringBefore('.');\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexValue<?> apexValue;
        apexValue = visitor.getResult(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Hello.There"));

        apexValue = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), equalTo("Hello"));
    }

    @Test
    public void testClassRefExpressionTypeRefDeclaration() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = MyOtherClass.class.getName();\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("MyOtherClass"));
    }

    @Test
    public void testClassRefExpressionTypeRefMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       logSomething(MyOtherClass.class.getName());\n"
                        + "    }\n"
                        + "    public static void logSomething(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("MyOtherClass"));
    }

    @Test
    public void testGetGlobalDescribe() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Map<String, Schema.SObjectType> gd;\n"
                    + "       System.debug(gd);\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       System.debug(gd);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        AbstractApexMapValue apexMapValue;

        apexMapValue = visitor.getResult(0);
        MatcherAssert.assertThat(apexMapValue.getValueVertex().isPresent(), equalTo(false));
        MatcherAssert.assertThat(apexMapValue.getKeyType(), equalToIgnoringCase("String"));
        MatcherAssert.assertThat(
                apexMapValue.getValueType(), equalToIgnoringCase("Schema.SObjectType"));

        apexMapValue = visitor.getResult(1);
        MatcherAssert.assertThat(apexMapValue.getValueVertex().isPresent(), equalTo(true));
        MatcherAssert.assertThat(apexMapValue.getKeyType(), equalToIgnoringCase("String"));
        MatcherAssert.assertThat(
                apexMapValue.getValueType(), equalToIgnoringCase("Schema.SObjectType"));
    }

    @Test
    public void testGlobalDescribeInvokeGet() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                    + "       System.debug(gd);\n"
                    + "       Schema.SObjectType sObjType = gd.get('Foo__c');\n"
                    + "       System.debug(sObjType);\n"
                    + "       Schema.DescribeSObjectResult objDescribe = sObjType.getDescribe();\n"
                    + "       System.debug(objDescribe);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(3)));

        ApexGlobalDescribeMapValue apexMapValue = visitor.getResult(0);
        MatcherAssert.assertThat(apexMapValue.getValueVertex().isPresent(), equalTo(true));
        MatcherAssert.assertThat(apexMapValue.getKeyType(), equalToIgnoringCase("String"));
        MatcherAssert.assertThat(
                apexMapValue.getValueType(), equalToIgnoringCase("Schema.SObjectType"));

        SObjectType sObjectType = visitor.getResult(1);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalToIgnoringCase("Foo__c"));

        DescribeSObjectResult describeSObjectResult = visitor.getResult(2);
        MatcherAssert.assertThat(describeSObjectResult, not(nullValue()));
    }

    @Test
    // Copied from github.com/SalesforceFoundation/NPSP
    public void testDescribeSObjectResult() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeSObjectResult result = DescribeSingleton.getObjectDescribe(MyObject__c.SObjectType);\n"
                    + "       System.debug(result);\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       System.debug(objectDescribesByType);\n"
                    + "       fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   private static void fillMapsForObject(string objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       System.debug(objectName);\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       System.debug(gd);\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       System.debug(objDescribe);\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(5)));

        ApexValue<?> apexValue;
        ApexMapValue apexMapValue;
        DescribeSObjectResult describeSObjectResult;

        apexMapValue = visitor.getResult(0);
        MatcherAssert.assertThat(apexMapValue.isDeterminant(), equalTo(true));

        apexValue = visitor.getResult(1);
        MatcherAssert.assertThat(apexValue.isDeterminant(), equalTo(true));

        ApexGlobalDescribeMapValue apexGlobalDescribeMapValue = visitor.getResult(2);
        MatcherAssert.assertThat(apexGlobalDescribeMapValue.isDeterminant(), equalTo(true));

        describeSObjectResult = visitor.getResult(3);
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));

        describeSObjectResult = visitor.getResult(4);
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
    }

    @Test
    @Disabled
    // Copied from github.com/SalesforceFoundation/NPSP
    public void testCached() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       if (checkDelete()) {\n"
                    + "           Foo__c foo = new Foo__c();\n"
                    + "           delete foo;\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public Boolean checkDelete() {\n"
                    + "       return PermissionsSingleton.getInstance().canDelete(Foo__c.SObjectType);\n"
                    + "   }\n"
                    + "}",
            "public class PermissionsSingleton {\n"
                    + "   private static PermissionsSingleton singleton;\n"
                    + "   public static PermissionsSingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new PermissionsSingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "   }\n"
                    + "   public Boolean canDelete(SObjectType sObjectType) {\n"
                    + "       return DescribeSingleton.getObjectDescribe(sObjectType).isDeletable();\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       if (objectDescribesByType == null || !objectDescribesByType.containsKey(objType)) {\n"
                    + "           fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       }\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   static void fillMapsForObject(string objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       if (gd == null) {\n"
                    + "           gd = Schema.getGlobalDescribe();\n"
                    + "       }\n"
                    + "       if (gd.containsKey(objectName)) {\n"
                    + "           if (!objectDescribes.containsKey(objectName)) {\n"
                    + "               Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "               objectDescribes.put(objectName, objDescribe);\n"
                    + "               objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "           }\n"
                    + "       } else {\n"
                    + "           throw new SchemaDescribeException('Invalid object name \\'' + objectName + '\\'');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        ApexPathExpanderConfig apexPathExpanderConfig =
                ApexPathUtil.getFullConfiguredPathExpanderConfig();

        List<ApexPath> paths = TestUtil.getApexPaths(config, apexPathExpanderConfig, "foo");
        MatcherAssert.assertThat(paths, hasSize(equalTo(4)));
        List<ApexPath> pathsWithException =
                paths.stream().filter(p -> p.endsInException()).collect(Collectors.toList());
        MatcherAssert.assertThat(pathsWithException, hasSize(equalTo(2)));
        List<MethodCallExpressionVertex> methodCalls = new ArrayList<>();
        for (ApexPath path : paths) {
            PathVertexVisitor visitor =
                    new DefaultNoOpPathVertexVisitor() {
                        @Override
                        public boolean visit(
                                MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                            if (vertex.getDefiningType().equals("DescribeSingleton")
                                    && vertex.getBeginLine() > 17) {
                                methodCalls.add(vertex);
                            }
                            return true;
                        }
                    };
            TestRunner.get(g, sourceCode).withPathVertexVisitor(() -> visitor).walkPath();
        }
    }

    @Test
    public void testClassGetName() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.SObjectType s1 = MyClass.class.getName();\n"
                    + "       System.debug(s1);\n"
                    + "       Schema.SObjectType s2 = MyClass.InnerClass.class.getName();\n"
                    + "       System.debug(s2);\n"
                    + "   }\n"
                    + "   public class InnerClass {\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getResult(0)), equalTo("MyClass"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getResult(1)), equalTo("MyClass.InnerClass"));
    }

    @Test
    public void testSObjectMutatedInLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       List<Contact> c = Database.query('SELECT Id, Name FROM Contact');\n"
                    + "       for (Integer i=0; i<c.size(); i++) {\n"
                    + "           c[i].CustField__c = i;\n"
                    + "       }\n"
                    + "       System.debug(c);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSoqlValue apexSoqlValue = visitor.getSingletonResult();
        // TODO: Callers should have way to get what was originally queried and also what was added
        // in one call
        MatcherAssert.assertThat(
                getImpliedProperties(apexSoqlValue), containsInAnyOrder("Id", "Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        apexSoqlValue.getApexValueProperties().keySet().iterator().next()),
                equalTo("CustField__c"));
    }

    @Test
    public void testDatabaseQueryWithVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String strSoql = 'SELECT Id, ';\n"
                    + "       strSoql += 'Name '; \n"
                    + "       strSoql += 'FROM ' + ' Contact';\n"
                    + "       List<Contact> c = Database.query(strSoql);\n"
                    + "       for (Integer i=0; i<c.size(); i++) {\n"
                    + "           c[i].CustField__c = i;\n"
                    + "       }\n"
                    + "       System.debug(c);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSoqlValue apexSoqlValue = visitor.getSingletonResult();
        // TODO: Callers should have way to get what was originally queried and also what was added
        // in one call
        MatcherAssert.assertThat(
                getImpliedProperties(apexSoqlValue), containsInAnyOrder("Id", "Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        apexSoqlValue.getApexValueProperties().keySet().iterator().next()),
                equalTo("CustField__c"));
    }

    @Test
    public void testStringConcatenationAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       s += ' There';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There"));
    }

    /** TODO: Hack hack. ApexValueBuilder is choosing the first value. */
    @Test
    public void testTernaryDeclaration() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       Boolean b = true;\n"
                    + "       String sObject = b ? 'Account' : 'Contact';\n"
                    + "       String s = 'SELECT';\n"
                    + "       s += ' Id FROM ' + sObject + ' WHERE Id = :someId';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue),
                equalTo("SELECT Id FROM Account WHERE Id = :someId"));
    }

    @Test
    @Disabled // Need to support multiple levels of classes. This needs the DMLOptions class added
    public void testDMLOptions() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       Database.DMLOptions dml = new Database.DMLOptions();\n"
                    + "       dml.DuplicateRuleHeader.AllowSave = true;\n"
                    + "       Boolean b = dml.DuplicateRuleHeader.AllowSave;\n"
                    + "       System.debug(b);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
    }

    @Test
    public void testCast() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                    + "       System.debug(obj);\n"
                    + "       logSomething((Account)obj);\n"
                    + "   }\n"
                    + "   public static void logSomething(Account a) {\n"
                    + "       System.debug(a);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexSingleValue apexSingleValue;
        SObjectType sObjectType;

        apexSingleValue = visitor.getResult(0);
        sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(sObjectType.getCanonicalType(), equalTo("Schema.SObjectType"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        apexSingleValue = visitor.getResult(1);
        sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(sObjectType.getCanonicalType(), equalTo("Schema.SObjectType"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));
    }

    @Test
    public void testStringConcatenationMultipleAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       s += ' There.';\n"
                    + "       s += ' How' + ' are you?';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There. How are you?"));
    }

    @Test
    public void testStringConcatenationDeclaration() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello' + ' There';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There"));
    }

    @Test
    public void testMultipleStringConcatenation() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       s += ' There.' + ' How are you?';\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There. How are you?"));
    }

    @Test
    public void testStringAssignmentConcatenationWithMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello';\n"
                    + "       s += ' There.' + getString();\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "   public static String getString() {\n"
                    + "       return ' How are you?';\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There. How are you?"));
    }

    @Test
    public void testStringDeclarationConcatenationWithMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       String s = 'Hello' + ' There. ' + getString1();\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "   public static String getString1() {\n"
                    + "       return 'How ' + getString2();\n"
                    + "   }\n"
                    + "   public static String getString2() {\n"
                    + "       return 'are you?';\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(apexStringValue), equalTo("Hello There. How are you?"));
    }

    @Test
    public void testBooleanInsideOfIf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(Id myId) {\n"
                        + "       Boolean canDelete = Schema.getGlobalDescribe().get('MyObject__c').getDescribe().isDeletable();\n"
                        + "       if (canDelete) {\n"
                        + "           System.debug(canDelete);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        List<TestRunner.Result<SystemDebugAccumulator>> filteredResults =
                results.stream()
                        .filter(r -> r.getPath().lastVertex().getBeginLine().equals(5))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(filteredResults, hasSize(equalTo(1)));
        SystemDebugAccumulator visitor = filteredResults.get(0).getVisitor();

        ApexBooleanValue apexBooleanValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexBooleanValue.getStatus(), equalTo(ValueStatus.INDETERMINANT));
        MatcherAssert.assertThat(apexBooleanValue.getValue().isPresent(), equalTo(false));
        MethodCallExpressionVertex methodCallExpression =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(methodCallExpression.getMethodName(), equalTo("isDeletable"));
    }

    @Test
    public void testBooleanProperty() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public Boolean aBoolProp { get; set; }\n"
                        + "   public void doSomething() {\n"
                        + "       System.debug(aBoolProp);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue apexBooleanValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexBooleanValue.getStatus(), equalTo(ValueStatus.UNINITIALIZED));
        MatcherAssert.assertThat(apexBooleanValue.isNull(), equalTo(true));
    }

    @Test
    public void testCustomSettings() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       EmailSettings__c emailSettings = EmailSettings__c.getOrgDefaults();\n"
                        + "       System.debug(emailSettings);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexCustomValue apexCustomValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexCustomValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                apexCustomValue.getTypeVertex().get().getCanonicalType(),
                Matchers.equalTo("EmailSettings__c"));
    }

    @Test
    public void testStringFormatSimpleString() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       String s = String.format('Hello {0}, how are you', new String[] {'there'});\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello there, how are you"));
    }

    @Test
    public void testStringFormatSimpleInteger() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       String s = String.format('Hello {0}, how are you', new Integer[] {10});\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello 10, how are you"));
    }

    @Test
    public void testStringFormatSimpleVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       String v = 'there';\n"
                        + "       String s = String.format('Hello {0}, how are you', new String[] {v});\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello there, how are you"));
    }

    @Test
    public void testStringFormatUnresolvedVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       String s = String.format('Hello {0}, how are you', new String[] {v});\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result,
                TestRunnerMatcher.hasValue(
                        "Hello " + UNRESOLVED_ARGUMENT_PREFIX + 0 + ", how are you"));
    }

    @Test
    public void testUnresolvedStringVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(String x) {\n"
                        + "       String s = x;\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testDecimalVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       Decimal x = 10.0;\n"
                        + "       System.debug(x);\n"
                        + "       System.debug(x.intValue());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexDecimalValue decimalValue = visitor.getResult(0);
        MatcherAssert.assertThat(decimalValue.getValue().get(), equalTo(new BigDecimal("10.0")));

        ApexIntegerValue integerValue = visitor.getResult(1);
        MatcherAssert.assertThat(integerValue.getValue().get(), equalTo(Integer.valueOf(10)));
    }

    @Test
    public void testUnresolvedDecimalVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(Decimal x) {\n"
                        + "       System.debug(x);\n"
                        + "       System.debug(x.intValue());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexDecimalValue decimalValue = visitor.getResult(0);
        MatcherAssert.assertThat(decimalValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(decimalValue.getValue().isPresent(), equalTo(false));

        ApexIntegerValue integerValue = visitor.getResult(1);
        MatcherAssert.assertThat(integerValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(integerValue.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testDoubleVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       Double x = 10.0d;\n"
                        + "       System.debug(x);\n"
                        + "       System.debug(x.intValue());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexDoubleValue DoubleValue = visitor.getResult(0);
        MatcherAssert.assertThat(DoubleValue.getValue().get(), equalTo(Double.valueOf("10.0")));

        ApexIntegerValue integerValue = visitor.getResult(1);
        MatcherAssert.assertThat(integerValue.getValue().get(), equalTo(Integer.valueOf(10)));
    }

    @Test
    public void testUnresolvedDoubleVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(Double x) {\n"
                        + "       System.debug(x);\n"
                        + "       System.debug(x.intValue());\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexDoubleValue DoubleValue = visitor.getResult(0);
        MatcherAssert.assertThat(DoubleValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(DoubleValue.getValue().isPresent(), equalTo(false));

        ApexIntegerValue integerValue = visitor.getResult(1);
        MatcherAssert.assertThat(integerValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(integerValue.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testNullValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       MyObject__c obj = new MyObject__c(Field1__c = null);\n"
                        + "       System.debug(obj);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                singleValue.getApexValueProperties().entrySet(), hasSize(equalTo(1)));
        ApexValue<?> value = singleValue.getApexValue("Field1__c").orElse(null);
        MatcherAssert.assertThat(value.isNull(), equalTo(true));
    }

    @Test
    public void testNullTernaryValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething(MyClass c) {\n"
                        + "       MyObject__c obj = new MyObject__c(Field1__c = c.reason == null ? null : c.reason.value);\n"
                        + "       System.debug(obj);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                singleValue.getApexValueProperties().entrySet(), hasSize(equalTo(1)));
        ApexValue<?> value =
                singleValue
                        .getApexValue(ObjectPropertiesHolder.TODO_TERNARY_KEY_VALUE_PREFIX + "0")
                        .orElse(null);
        MatcherAssert.assertThat(value.isNull(), equalTo(true));
    }

    public static Stream<Arguments> testVariablesInitializedToCorrectStatus() {
        List<Arguments> arguments = new ArrayList<>();

        List<Arguments> argumentsWithValues =
                Arrays.asList(
                        Arguments.of("Decimal", "= 10.0", ValueStatus.INITIALIZED, false),
                        Arguments.of("Double", "= 10.0", ValueStatus.INITIALIZED, false),
                        Arguments.of("Integer", " = 10", ValueStatus.INITIALIZED, false),
                        Arguments.of("Long", " = 20", ValueStatus.INITIALIZED, false),
                        Arguments.of("String", " = 'Hello'", ValueStatus.INITIALIZED, false),
                        Arguments.of(
                                "MyObject__c",
                                " = new MyObject__c()",
                                ValueStatus.INITIALIZED,
                                false));

        for (Arguments args : argumentsWithValues) {
            arguments.add(args);
            // Add additional use cases
            // Variable does not have an assignment, it should be UNINITIALIZED and null
            arguments.add(Arguments.of(args.get()[0], "", ValueStatus.UNINITIALIZED, true));
            // Variable has an assignment, it should be INITIALIZED and null
            arguments.add(Arguments.of(args.get()[0], "=  null", ValueStatus.INITIALIZED, true));
            // Variable is assigned to a method that doesn't resolve. It should be INDETERMINANT and
            // non-null
            arguments.add(
                    Arguments.of(
                            args.get()[0],
                            "=  nonExistentMethod()",
                            ValueStatus.INDETERMINANT,
                            false));
        }

        return arguments.stream();
    }

    /** Ensure that variables are initialized to the correct status */
    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: type=({0}):initializer=({1}):expectedStatus=({2}):isNull=({3})")
    public void testVariablesInitializedToCorrectStatus(
            String type, String initializer, ValueStatus expectedStatus, boolean isNull) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       "
                        + type
                        + " v "
                        + initializer
                        + ";\n"
                        + "       System.debug(v);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getStatus(), equalTo(expectedStatus));
        MatcherAssert.assertThat(
                value.isIndeterminant(), equalTo(expectedStatus.equals(ValueStatus.INDETERMINANT)));
        MatcherAssert.assertThat(
                value.isDeterminant(), equalTo(!expectedStatus.equals(ValueStatus.INDETERMINANT)));
        MatcherAssert.assertThat(value.isNull(), equalTo(isNull));
    }

    @Test
    public void testArrayLoadExpressionFromIndeterminantList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(Id objId) {\n"
                        + "    	MyObject__c[] objsToDelete;\n"
                        + "    	objsToDelete = [SELECT ID FROM MyObject__c WHERE ID=:objId];\n"
                        + "    	System.debug(objsToDelete);\n"
                        + "    	System.debug(objsToDelete[0]);\n"
                        + "    }\n"
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        final ApexListValue soqlResult = visitor.getResult(0);
        MatcherAssert.assertThat(soqlResult.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(soqlResult, ApexValueMatchers.typeEqualTo("List<MyObject__c>"));

        final ApexSingleValue apexSingleValue = visitor.getResult(1);
        MatcherAssert.assertThat(apexSingleValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(apexSingleValue, ApexValueMatchers.typeEqualTo("MyObject__c"));
    }

    @Test
    public void testArrayLoadExpressionFromDeterminantList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething(Id objId) {\n"
                        + "    	String[] strings = new String[]{'value1', 'value2'};\n"
                        + "    	System.debug(strings);\n"
                        + "    	System.debug(strings[0]);\n"
                        + "    	System.debug(strings[1]);\n"
                        + "    }\n"
                        + "}\n";

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        final ApexListValue strings = visitor.getResult(0);
        MatcherAssert.assertThat(strings.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(strings, ApexValueMatchers.typeEqualTo("List<String>"));

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getResult(1)), equalTo("value1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getResult(2)), equalTo("value2"));
    }

    private void assertAccount1AndAccount2(String sourceCode) {
        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "foo");

        List<ApexValue<?>> apexValues = new ArrayList<>();

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        DefaultNoOpPathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        apexValues.add(symbols.getApexValue("accounts").orElse(null));
                        return true;
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        MatcherAssert.assertThat(apexValues, hasSize(equalTo(1)));
        ApexValue<?> apexValue = apexValues.get(0);
        ApexListValue apexListValue = (ApexListValue) apexValue;
        List<ApexValue<?>> values = apexListValue.getValues();
        MatcherAssert.assertThat(values, hasSize(equalTo(2)));

        ApexSingleValue apexSingleValue;
        ApexStringValue value;
        Map<ApexValue<?>, ApexValue<?>> properties;

        // First value has Name set
        apexValue = values.get(0);
        apexSingleValue = (ApexSingleValue) apexValue;
        properties = apexSingleValue.getApexValueProperties();
        MatcherAssert.assertThat(properties.entrySet(), hasSize(equalTo(1)));
        value = (ApexStringValue) apexSingleValue.getApexValue("name").orElse(null);
        MatcherAssert.assertThat(value, not(nullValue()));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value), equalToIgnoringCase("Acme Inc. 1"));

        // Second value has Name and Phone set
        apexValue = values.get(1);
        apexSingleValue = (ApexSingleValue) apexValue;
        properties = apexSingleValue.getApexValueProperties();
        MatcherAssert.assertThat(properties.entrySet(), hasSize(equalTo(2)));
        value = (ApexStringValue) apexSingleValue.getApexValue("name").orElse(null);
        MatcherAssert.assertThat(value, not(nullValue()));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value), equalToIgnoringCase("Acme Inc. 2"));
        value = (ApexStringValue) apexSingleValue.getApexValue("phone").orElse(null);
        MatcherAssert.assertThat(value, not(nullValue()));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value), equalToIgnoringCase("415-555-1212"));
    }

    private TreeSet<String> getImpliedProperties(ApexSoqlValue value) {
        final Set<SoqlQueryInfo> processedQueries = value.getProcessedQueries();
        MatcherAssert.assertThat(processedQueries, hasSize(1));
        return processedQueries.iterator().next().getFields();
    }
}
