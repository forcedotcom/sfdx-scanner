package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests verifying behavior of method calls made on iterated items. The types of values iterated
 * belong to three categories:
 *
 * <ul>
 *   <li>1. Primitives such as String, Integer.
 *   <li>2. Apex Standard Library classes such as Schema.DescribeFieldResult, Schema.SObjectType,
 *       etc.
 *   <li>3. Instances of classes defined within source code.
 * </ul>
 *
 * Even though we'd ideally prefer to have all of them treated the same way, we are currently
 * treating them differently. Category 3 (Method invocation on instances of classes) require path
 * expansion to understand what value is returned, but Categories 1 and 2 have predetermined methods
 * whose value we derive directly within Graph Engine (for example, toLowerCase() on a String
 * primitive). This leads to Categories 1 & 2 returning an ApexForLoopValue when a method is invoked
 * on them during iteration (see {@link ApexForLoopValue#apply(MethodCallExpressionVertex,
 * SymbolProvider)}) and Category 3 returning a non-loop value when a method is invoked on it.
 *
 * <p>While For-loop and ForEach-loop are well-structured, While-loops don't clearly state what's
 * iterated. Because of this shortcoming, we don't give loop-treatment to method calls made inside a
 * While-loop. This should be addressed in the future.
 *
 * <p>The tests in this class have various cases enumerating the explanation above.
 */
public class ApexPathExpanderInLoopTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleNoLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean b = new Bean();\n"
                    + "       System.debug(b.getValue());\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   public String getValue() {\n"
                    + "       return 'hi';\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("hi"));
    }

    @Test
    public void testSimpleForEachLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean(), new Bean()};\n"
                    + "       for (Bean b : beans) {\n"
                    + "           System.debug(b.getValue());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   public String getValue() {\n"
                    + "       return 'hi';\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Consider getting output value of method application on ForLoopValue to return
        //  another ForLoopValue
        final ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testForLoopWithPrimitive() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String[] myList = new String[] {' hi '};\n"
                    + "       for (Integer i = 0; i < myList.size(); i++) {\n"
                    + "           System.debug(myList[i].trim());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue apexForLoopValue = visitor.getSingletonResult();
        List<String> valueList =
                apexForLoopValue.getForLoopValues().stream()
                        .map(apexValue -> TestUtil.apexValueToString(apexValue))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, Matchers.containsInAnyOrder("hi"));
    }

    @Test
    public void testWhileLoopWithPrimitive() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String[] myList = new String[] {' hi '};\n"
                    + "       Integer i = 0;\n"
                    + "       while (i < myList.size()) {\n"
                    + "           System.debug(myList[i].trim());\n"
                    + "           i++;\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Detect while-loop iteration and create ApexForLoopValue (new name?) for the
        // iterated item.
        final ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testForEachLoopWithPrimitive() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String[] myList = new String[] {' hi '};\n"
                    + "       for (String s : myList) {\n"
                    + "           System.debug(s.trim());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue apexForLoopValue = visitor.getSingletonResult();
        List<String> valueList =
                apexForLoopValue.getForLoopValues().stream()
                        .map(apexValue -> TestUtil.apexValueToString(apexValue))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, Matchers.containsInAnyOrder("hi"));
    }

    @Test
    public void testSimpleForLoop_withTempVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean()};\n"
                    + "       for (Integer i = 0; i < beans.size(); i++) {\n"
                    + "           Bean bean1 = beans[i];"
                    + "           System.debug(bean1.getValue());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   public String getValue() {\n"
                    + "       return 'hi';\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Consider getting output value of method application on ForLoopValue to return
        //  another ForLoopValue

        ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testSimpleForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean()};\n"
                    + "       for (Integer i = 0; i < beans.size(); i++) {\n"
                    + "           System.debug(beans[i].getValue());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   public String getValue() {\n"
                    + "       return 'hi';\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Consider getting output value of method application on ForLoopValue to return
        //  another ForLoopValue

        ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testSimpleNoLoopWithField() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean b = new Bean('hi');\n"
                    + "       System.debug(b.getParam());\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   private String param;\n"
                    + "   public Bean(String s) {\n"
                    + "       param = s;"
                    + "   }\n"
                    + "   public String getParam() {\n"
                    + "       return param;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("hi"));
    }

    @Test
    public void testForEachLoopWithField() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean('hi'), new Bean('hello')};\n"
                    + "       for (Bean b: beans) {\n"
                    + "           System.debug(b.getParam());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   private String param;\n"
                    + "   public Bean(String s) {\n"
                    + "       param = s;"
                    + "   }\n"
                    + "   public String getParam() {\n"
                    + "       return param;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Consider getting output value of method application on ForLoopValue to return
        //  another ForLoopValue
        final ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testForLoopWithField() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean('hi'), new Bean('hello')};\n"
                    + "       for (Integer i = 0; i < beans.size(); i++) {\n"
                    + "           System.debug(beans[i].getParam());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {\n"
                    + "   private String param;\n"
                    + "   public Bean(String s) {\n"
                    + "       param = s;"
                    + "   }\n"
                    + "   public String getParam() {\n"
                    + "       return param;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Consider getting output value of method application on ForLoopValue to return
        // another ForLoopValue
        final ApexStringValue stringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(stringValue.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testForLoopWithStandardClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeFieldResult[] fieldResults = new Schema.DescribeFieldResult[] {Account.Name.getDescribe(), Account.description.getDescribe()};\n"
                    + "       for (Integer i = 0; i < fieldResults.size(); i++) {\n"
                    + "           System.debug(fieldResults[i].isAccessible());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();
        List<ApexValue<?>> forLoopValues = forLoopValue.getForLoopValues();
        MatcherAssert.assertThat(forLoopValues.size(), equalTo(2));
        MatcherAssert.assertThat(forLoopValues.get(0).isIndeterminant(), equalTo(true));
    }

    @Test
    public void testWhileLoopWithStandardClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeFieldResult[] fieldResults = new Schema.DescribeFieldResult[] {Account.Name.getDescribe(), Account.description.getDescribe()};\n"
                    + "       Integer i = 0;\n"
                    + "       while (i < fieldResults.size()) {\n"
                    + "           System.debug(fieldResults[i].isAccessible());\n"
                    + "           i++;\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        // TODO: Detect while-loop iteration and create ApexForLoopValue (new name?) for the
        // iterated item.
        final ApexBooleanValue booleanValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(booleanValue.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testForEachLoopWithStandardClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeFieldResult[] fieldResults = new Schema.DescribeFieldResult[] {Account.Name.getDescribe(), Account.description.getDescribe()};\n"
                    + "       for (Schema.DescribeFieldResult fieldResult : fieldResults) {\n"
                    + "           System.debug(fieldResult.isAccessible());\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();
        List<ApexValue<?>> forLoopValues = forLoopValue.getForLoopValues();
        MatcherAssert.assertThat(forLoopValues.size(), equalTo(2));
        MatcherAssert.assertThat(forLoopValues.get(0).isIndeterminant(), equalTo(true));
    }
}
