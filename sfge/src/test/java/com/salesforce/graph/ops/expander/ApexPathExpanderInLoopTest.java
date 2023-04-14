package com.salesforce.graph.ops.expander;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;

public class ApexPathExpanderInLoopTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleNoLoop() {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "   public void doSomething() {\n" +
                "       Bean b = new Bean();\n" +
                "       System.debug(b.getValue());\n" +
                "   }\n" +
                "}\n",
            "public class Bean {\n" +
                "   public String getValue() {\n" +
                "       return 'hi';\n" +
                "   }\n" +
                "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("hi"));
    }

    @Test
    @Disabled // Fix in progress
    public void testSimpleForEachLoop() {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "   public void doSomething() {\n" +
                "       Bean[] beans = new Bean[] {new Bean(), new Bean()};\n" +
                "       for (Bean b : beans) {\n" +
                "           System.debug(b.getValue());\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Bean {\n" +
                "   public String getValue() {\n" +
                "       return 'hi';\n" +
                "   }\n" +
                "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue apexForLoopValue = visitor.getSingletonResult();
        List<String> valueList = apexForLoopValue.getForLoopValues().stream().map(apexValue -> TestUtil.apexValueToString(apexValue)).collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, Matchers.containsInAnyOrder("hi", "hi"));
    }

    @Test
    @Disabled // Fix in progress
    public void testSimpleForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "   public void doSomething() {\n" +
                "       Bean[] beans = new Bean[] {new Bean()};\n" +
                "       for (Integer i = 0; i < beans.size(); i++) {\n" +
                "           System.debug(beans[i].getValue());\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Bean {\n" +
                "   public String getValue() {\n" +
                "       return 'hi';\n" +
                "   }\n" +
                "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue apexForLoopValue = visitor.getSingletonResult();
        List<String> valueList = apexForLoopValue.getForLoopValues().stream().map(apexValue -> TestUtil.apexValueToString(apexValue)).collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, Matchers.containsInAnyOrder("hi"));
    }

    @Test
    public void testSimpleNoLoopWithField() {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "   public void doSomething() {\n" +
                "       Bean b = new Bean('hi');\n" +
                "       System.debug(b.getParam());\n" +
                "   }\n" +
                "}\n",
            "public class Bean {\n" +
                "   private String param;\n" +
                "   public Bean(String s) {\n" +
                "       param = s;" +
                "   }\n" +
                "   public String getParam() {\n" +
                "       return param;\n" +
                "   }\n" +
                "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("hi"));
    }

    @Test
    @Disabled // Fix in progress
    public void testSimpleLoopWithField() {
        String[] sourceCode = {
            "public class MyClass {\n" +
                "   public void doSomething() {\n" +
                "       Bean[] beans = new Bean[] {new Bean('hi'), new Bean('hello')};\n" +
                "       for (Bean b: beans) {\n" +
                "           System.debug(b.getParam());\n" +
                "       }\n" +
                "   }\n" +
                "}\n",
            "public class Bean {\n" +
                "   private String param;\n" +
                "   public Bean(String s) {\n" +
                "       param = s;" +
                "   }\n" +
                "   public String getParam() {\n" +
                "       return param;\n" +
                "   }\n" +
                "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue apexForLoopValue = visitor.getSingletonResult();
        List<String> valueList = apexForLoopValue.getForLoopValues().stream().map(apexValue -> TestUtil.apexValueToString(apexValue)).collect(Collectors.toList());
        MatcherAssert.assertThat(valueList, Matchers.containsInAnyOrder("hi", "hello"));
    }

}
