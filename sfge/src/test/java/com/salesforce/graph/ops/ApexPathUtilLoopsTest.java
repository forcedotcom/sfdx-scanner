package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** {@link ApexPathUtil} tests specific to loop structures. */
public class ApexPathUtilLoopsTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testForLoopMethodCallOnIndeterminantList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(String[] myList) {\n"
                        + "       for (Integer i = 0; i < myList.size(); i++) {\n"
                        + "           debug1(myList[i]);\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void debug1(String s) {\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testForEachLoopWithNonForLoopMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "       for (String myString: myList) {\n"
                        + "           debug1(100);\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void debug1(Integer int) {\n"
                        + "       System.debug(int);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(100));
    }

    @Test
    public void testForLoopWithNonForLoopMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "       for (Integer i = 0; i < myList.size(); i++) {\n"
                        + "           debug1(100);\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void debug1(Integer int) {\n"
                        + "       System.debug(int);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(100));
    }

    @Test
    @Disabled // TODO: Handle values looped within while-loops as a ApexLoopValue
    public void testWhileLoopWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "           Integer i = 0;\n"
                        + "       while (i < myList.size()) {\n"
                        + "           debug1(myList[i]);\n"
                        + "           i++;\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void debug1(String str) {\n"
                        + "       System.debug(str);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexForLoopValue value = visitor.getSingletonResult();
        List<String> stringValues =
                value.getForLoopValues().stream()
                        .map(item -> TestUtil.apexValueToString(item))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(stringValues, containsInAnyOrder("hi", "hello"));
    }

    @Test
    public void testWhileLoopWithMethodCallOnNonIterativeItem() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "           Integer i = 0;\n"
                        + "       while (i < myList.size()) {\n"
                        + "           debug1(100);\n"
                        + "           i++;\n"
                        + "       }\n"
                        + "   }\n"
                        + "   public void debug1(Integer int) {\n"
                        + "       System.debug(int);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(100));
    }

    @Test
    @Disabled // TODO: Handle do/while loops
    public void testDoWhileLoopWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "           Integer i = 0;\n"
                        + "       do {\n"
                        + "           debug1(myList[i]);\n"
                        + "           i++;\n"
                        + "       } while (i < myList.size());\n"
                        + "   }\n"
                        + "   public void debug1(String str) {\n"
                        + "       System.debug(str);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo("hi"));
    }

    @Test
    @Disabled // TODO: Handle do/while loops
    public void testDoWhileLoopWithMethodCallOnNonIterativeItem() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String[] myList = new String[]{'hi','hello'};\n"
                        + "           Integer i = 0;\n"
                        + "       do {\n"
                        + "           debug1(100);\n"
                        + "           i++;\n"
                        + "       } while (i < myList.size());\n"
                        + "   }\n"
                        + "   public void debug1(Integer int) {\n"
                        + "       System.debug(int);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo(100));
    }
}
