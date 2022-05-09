package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.StringUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JSONDeserializeFactoryTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testJSONDeserializeClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	InnerClass c = (MyOtherClass)JSON.deserialize(asJson, MyOtherClass.class);\n"
                    + "   	System.debug(c);\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n" + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("MyOtherClass"));
    }

    @Test
    public void testJSONDeserializeInnerClassSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "   }\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	InnerClass c = (InnerClass)JSON.deserialize(asJson, InnerClass.class);\n"
                    + "   	System.debug(c);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("MyClass.InnerClass"));
    }

    public static Stream<Arguments> testJSONDeserializeInnerClassComplex() {
        return Stream.of(Arguments.of(""), Arguments.of("MyClass."));
    }

    /**
     * Verify that class inner class resolution works with and without qualification of the outer
     * class.
     */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testJSONDeserializeInnerClassComplex(String prefix) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass1 {\n"
                    + "   }\n"
                    + "	public class InnerClass2 {\n"
                    + "   	public static "
                    + prefix
                    + "InnerClass1 getInner1(String asJson) {\n"
                    + "   		return ("
                    + prefix
                    + "InnerClass1)JSON.deserialize(asJson, "
                    + prefix
                    + "InnerClass1.class);\n"
                    + "   	}\n"
                    + "   }\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	"
                    + prefix
                    + "InnerClass1 c = "
                    + prefix
                    + "InnerClass2.getInner1(asJson);\n"
                    + "   	System.debug(c);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo("MyClass.InnerClass1"));
    }

    @Test
    public void testStandardObject() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	Account a = (Account)JSON.deserialize(asJson, Account.class);\n"
                    + "   	a.put('Name', 'Hello');\n"
                    + "   	System.debug(a);\n"
                    + "   	System.debug(a.Name);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexSingleValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                value.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(TestUtil.apexValueToString(name), equalTo("Hello"));
    }

    @Test
    public void testCustomObject() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	MyObject__c o = (MyObject__c)JSON.deserialize(asJson, MyObject__c.class);\n"
                    + "   	o.put('Name', 'Hello');\n"
                    + "   	System.debug(o);\n"
                    + "   	System.debug(o.Name);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexSingleValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                value.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(TestUtil.apexValueToString(name), equalTo("Hello"));
    }

    @Test
    public void testCustomSetting() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	MyObject__c o = (MyObject__c)JSON.deserialize(asJson, MyObject__c.class);\n"
                    + "   	o.put('Name', 'Hello');\n"
                    + "   	System.debug(o);\n"
                    + "   	System.debug(o.Name);\n"
                    + "   }\n"
                    + "   public static void otherMethod() {\n"
                    +
                    // Triggers MetadataInfo to recognize this as a CustomSetting
                    "   	MyObject__c.getInstance();\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexCustomValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                value.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(TestUtil.apexValueToString(name), equalTo("Hello"));
    }

    public static Stream<Arguments> testTypesBuiltByBuilder() {
        return Stream.of(
                Arguments.of("Boolean", ApexBooleanValue.class),
                Arguments.of("Decimal", ApexDecimalValue.class),
                Arguments.of("Double", ApexDoubleValue.class),
                Arguments.of("Id", ApexIdValue.class),
                Arguments.of("Integer", ApexIntegerValue.class),
                Arguments.of("Long", ApexLongValue.class),
                Arguments.of("List<String>", ApexListValue.class),
                Arguments.of("Map<String, String>", ApexMapValue.class),
                Arguments.of("Set<String>", ApexSetValue.class),
                Arguments.of("SObject[]", ApexListValue.class));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: type=({0}):class=({1})")
    public void testTypesBuiltByBuilder(String type, Class<? extends ApexValue<?>> clazz) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	"
                    + type
                    + " v = ("
                    + type
                    + ")JSON.deserialize(asJson, "
                    + type
                    + ".class);\n"
                    + "   	System.debug(v);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final String expectedType =
                ApexStandardLibraryUtil.convertArrayToList(type)
                        .orElse(StringUtil.stripAllSpaces(type));
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                value.getTypeVertex().get().getCanonicalType(), equalTo(expectedType));
        MatcherAssert.assertThat(value, instanceOf(clazz));
    }
}
