package com.salesforce.graph.ops;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApexValueUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testApplyBinaryExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		System.debug('hello' + 'hi');\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("hellohi"));
    }

    @Test
    public void testApplyBinaryExpressionWithDeterminant() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		String input = getInput();"
                        + "		System.debug('hello' + input);\n"
                        + "	}\n"
                        + "	public String getInput() {\n"
                        + "		return 'hi';\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("hellohi"));
    }

    @Test
    public void testApplyBinaryExpressionWithNullValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		String input = null;"
                        + "		System.debug('hello' + input);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("hellonull"));
    }

    @Test
    @Disabled // TODO: handle apex value for binary expressions with indeterminant portions
    public void testApplyBinaryExpressionWithIndeterminant() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething(String input) {\n"
                        + "		System.debug('hello' + input);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        assertThat(apexStringValue.isIndeterminant(), equalTo(true));
    }
}
