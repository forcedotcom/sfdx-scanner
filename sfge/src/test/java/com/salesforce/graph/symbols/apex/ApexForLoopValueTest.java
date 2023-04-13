package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNot.not;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.ApexValueMatchers;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.List;
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

public class ApexForLoopValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    public static Stream<Arguments> forLoopContentsSource() {
        return Stream.of(
                // The loop variable is saved to an intermediate variable
                // This is handled by VariableExpressionVertex
                Arguments.of(
                        "withVariable",
                        "String fieldToCheck = fieldsToCheck[i];\n"
                                + "System.debug(fieldToCheck);\n"),
                // The loop variable is accessed without assigning it to an intermediate
                // This is handled by PathScopeVisitor
                Arguments.of("withoutVariable", "System.debug(fieldsToCheck[i]);"));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithDeclaredIndex(String permutationName, String loopContents) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                    + "       for (Integer i = 0; i < fieldsToCheck.size(); i++) {\n"
                    + loopContents
                    + "       }\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithBizarreCasing(String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; I < fieldsTOCHECK.size(); i++) {\n"
                        + loopContents.replaceAll("o", "O").replaceAll("c", "C")
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithAssignedIndex(String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       Integer i;\n"
                        + "       for (i = 0; i < fieldsToCheck.size(); i++) {\n"
                        + loopContents
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithPlusEqualsOneIncrementation(
            String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; i < fieldsToCheck.size(); i+=1) {\n"
                        + loopContents
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithNonzeroIndexInit(String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 1; i < fieldsToCheck.size(); i++) {\n"
                        + loopContents
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Long-term, we DO want this to be an instance of ApexForLoopValue, but one that
        // can't
        //  necessarily resolve to known values.
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, not(instanceOf(ApexForLoopValue.class)));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithIncompleteArrayIteration(
            String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; i < fieldsToCheck.size() - 1; i++) {\n"
                        + loopContents
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Long-term, we DO want this to be an instance of ApexForLoopValue, but one that
        // can't
        //  necessarily resolve to known values.
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, not(instanceOf(ApexForLoopValue.class)));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithPlusEqualsTwoIncrementation(
            String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; i < fieldsToCheck.size(); i+=2) {\n"
                        + loopContents
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Long-term, we DO want this to be an instance of ApexForLoopValue, but one that
        // can't
        //  necessarily resolve to known values.
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, not(instanceOf(ApexForLoopValue.class)));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithReassignedIndexVariable(
            String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; i < fieldsToCheck.size(); i++) {\n"
                        + loopContents
                        + "           i += 1;\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Long-term, we DO want this to be an instance of ApexForLoopValue, but one that
        // can't
        //  necessarily resolve to known values.
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, not(instanceOf(ApexForLoopValue.class)));
    }

    @MethodSource("forLoopContentsSource")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testForLoopWithReassignedArrayVariable(
            String permutationName, String loopContents) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i = 0; i < fieldsToCheck.size(); i++) {\n"
                        + loopContents
                        + "           fieldsToCheck = new String[]{'beep', 'boop'};\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Long-term, we DO want this to be an instance of ApexForLoopValue, but one that
        // can't
        //  necessarily resolve to known values.
        ApexValue<?> value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value, not(instanceOf(ApexForLoopValue.class)));
    }

    @Test
    public void testForEach() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
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
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @Test
    public void testForEachInline() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       for (String fieldToCheck : new String [] {'Name', 'Phone'}) {\n"
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
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @Test
    public void testForEachOtherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           verifyCreateable(fieldToCheck);\n"
                        + "       }\n"
                        + "   }\n"
                        + "    public void verifyCreateable(String fieldName) {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        System.debug(fieldName);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    @Test
    public void testForEachSetsVariableOnInheritedScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String s = 'Hello1';\n"
                        + "       for (String fieldToCheck : new String [] {'Name', 'Phone'}) {\n"
                        + "           s = 'Hello2';\n"
                        + "       }\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello2"));
    }

    @Test
    public void testListFromMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static string myString;\n"
                    + "   public static void doSomething() {\n"
                    + "       List<String> l = getList();\n"
                    + "       for (integer i=0; i<l.size(); i++) {\n"
                    + "       	System.debug(l[i]);\n"
                    + "   	}\n"
                    + "   }\n"
                    + "   public static List<String> getList() {\n"
                    + "   	return new List<String>{'Name', 'Phone'};\n"
                    + "   }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> values =
                value.getForLoopValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    /**
     * This test verifies that a null list is not iterated over. This is because {@link
     * ApexValue#checkForNullAccess(MethodCallExpressionVertex, SymbolProvider)} will throw an
     * {@link NullValueAccessedException} when l.size is invoked. This results in the 2 paths
     * collapsing into a single path.
     */
    @Test
    public void testNullListIsNotIteratedOver() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static string myString;\n"
                    + "   public static void doSomething(Boolean b) {\n"
                    + "       List<String> l = getList(b);\n"
                    + "       for (integer i=0; i<l.size(); i++) {\n"
                    + "       	System.debug(l[i]);\n"
                    + "   	}\n"
                    + "   }\n"
                    + "   public static List<String> getList(Boolean b) {\n"
                    + "       if (b) {\n"
                    + "       	return null;\n"
                    + "   	} else {\n"
                    + "       	List<String> result = new List<String>();\n"
                    + "       	result.add('Hello');\n"
                    + "       	return result;\n"
                    + "   	}\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<ApexValue<?>> values = value.getForLoopValues();
        MatcherAssert.assertThat(values, hasSize(equalTo(1)));
        MatcherAssert.assertThat(TestUtil.apexValueToString(values.get(0)), equalTo("Hello"));
    }

    /**
     * Verifies that a method invoked on a list containing NewObjectExpressions are executed on the
     * NewObjectExpressions and not the list itself.
     */
    @Test
    public void testInvokeMethodExecutesOnObjectNotList() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + " 	private static final List<SelectOption> options =\n"
                    + "   	new List<SelectOption> {\n"
                    + "       	new SelectOption('', '--Select One--'),\n"
                    + "           new SelectOption('opt1', 'Option 1')\n"
                    + "      	};\n"
                    + "   public static void doSomething(String s) {\n"
                    + "       for (SelectOption option : options) {\n"
                    + "   		System.debug(option.getLabel());\n"
                    + "   	}\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        // TODO: This is SelectOption because we don't have the code for SelectOption.
        // PathScopeVisitor should
        // return an ApexSingleValue of an unknown type instead
        MatcherAssert.assertThat(value, ApexValueMatchers.typeEqualTo("SelectOption"));
    }

    @Test
    public void testStdMethodCallOnForLoopVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		List<Schema.SObjectField> fields = new List<Schema.SObjectFields>{Schema.Account.fields.Name,Schema.Account.fields.Phone};\n"
                        + "		for (Schema.SObjectField field: fields) {\n"
                        + "			System.debug(field.getDescribe());\n"
                        + "       System.debug(field.getDescribe().isCreateable());\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue forLoopValue1 = visitor.getResult(0);
        List<ApexValue<?>> describeForLoopValues = forLoopValue1.getForLoopValues();
        List<String> fieldNames =
                describeForLoopValues.stream()
                        .map(
                                apexValue ->
                                        TestUtil.apexValueToString(
                                                ((DescribeFieldResult) apexValue).getFieldName()))
                        .collect(Collectors.toList());

        MatcherAssert.assertThat(fieldNames, containsInAnyOrder("Name", "Phone"));

        ApexForLoopValue forLoopValue2 = visitor.getResult(1);
        List<ApexValue<?>> isCreateableValues = forLoopValue2.getForLoopValues();
        isCreateableValues.forEach(
                value -> {
                    MatcherAssert.assertThat(value, Matchers.instanceOf(ApexBooleanValue.class));
                    MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
                });
    }

    @Test
