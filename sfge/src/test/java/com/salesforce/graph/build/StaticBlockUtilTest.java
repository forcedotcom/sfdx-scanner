package com.salesforce.graph.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StaticBlockUtilTest {
    protected GraphTraversalSource g;
    private static final String SYNTHETIC_STATIC_BLOCK_METHOD_1 =
            String.format(StaticBlockUtil.SYNTHETIC_STATIC_BLOCK_METHOD_NAME, 1);

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    // does each static block have a corresponding synthetic method block?
    @Test
    public void testMethodForStaticBlock() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	static {\n"
                    + "	System.debug('inside static block 1');\n"
                    + "}\n"
                    + "	static {\n"
                    + "		System.debug('inside static block 2');\n"
                    + "	}\n"
                    + "void doSomething() {\n"
                    + "	System.debug('inside doSomething');\n"
                    + "}\n"
                    + "}"
        };

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        verifyStaticBlockMethod(
                String.format(StaticBlockUtil.SYNTHETIC_STATIC_BLOCK_METHOD_NAME, 1),
                "inside static block 1");
        verifyStaticBlockMethod(
                String.format(StaticBlockUtil.SYNTHETIC_STATIC_BLOCK_METHOD_NAME, 2),
                "inside static block 2");
    }

    private void verifyStaticBlockMethod(String methodName, String printedString) {
        final List<ApexPath> staticBlockPaths = GraphBuildTestUtil.walkAllPaths(g, methodName);
        assertThat(staticBlockPaths, hasSize(1));
        final ApexPath path = staticBlockPaths.get(0);

        // Make sure synthetic method is linked to the static block and its contents
        final BlockStatementVertex blockStatementVertex = (BlockStatementVertex) path.firstVertex();
        final List<BaseSFVertex> blockStmtChildren = blockStatementVertex.getChildren();
        assertThat(blockStmtChildren, hasSize(1));

        final ExpressionStatementVertex expressionStatementVertex =
                (ExpressionStatementVertex) blockStmtChildren.get(0);
        final List<BaseSFVertex> exprStmtChildren = expressionStatementVertex.getChildren();
        assertThat(exprStmtChildren, hasSize(1));

        final MethodCallExpressionVertex methodCallExpressionVertex =
                (MethodCallExpressionVertex) exprStmtChildren.get(0);
        assertThat(methodCallExpressionVertex.getFullMethodName(), equalTo("System.debug"));
        final LiteralExpressionVertex<String> literalExpressionVertex =
                (LiteralExpressionVertex<String>) methodCallExpressionVertex.getParameters().get(0);

        assertThat(literalExpressionVertex.getLiteralAsString(), equalTo(printedString));
    }

    // does synthetic invoker method exist and contain the necessary parts and relationship?
    @Test
    public void testMethodInvokerForStaticBlock() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	static {\n"
                    + "	System.debug('inside static block');\n"
                    + "}\n"
                    + "void doSomething() {\n"
                    + "	System.debug('inside doSomething');\n"
                    + "}\n"
                    + "}"
        };

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        final List<ApexPath> staticBlockPaths =
                GraphBuildTestUtil.walkAllPaths(g, StaticBlockUtil.STATIC_BLOCK_INVOKER_METHOD);
        assertThat(staticBlockPaths, hasSize(1));
        final ApexPath path = staticBlockPaths.get(0);

        // Make sure synthetic method is linked to the static block and its contents
        final BlockStatementVertex blockStatementVertex = (BlockStatementVertex) path.firstVertex();
        final List<BaseSFVertex> blockStmtChildren = blockStatementVertex.getChildren();
        assertThat(blockStmtChildren, hasSize(1));

        final ExpressionStatementVertex expressionStatementVertex =
                (ExpressionStatementVertex) blockStmtChildren.get(0);
        final List<BaseSFVertex> exprStmtChildren = expressionStatementVertex.getChildren();
        assertThat(exprStmtChildren, hasSize(1));

        final MethodCallExpressionVertex methodCallExpressionVertex =
                (MethodCallExpressionVertex) exprStmtChildren.get(0);
        assertThat(
                methodCallExpressionVertex.getFullMethodName(),
                equalTo(SYNTHETIC_STATIC_BLOCK_METHOD_1));
    }
}