//    @Disabled // TODO: apply() method on ApexClassInstanceValue should have the capability to match
    // the method call and convert to ApexValue
    public void testMethodCallOnForLoopVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething() {\n"
                    + "		List<Bean> beans = new List<Bean>{new Bean('hi'),new Bean('hello')};\n"
                    + "		for (Bean mybean: beans) {\n"
                    + "			System.debug(mybean);\n"
                    + "			System.debug(mybean.getValue());\n"
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

        ApexForLoopValue value = visitor.getResult(0);
        List<ApexValue<?>> forLoopValues = value.getForLoopValues();
        MatcherAssert.assertThat(forLoopValues, hasSize(2));
        ApexClassInstanceValue classInstanceValue = (ApexClassInstanceValue) forLoopValues.get(0);
        MatcherAssert.assertThat(classInstanceValue.getCanonicalType(), equalTo("Bean"));

        ApexForLoopValue derivedValue = visitor.getResult(1);
        List<ApexValue<?>> derivedForLoopValues = derivedValue.getForLoopValues();
        List<String> valueStrings =
                derivedForLoopValues.stream()
                        .map(apexValue -> TestUtil.apexValueToString(apexValue))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(valueStrings, Matchers.containsInAnyOrder("hi", "hello"));
    }

    @Test
    @Disabled // TODO: Method invocation on indeterminant forLoop values should lead to
    // indeterminant return values
    public void testMethodCallOnIndeterminantForLoopVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething(List<Bean> beans) {\n"
                    + "       System.debug(beans);\n"
                    + "		for (Bean myBean: beans) {\n"
                    + "			System.debug(myBean);\n"
                    + "			System.debug(myBean.getValue());\n"
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

        ApexListValue listValue = visitor.getResult(0);
        MatcherAssert.assertThat(listValue.isIndeterminant(), equalTo(true));

        ApexForLoopValue value = visitor.getResult(1);
        List<ApexValue<?>> forLoopValues = value.getForLoopValues();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                forLoopValues, hasSize(1)); // TODO: should this be a single indeterminant value?
        MatcherAssert.assertThat(value.getDeclaredType().get(), equalTo("Bean"));

        ApexForLoopValue derivedValue = visitor.getResult(2);
        List<ApexValue<?>> derivedForLoopValues = derivedValue.getForLoopValues();
        MatcherAssert.assertThat(derivedValue.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                derivedForLoopValues,
                hasSize(1)); // TODO: should this be a single indeterminant value?
    }
}
