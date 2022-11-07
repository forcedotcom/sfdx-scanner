package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.exception.UserActionException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.CatchBlockStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.IdentifierCaseVertex;
import com.salesforce.graph.vertex.IfBlockStatementVertex;
import com.salesforce.graph.vertex.IfElseBlockStatementVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ReturnStatementVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import com.salesforce.graph.vertex.TryCatchFinallyBlockStatementVertex;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MethodPathBuilderTest {
    protected GraphTraversalSource g;

    private static final String[] BLOCK = new String[] {NodeType.BLOCK_STATEMENT};

    private static final String[] BLOCK_IF =
            new String[] {NodeType.BLOCK_STATEMENT, NodeType.IF_ELSE_BLOCK_STATEMENT};

    private static final String[] BLOCK_IF_BLOCK =
            new String[] {
                NodeType.BLOCK_STATEMENT, NodeType.IF_ELSE_BLOCK_STATEMENT, NodeType.BLOCK_STATEMENT
            };

    private static final String[] BLOCK_IF_BLOCK_IF_BLOCK =
            new String[] {
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT
            };

    private static final String[] BLOCK_IF_BLOCK_IF =
            new String[] {
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT
            };

    private static final String[] BLOCK_IF_BLOCK_FOREACH_BLOCK =
            new String[] {
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT,
                NodeType.FOR_EACH_STATEMENT,
                NodeType.BLOCK_STATEMENT
            };

    private static final String[] BLOCK_FORLOOP =
            new String[] {NodeType.BLOCK_STATEMENT, NodeType.FOR_LOOP_STATEMENT};

    private static final String[] BLOCK_IF_BLOCK_FORLOOP =
            new String[] {
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT,
                NodeType.FOR_LOOP_STATEMENT
            };

    private static final String[] BLOCK_IF_BLOCK_FORLOOP_BLOCK =
            new String[] {
                NodeType.BLOCK_STATEMENT,
                NodeType.IF_ELSE_BLOCK_STATEMENT,
                NodeType.BLOCK_STATEMENT,
                NodeType.FOR_LOOP_STATEMENT,
                NodeType.BLOCK_STATEMENT
            };

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testMethodWithSingleExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       System.debug('Hello');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='4'>
        //    <ExpressionStatement BeginLine='3' EndLine='3'>
        //        <MethodCallExpression BeginLine='3' EndLine='3' FullMethodName='System.debug'>
        //            <ReferenceExpression BeginLine='3' EndLine='3' Image='System' />
        //            <LiteralExpression BeginLine='3' EndLine='3' Image='Hello' />
        //        </MethodCallExpression>
        //    </ExpressionStatement>
        // </BlockStatement>

        BlockStatementVertex blockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        ExpressionStatementVertex expressionStatementVertex =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 3);
        assertPathEdgesExist(Schema.CFG_PATH, Pair.of(blockStatement, expressionStatementVertex));

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.verticesInCurrentMethod(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                path.verticesInCurrentMethod().get(0), instanceOf(BlockStatementVertex.class));
        MatcherAssert.assertThat(
                path.verticesInCurrentMethod().get(1), instanceOf(ExpressionStatementVertex.class));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(1)));
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 3);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithNestedIfs() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log1, boolean log2) {\n"
                        + "       if (log1) {\n"
                        + "           if (log2) {\n"
                        + "               System.debug('Hello');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='8' Image='' RealLoc='true'>
        //    <IfElseBlockStatement BeginLine='3' DefiningType='MyClass' ElseStatement='false'
        // EndLine='3' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //                <VariableExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='log1' RealLoc='true'>
        //                    <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true'>
        //                <IfElseBlockStatement BeginLine='4' DefiningType='MyClass'
        // ElseStatement='false' EndLine='4' Image='' RealLoc='true'>
        //                    <IfBlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true'>
        //                        <StandardCondition BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='' RealLoc='true'>
        //                            <VariableExpression BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='log2' RealLoc='true'>
        //                                <EmptyReferenceExpression BeginLine='4' DefiningType=''
        // EndLine='4' Image='' RealLoc='false' />
        //                            </VariableExpression>
        //                        </StandardCondition>
        //                        <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='6'
        // Image='' RealLoc='true'>
        //                            <ExpressionStatement BeginLine='5' DefiningType='MyClass'
        // EndLine='5' Image='' RealLoc='true'>
        //                                <MethodCallExpression BeginLine='5' DefiningType='MyClass'
        // EndLine='5' FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                                    <ReferenceExpression BeginLine='5' Context=''
        // DefiningType='MyClass' EndLine='5' Image='System' Names='[System]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                                    <LiteralExpression BeginLine='5'
        // DefiningType='MyClass' EndLine='5' Image='Hello' LiteralType='STRING' Long='false'
        // Name='' Null='false' RealLoc='true' />
        //                                </MethodCallExpression>
        //                            </ExpressionStatement>
        //                        </BlockStatement>
        //                    </IfBlockStatement>
        //                    <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true' />
        //                </IfElseBlockStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        // </BlockStatement>

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));
        // System.debug('Hello');
        String[] expectedScopes =
                new String[] {
                    NodeType.BLOCK_STATEMENT,
                    NodeType.IF_ELSE_BLOCK_STATEMENT,
                    NodeType.BLOCK_STATEMENT,
                    NodeType.IF_ELSE_BLOCK_STATEMENT,
                    NodeType.BLOCK_STATEMENT
                };
        assertEndScopes(BLOCK_IF_BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 5);

        // inner implicit else
        assertEndScopes(BLOCK_IF_BLOCK_IF_BLOCK, BlockStatementVertex.class, 4, 4);

        // Outer implicit else
        assertEndScopes(BLOCK_IF_BLOCK, BlockStatementVertex.class, 3, 3);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithNestedIfsExpressionAfter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log1, boolean log2) {\n"
                        + "       if (log1) {\n"
                        + "           if (log2) {\n"
                        + "               System.debug('Hello');\n"
                        + "           }\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='8' Image='' RealLoc='true'>
        //    <IfElseBlockStatement BeginLine='3' DefiningType='MyClass' ElseStatement='false'
        // EndLine='3' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //                <VariableExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='log1' RealLoc='true'>
        //                    <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true'>
        //                <IfElseBlockStatement BeginLine='4' DefiningType='MyClass'
        // ElseStatement='false' EndLine='4' Image='' RealLoc='true'>
        //                    <IfBlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true'>
        //                        <StandardCondition BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='' RealLoc='true'>
        //                            <VariableExpression BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='log2' RealLoc='true'>
        //                                <EmptyReferenceExpression BeginLine='4' DefiningType=''
        // EndLine='4' Image='' RealLoc='false' />
        //                            </VariableExpression>
        //                        </StandardCondition>
        //                        <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='6'
        // Image='' RealLoc='true'>
        //                            <ExpressionStatement BeginLine='5' DefiningType='MyClass'
        // EndLine='5' Image='' RealLoc='true'>
        //                                <MethodCallExpression BeginLine='5' DefiningType='MyClass'
        // EndLine='5' FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                                    <ReferenceExpression BeginLine='5' Context=''
        // DefiningType='MyClass' EndLine='5' Image='System' Names='[System]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                                    <LiteralExpression BeginLine='5'
        // DefiningType='MyClass' EndLine='5' Image='Hello' LiteralType='STRING' Long='false'
        // Name='' Null='false' RealLoc='true' />
        //                                </MethodCallExpression>
        //                            </ExpressionStatement>
        //                        </BlockStatement>
        //                    </IfBlockStatement>
        //                    <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true' />
        //                </IfElseBlockStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        // </BlockStatement>

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(4)));

        // System.debug('Hello')
        assertEndScopes(BLOCK_IF_BLOCK_IF, ExpressionStatementVertex.class, 5);

        // inner implicit else
        assertEndScopes(BLOCK_IF_BLOCK_IF, BlockStatementVertex.class, 4, 4);

        // Outer implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 3, 3);

        // System.debug('After')
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 8);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithIfStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (log) {\n"
                        + "           System.debug('Hello');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='6'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='3'>
        //        <IfBlockStatement BeginLine='3' EndLine='3'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='5'>
        //                <ExpressionStatement BeginLine='4' EndLine='4'>
        //                    <MethodCallExpression BeginLine='4' EndLine='4'
        // FullMethodName='System.debug'>
        //                        <ReferenceExpression BeginLine='4' EndLine='4' Image='System' />
        //                        <LiteralExpression BeginLine='4' EndLine='4' Image='Hello' />
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' EndLine='3'/>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        BlockStatementVertex blockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        IfElseBlockStatementVertex ifElseBlockStatement =
                TestUtil.getVertexOnLine(g, IfElseBlockStatementVertex.class, 3);
        IfBlockStatementVertex ifBlockStatement =
                TestUtil.getVertexOnLine(g, IfBlockStatementVertex.class, 3);
        StandardConditionVertex standardCondition =
                TestUtil.getVertexOnLine(g, StandardConditionVertex.class, 3);
        BlockStatementVertex positiveStandardConditionBlockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3, 5);
        ExpressionStatementVertex expressionStatement =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 4);
        // This is the implicit else Empty block statement
        BlockStatementVertex negativeStandardConditionBlockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3, 3);

        assertPathEdgesExist(
                Schema.CFG_PATH,
                Pair.of(blockStatement, ifElseBlockStatement),
                Pair.of(ifElseBlockStatement, ifBlockStatement),
                Pair.of(ifBlockStatement, standardCondition),
                Pair.of(standardCondition, positiveStandardConditionBlockStatement),
                Pair.of(positiveStandardConditionBlockStatement, expressionStatement),
                Pair.of(standardCondition, negativeStandardConditionBlockStatement));

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(5, 6));

        ApexPath positivePath = TestUtil.getPathOfSize(paths, 6);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));
        // Call to System.debug
        MatcherAssert.assertThat(
                positivePath.lastVertex(), instanceOf(ExpressionStatementVertex.class));

        ApexPath negativePath = TestUtil.getPathOfSize(paths, 5);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));

        // Default implicit if which is an empty block statement
        MatcherAssert.assertThat(negativePath.lastVertex(), instanceOf(BlockStatementVertex.class));
        MatcherAssert.assertThat(negativePath.lastVertex().getChildren(), hasSize(equalTo(0)));

        // These statements end both of the if scopes
        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug('Hello');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 4);

        // Implicit else
        assertEndScopes(BLOCK_IF_BLOCK, BlockStatementVertex.class, 3, 3);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithSingleIfElseStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (log) {\n"
                        + "           System.debug('Hello');\n"
                        + "       } else {\n"
                        + "           System.debug('Goodbye');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='8'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='3'>
        //        <IfBlockStatement BeginLine='3' EndLine='3'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='5'>
        //                <ExpressionStatement BeginLine='4' EndLine='4'>
        //                    <MethodCallExpression BeginLine='4' EndLine='4'
        // FullMethodName='System.debug'>
        //                        <ReferenceExpression BeginLine='4' EndLine='4' Image='System' />
        //                        <LiteralExpression BeginLine='4' EndLine='4' Image='Hello' />
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='5' EndLine='7'>
        //            <ExpressionStatement BeginLine='6' EndLine='6'>
        //                <MethodCallExpression BeginLine='6' EndLine='6'
        // FullMethodName='System.debug'>
        //                    <ReferenceExpression BeginLine='6' EndLine='6' Image='System' />
        //                    <LiteralExpression BeginLine='6' EndLine='6' Image='Goodbye' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        BlockStatementVertex blockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        IfElseBlockStatementVertex ifElseBlockStatement =
                TestUtil.getVertexOnLine(g, IfElseBlockStatementVertex.class, 3);
        IfBlockStatementVertex ifBlockStatement =
                TestUtil.getVertexOnLine(g, IfBlockStatementVertex.class, 3);
        StandardConditionVertex standardCondition =
                TestUtil.getVertexOnLine(g, StandardConditionVertex.class, 3);
        BlockStatementVertex positiveStandardConditionBlockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3, 5);
        ExpressionStatementVertex expressionStatementLine4 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 4);
        BlockStatementVertex negativeStandardConditionBlockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 5, 7);
        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);

        assertPathEdgesExist(
                Schema.CFG_PATH,
                Pair.of(blockStatement, ifElseBlockStatement),
                Pair.of(ifElseBlockStatement, ifBlockStatement),
                Pair.of(ifBlockStatement, standardCondition),
                Pair.of(standardCondition, positiveStandardConditionBlockStatement),
                Pair.of(positiveStandardConditionBlockStatement, expressionStatementLine4),
                Pair.of(standardCondition, negativeStandardConditionBlockStatement),
                Pair.of(negativeStandardConditionBlockStatement, expressionStatementLine6));

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(6, 6));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine4);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));
        // Call to System.debug('Hello');
        MatcherAssert.assertThat(
                positivePath.lastVertex(), instanceOf(ExpressionStatementVertex.class));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine6);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        // Call to System.debug('Goodbye');
        MatcherAssert.assertThat(
                negativePath.lastVertex(), instanceOf(ExpressionStatementVertex.class));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug('Hello');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 4);

        // System.debug('GoodBye');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 6);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithSingleIf_ElseIf_ElseStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (log) {\n"
                        + "           System.debug('Hello');\n"
                        + "       } else if (x == 10) {\n"
                        + "           System.debug('X Is 10');\n"
                        + "       } else {\n"
                        + "           System.debug('Goodbye');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='10'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='5'>
        //        <IfBlockStatement BeginLine='3' EndLine='5'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='5'>
        //                <ExpressionStatement BeginLine='4' EndLine='4'>
        //                    <MethodCallExpression BeginLine='4' EndLine='4'
        // FullMethodName='System.debug'>
        //                        <ReferenceExpression BeginLine='4' EndLine='4' Image='System' />
        //                        <LiteralExpression BeginLine='4' EndLine='4' Image='Hello' />
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <IfBlockStatement BeginLine='3' EndLine='5'>
        //            <StandardCondition BeginLine='5' EndLine='5'>
        //                <BooleanExpression BeginLine='5' EndLine='5'>
        //                    <VariableExpression BeginLine='5' EndLine='5' Image='x'>
        //                        <EmptyReferenceExpression BeginLine='5' EndLine='5'/>
        //                    </VariableExpression>
        //                    <LiteralExpression BeginLine='5' EndLine='5' Image='10' />
        //                </BooleanExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='5' EndLine='7'>
        //                <ExpressionStatement BeginLine='6' EndLine='6'>
        //                    <MethodCallExpression BeginLine='6' EndLine='6'
        // FullMethodName='System.debug'>
        //                        <ReferenceExpression BeginLine='6' EndLine='6' Image='System' />
        //                        <LiteralExpression BeginLine='6' EndLine='6' Image='X Is 10' />
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='7' EndLine='9'>
        //            <ExpressionStatement BeginLine='8' EndLine='8'>
        //                <MethodCallExpression BeginLine='8' EndLine='8'
        // FullMethodName='System.debug'>
        //                    <ReferenceExpression BeginLine='8' EndLine='8' Image='System' />
        //                    <LiteralExpression BeginLine='8' EndLine='8' Image='Goodbye' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        BlockStatementVertex blockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        IfElseBlockStatementVertex ifElseBlockStatement =
                TestUtil.getVertexOnLine(g, IfElseBlockStatementVertex.class, 3);
        IfBlockStatementVertex ifStatement =
                TestUtil.getVertexOnLine(g, IfElseBlockStatementVertex.class, 3).getChild(0);
        BlockStatementVertex standardConditionBlockStatementLine3 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3);
        BlockStatementVertex standardConditionBlockStatementLine5 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 5);
        BlockStatementVertex standardConditionBlockStatementLine7 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 7);
        IfBlockStatementVertex elseIfStatement =
                TestUtil.getVertexOnLine(g, IfElseBlockStatementVertex.class, 3).getChild(1);
        StandardConditionVertex standardConditionLine3 =
                TestUtil.getVertexOnLine(g, StandardConditionVertex.class, 3);
        StandardConditionVertex standardConditionLine5 =
                TestUtil.getVertexOnLine(g, StandardConditionVertex.class, 5);

        ExpressionStatementVertex expressionStatementLine4 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 4);
        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);
        ExpressionStatementVertex expressionStatementLine8 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 8);

        assertPathEdgesExist(
                Schema.CFG_PATH,
                Pair.of(blockStatement, ifElseBlockStatement),
                Pair.of(ifElseBlockStatement, ifStatement),
                Pair.of(ifStatement, standardConditionLine3),
                Pair.of(standardConditionLine3, standardConditionBlockStatementLine3),
                Pair.of(standardConditionBlockStatementLine3, expressionStatementLine4),
                Pair.of(standardConditionLine3, elseIfStatement),
                Pair.of(elseIfStatement, standardConditionLine5),
                Pair.of(standardConditionLine5, standardConditionBlockStatementLine5),
                Pair.of(standardConditionBlockStatementLine5, expressionStatementLine6),
                Pair.of(standardConditionLine5, standardConditionBlockStatementLine7),
                Pair.of(standardConditionBlockStatementLine7, expressionStatementLine8));

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(6, 8, 8));

        ApexPath expressionLine4Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine4);
        MatcherAssert.assertThat(
                expressionLine4Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));

        ApexPath expressionLine6Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine6);
        MatcherAssert.assertThat(
                expressionLine6Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                expressionLine6Path.verticesInCurrentMethod().get(5),
                instanceOf(StandardConditionVertex.Positive.class));

        ApexPath expressionLine8Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine8);
        MatcherAssert.assertThat(
                expressionLine8Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                expressionLine8Path.verticesInCurrentMethod().get(5),
                instanceOf(StandardConditionVertex.Negative.class));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // System.debug('Hello');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 4);

        // System.debug('X Is 10');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 6);

        // System.debug('GoodBye');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 8);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithNestedIfElses() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (log) {\n"
                        + "           if (x == 10) {\n"
                        + "               System.debug('X Is 10');\n"
                        + "           } else {\n"
                        + "               System.debug('X Is Not 10');\n"
                        + "           }\n"
                        + "       } else {\n"
                        + "           System.debug('Not Logged');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='12'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='3'>
        //        <IfBlockStatement BeginLine='3' EndLine='3'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='9'>
        //                <IfElseBlockStatement BeginLine='4' EndLine='4'>
        //                    <IfBlockStatement BeginLine='4' EndLine='4'>
        //                        <StandardCondition BeginLine='4' EndLine='4'>
        //                            <BooleanExpression BeginLine='4' EndLine='4'>
        //                                <VariableExpression BeginLine='4' EndLine='4' Image='x'>
        //                                    <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
        //                                </VariableExpression>
        //                                <LiteralExpression BeginLine='4' EndLine='4' Image='10' />
        //                            </BooleanExpression>
        //                        </StandardCondition>
        //                        <BlockStatement BeginLine='4' EndLine='6'>
        //                            <ExpressionStatement BeginLine='5' EndLine='5'>
        //                                <MethodCallExpression BeginLine='5' EndLine='5'
        // FullMethodName='System.debug'>
        //                                    <ReferenceExpression BeginLine='5' EndLine='5'
        // Image='System' />
        //                                    <LiteralExpression BeginLine='5' EndLine='5' Image='X
        // Is 10' />
        //                                </MethodCallExpression>
        //                            </ExpressionStatement>
        //                        </BlockStatement>
        //                    </IfBlockStatement>
        //                    <BlockStatement BeginLine='6' EndLine='8'>
        //                        <ExpressionStatement BeginLine='7' EndLine='7'>
        //                            <MethodCallExpression BeginLine='7' EndLine='7'
        // FullMethodName='System.debug'>
        //                                <ReferenceExpression BeginLine='7' EndLine='7'
        // Image='System' />
        //                                <LiteralExpression BeginLine='7' EndLine='7' Image='X Is
        // Not 10' />
        //                            </MethodCallExpression>
        //                        </ExpressionStatement>
        //                    </BlockStatement>
        //                </IfElseBlockStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='9' EndLine='11'>
        //            <ExpressionStatement BeginLine='10' EndLine='10'>
        //                <MethodCallExpression BeginLine='10' EndLine='10'
        // FullMethodName='System.debug'>
        //                    <ReferenceExpression BeginLine='10' EndLine='10' Image='System' />
        //                    <LiteralExpression BeginLine='10' EndLine='10' Image='Not Logged' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        ExpressionStatementVertex expressionStatementLine5 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 5);
        ExpressionStatementVertex expressionStatementLine7 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 7);
        ExpressionStatementVertex expressionStatementLine10 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 10);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(6, 10, 10));

        ApexPath expressionLine5Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine5);
        MatcherAssert.assertThat(
                expressionLine5Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                expressionLine5Path.verticesInCurrentMethod().get(7),
                instanceOf(StandardConditionVertex.Positive.class));

        ApexPath expressionLine7Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine7);
        MatcherAssert.assertThat(
                expressionLine7Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                expressionLine7Path.verticesInCurrentMethod().get(7),
                instanceOf(StandardConditionVertex.Negative.class));

        ApexPath expressionLine10Path =
                TestUtil.getPathEndingAtVertex(paths, expressionStatementLine10);
        MatcherAssert.assertThat(
                expressionLine10Path.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // System.debug('X is 10');
        assertEndScopes(BLOCK_IF_BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 5);

        // System.debug('X Is Not 10');
        assertEndScopes(BLOCK_IF_BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 7);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK, ExpressionStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithExpressionBeforeAndAfterIf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       System.debug('Before');\n"
                        + "       if (log) {\n"
                        + "           System.debug('Hello');\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='12'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='3'>
        //        <IfBlockStatement BeginLine='3' EndLine='3'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='9'>
        //                <IfElseBlockStatement BeginLine='4' EndLine='4'>
        //                    <IfBlockStatement BeginLine='4' EndLine='4'>
        //                        <StandardCondition BeginLine='4' EndLine='4'>
        //                            <BooleanExpression BeginLine='4' EndLine='4'>
        //                                <VariableExpression BeginLine='4' EndLine='4' Image='x'>
        //                                    <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
        //                                </VariableExpression>
        //                                <LiteralExpression BeginLine='4' EndLine='4' Image='10' />
        //                            </BooleanExpression>
        //                        </StandardCondition>
        //                        <BlockStatement BeginLine='4' EndLine='6'>
        //                            <ExpressionStatement BeginLine='5' EndLine='5'>
        //                                <MethodCallExpression BeginLine='5' EndLine='5'
        // FullMethodName='System.debug'>
        //                                    <ReferenceExpression BeginLine='5' EndLine='5'
        // Image='System' />
        //                                    <LiteralExpression BeginLine='5' EndLine='5' Image='X
        // Is 10' />
        //                                </MethodCallExpression>
        //                            </ExpressionStatement>
        //                        </BlockStatement>
        //                    </IfBlockStatement>
        //                    <BlockStatement BeginLine='6' EndLine='8'>
        //                        <ExpressionStatement BeginLine='7' EndLine='7'>
        //                            <MethodCallExpression BeginLine='7' EndLine='7'
        // FullMethodName='System.debug'>
        //                                <ReferenceExpression BeginLine='7' EndLine='7'
        // Image='System' />
        //                                <LiteralExpression BeginLine='7' EndLine='7' Image='X Is
        // Not 10' />
        //                            </MethodCallExpression>
        //                        </ExpressionStatement>
        //                    </BlockStatement>
        //                </IfElseBlockStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='9' EndLine='11'>
        //            <ExpressionStatement BeginLine='10' EndLine='10'>
        //                <MethodCallExpression BeginLine='10' EndLine='10'
        // FullMethodName='System.debug'>
        //                    <ReferenceExpression BeginLine='10' EndLine='10' Image='System' />
        //                    <LiteralExpression BeginLine='10' EndLine='10' Image='Not Logged' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        // (4 edges)
        // BlockStatement->ExpressionStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (3 edges)
        // StandardCondition->PositiveBlockStatement->ExpressionStatement(Line5)->ExpressionStatement(Line7)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line7)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(9));

        ExpressionStatementVertex expressionStatementLine3 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 3);
        ExpressionStatementVertex expressionStatementLine5 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 5);
        ExpressionStatementVertex expressionStatementLine7 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 7);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(7, 8));

        ApexPath positivePath = TestUtil.getPathOfSize(paths, 8);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(1), equalTo(expressionStatementLine3));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(4),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(6), equalTo(expressionStatementLine5));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(7), equalTo(expressionStatementLine7));

        ApexPath negativePath = TestUtil.getPathOfSize(paths, 7);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(1), equalTo(expressionStatementLine3));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(4),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(6), equalTo(expressionStatementLine7));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // System.debug('Hello');
        assertEndScopes(BLOCK_IF, ExpressionStatementVertex.class, 5);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 4, 4);

        // System.debug('After');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 7);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithInnerIfExpressionBeforeAndAfterIf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       System.debug('Before');\n"
                        + "       if (log1) {\n"
                        + "           System.debug('Hello-1');\n"
                        + "           if (log2) {\n"
                        + "               System.debug('Hello-2');\n"
                        + "           }\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' EndLine='12'>
        //    <IfElseBlockStatement BeginLine='3' EndLine='3'>
        //        <IfBlockStatement BeginLine='3' EndLine='3'>
        //            <StandardCondition BeginLine='3' EndLine='3'>
        //                <VariableExpression BeginLine='3' EndLine='3' Image='log'>
        //                    <EmptyReferenceExpression BeginLine='3' EndLine='3'/>
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' EndLine='9'>
        //                <IfElseBlockStatement BeginLine='4' EndLine='4'>
        //                    <IfBlockStatement BeginLine='4' EndLine='4'>
        //                        <StandardCondition BeginLine='4' EndLine='4'>
        //                            <BooleanExpression BeginLine='4' EndLine='4'>
        //                                <VariableExpression BeginLine='4' EndLine='4' Image='x'>
        //                                    <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
        //                                </VariableExpression>
        //                                <LiteralExpression BeginLine='4' EndLine='4' Image='10' />
        //                            </BooleanExpression>
        //                        </StandardCondition>
        //                        <BlockStatement BeginLine='4' EndLine='6'>
        //                            <ExpressionStatement BeginLine='5' EndLine='5'>
        //                                <MethodCallExpression BeginLine='5' EndLine='5'
        // FullMethodName='System.debug'>
        //                                    <ReferenceExpression BeginLine='5' EndLine='5'
        // Image='System' />
        //                                    <LiteralExpression BeginLine='5' EndLine='5' Image='X
        // Is 10' />
        //                                </MethodCallExpression>
        //                            </ExpressionStatement>
        //                        </BlockStatement>
        //                    </IfBlockStatement>
        //                    <BlockStatement BeginLine='6' EndLine='8'>
        //                        <ExpressionStatement BeginLine='7' EndLine='7'>
        //                            <MethodCallExpression BeginLine='7' EndLine='7'
        // FullMethodName='System.debug'>
        //                                <ReferenceExpression BeginLine='7' EndLine='7'
        // Image='System' />
        //                                <LiteralExpression BeginLine='7' EndLine='7' Image='X Is
        // Not 10' />
        //                            </MethodCallExpression>
        //                        </ExpressionStatement>
        //                    </BlockStatement>
        //                </IfElseBlockStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='9' EndLine='11'>
        //            <ExpressionStatement BeginLine='10' EndLine='10'>
        //                <MethodCallExpression BeginLine='10' EndLine='10'
        // FullMethodName='System.debug'>
        //                    <ReferenceExpression BeginLine='10' EndLine='10' Image='System' />
        //                    <LiteralExpression BeginLine='10' EndLine='10' Image='Not Logged' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //    </IfElseBlockStatement>
        // </BlockStatement>

        // (4 edges)
        // BlockStatement->ExpressionStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition(log1)
        // (5 edges)
        // StandardCondition(log1)->PositiveBlockStatement->ExpressionStatement(Line5)->IfElseBlockStatement->IfBlockStatement->StandardCondition(log2)
        // (2 edges) StandardCondition(log1)->NegativeBlockStatement->ExpressionStatement(Line10)
        // (3 edges)
        // StandardCondition(log2)->PositiveBlockStatement->ExpressionStatement(Line7)->ExpressionStatement(Line10)
        // (2 edges) StandardCondition(log2)->NegativeBlockStatement->ExpressionStatement(Line10)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(16));

        ExpressionStatementVertex expressionStatementLine3 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 3);
        ExpressionStatementVertex expressionStatementLine5 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 5);
        ExpressionStatementVertex expressionStatementLine7 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 7);
        ExpressionStatementVertex expressionStatementLine10 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 10);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(7, 12, 13));

        ApexPath twoSystemDebugs = TestUtil.getPathOfSize(paths, 7);
        MatcherAssert.assertThat(
                twoSystemDebugs.verticesInCurrentMethod().get(1),
                equalTo(expressionStatementLine3));
        MatcherAssert.assertThat(
                twoSystemDebugs.verticesInCurrentMethod().get(4),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                twoSystemDebugs.verticesInCurrentMethod().get(6),
                equalTo(expressionStatementLine10));

        ApexPath threeSystemDebugs = TestUtil.getPathOfSize(paths, 12);
        MatcherAssert.assertThat(
                threeSystemDebugs.verticesInCurrentMethod().get(1),
                equalTo(expressionStatementLine3));
        MatcherAssert.assertThat(
                threeSystemDebugs.verticesInCurrentMethod().get(4),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                threeSystemDebugs.verticesInCurrentMethod().get(6),
                equalTo(expressionStatementLine5));
        MatcherAssert.assertThat(
                threeSystemDebugs.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                threeSystemDebugs.verticesInCurrentMethod().get(11),
                equalTo(expressionStatementLine10));

        ApexPath fourSystemDebugs = TestUtil.getPathOfSize(paths, 13);
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(1),
                equalTo(expressionStatementLine3));
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(4),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(6),
                equalTo(expressionStatementLine5));
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(11),
                equalTo(expressionStatementLine7));
        MatcherAssert.assertThat(
                fourSystemDebugs.verticesInCurrentMethod().get(12),
                equalTo(expressionStatementLine10));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(4)));

        // System.debug('Hello-2'). The next vertex to execute is System.debug('After'). This
        // requires popping two scopes
        assertEndScopes(BLOCK_IF_BLOCK_IF, ExpressionStatementVertex.class, 7);

        // Inner Implicit else. System.debug('Hello-2'). The next vertex to execute is
        // System.debug('After'). This requires popping two scopes
        assertEndScopes(BLOCK_IF_BLOCK_IF, BlockStatementVertex.class, 6, 6);

        // Implicit outer else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 4, 4);

        // System.debug('After');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithForEach() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (String fieldToCheck : fieldsToCheck) {\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (9 edges) BlockStatement->VariableDeclarationStatements->ForEachStatement
        //
        // ->VariableExpression->VariableDeclarationStatements->VariableExpression->BlockStatement
        //          ->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ExpressionStatement(Line6)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line8)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(13));

        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);
        ExpressionStatementVertex expressionStatementLine8 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 8);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(12, 12));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine6);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine6));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine8)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(11), equalTo(expressionStatementLine6));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine8);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine8));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine6)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(11), equalTo(expressionStatementLine8));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FOREACH_BLOCK, ExpressionStatementVertex.class, 6);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FOREACH_BLOCK, ExpressionStatementVertex.class, 8);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithForLoop() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i=0; i<fieldsToCheck.length; i++) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (10 edges) BlockStatement->VariableDeclarationStatements->ForLoopStatement
        //
        // ->VariableDeclarationStatements->StandardCondition->PostfixExpression->BlockStatement
        //
        // ->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ExpressionStatement(Line7)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line9)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(14));

        ExpressionStatementVertex expressionStatementLine7 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 7);
        ExpressionStatementVertex expressionStatementLine9 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 9);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(13, 13));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine7);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine7));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine9)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine7));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine9);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine9));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine7)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine9));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 7);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 9);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    /**
     * Tests the case where the for loop doesn't contain comparisons or increments. In the real
     * world the inner loop would do something to cause the loop to break.
     */
    @Test
    public void testMethodWithForLoopNoInitializerOrStandardConditionOrIncrementer() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       Integer i=0;\n"
                        + "       for (;;) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (8 edges) BlockStatement->VariableDeclarationStatements->ForLoopStatement
        //          ->BlockStatement
        //
        // ->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ExpressionStatement(Line7)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line9)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(12));

        ExpressionStatementVertex expressionStatementLine8 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 8);
        ExpressionStatementVertex expressionStatementLine10 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 10);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(11, 11));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine8);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine8));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine10)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(8),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(10), equalTo(expressionStatementLine8));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine10);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine10));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine8)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(8),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(10), equalTo(expressionStatementLine10));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 8);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    /** Tests the case where the for loop doesn't contain an initializer. */
    @Test
    public void testMethodWithForLoopNoInitializer() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       Integer i=0;\n"
                        + "       for (; i<10; i++) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (10 edges)
        // BlockStatement->VariableDeclarationStatements->ForLoopStatement->StandardCondition->PostfixExpression
        //          ->BlockStatement
        //
        // ->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ExpressionStatement(Line7)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line9)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(14));

        ExpressionStatementVertex expressionStatementLine8 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 8);
        ExpressionStatementVertex expressionStatementLine10 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 10);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(13, 13));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine8);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine8));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine10)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine8));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine10);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine10));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine8)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine10));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 8);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    /**
     * Tests the case where the for loop doesn't contain an initializer. In the real world the inner
     * loop would do something to cause the loop to break.
     */
    @Test
    public void testMethodWithForLoopNoInitializerOrStandardCondition() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       Integer i=0;\n"
                        + "       for (;; i++) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (9 edges)
        // BlockStatement->VariableDeclarationStatements->ForLoopStatement->PostfixExpression
        //          ->BlockStatement
        //
        // ->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ExpressionStatement(Line7)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line9)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(13));

        ExpressionStatementVertex expressionStatementLine8 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 8);
        ExpressionStatementVertex expressionStatementLine10 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 10);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(12, 12));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine8);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine8));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine10)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(11), equalTo(expressionStatementLine8));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine10);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine10));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine8)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(9),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(11), equalTo(expressionStatementLine10));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 8);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP_BLOCK, ExpressionStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithForLoopExpressionBeforeAndAfter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i=0; i<fieldsToCheck.length; i++) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (10 edges) BlockStatement->VariableDeclarationStatements->ForLoopStatement
        //
        // ->VariableDeclarationStatements->StandardCondition->PostfixExpression->BlockStatement
        //
        // ->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (3 edges)
        // StandardCondition->PositiveBlockStatement->ExpressionStatement(Line7)->ExpressionStatement(Line12)
        // (3 edges)
        // StandardCondition->NegativeBlockStatement->ExpressionStatement(Line9)->ExpressionStatement(Line12)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(16));

        ExpressionStatementVertex expressionStatementLine7 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 7);
        ExpressionStatementVertex expressionStatementLine9 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 9);
        ExpressionStatementVertex expressionStatementLine12 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 12);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), containsInAnyOrder(14, 14));

        ApexPath positivePath =
                paths.stream()
                        .filter(
                                p ->
                                        p.verticesInCurrentMethod()
                                                        .contains(expressionStatementLine7)
                                                && !p.verticesInCurrentMethod()
                                                        .contains(expressionStatementLine9))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine7));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine9)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(expressionStatementLine12));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine7));

        ApexPath negativePath =
                paths.stream()
                        .filter(
                                p ->
                                        p.verticesInCurrentMethod()
                                                        .contains(expressionStatementLine9)
                                                && !p.verticesInCurrentMethod()
                                                        .contains(expressionStatementLine7))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine9));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine7)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine12));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(10),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(12), equalTo(expressionStatementLine9));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP, ExpressionStatementVertex.class, 7);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF_BLOCK_FORLOOP, ExpressionStatementVertex.class, 9);

        // System.debug('After');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 12);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithForLoopExpressionBeforeAndAfterEndsForScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       for (Integer i=0; i<fieldsToCheck.length; i++) {\n"
                        + "           String fieldToCheck = fieldsToCheck[i];\n"
                        + "           if (log) {\n"
                        + "               System.debug(fieldToCheck);\n"
                        + "           } else {\n"
                        + "               System.debug('Not Logged');\n"
                        + "           }\n"
                        + "           System.debug('Ends For Scope');\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(4)));

        // System.debug(fieldToCheck);
        assertEndScopes(BLOCK_IF, ExpressionStatementVertex.class, 7);

        // System.debug('Not Logged');
        assertEndScopes(BLOCK_IF, ExpressionStatementVertex.class, 9);

        // System.debug('Ends For Scope');
        assertEndScopes(BLOCK_FORLOOP, ExpressionStatementVertex.class, 11);

        // System.debug('After');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 13);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithEarlyReturn() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (exception) {\n"
                        + "           return;\n"
                        + "       }\n"
                        + "       System.debug('Hello');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='7' Image='' RealLoc='true'>
        //    <IfElseBlockStatement BeginLine='3' DefiningType='MyClass' ElseStatement='false'
        // EndLine='3' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //                <VariableExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='exception' RealLoc='true'>
        //                    <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true'>
        //                <ReturnStatement BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true' />
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        //    <ExpressionStatement BeginLine='6' DefiningType='MyClass' EndLine='6' Image=''
        // RealLoc='true'>
        //        <MethodCallExpression BeginLine='6' DefiningType='MyClass' EndLine='6'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //            <ReferenceExpression BeginLine='6' Context='' DefiningType='MyClass'
        // EndLine='6' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //            <LiteralExpression BeginLine='6' DefiningType='MyClass' EndLine='6'
        // Image='Hello' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //        </MethodCallExpression>
        //    </ExpressionStatement>
        // </BlockStatement>

        // (3 edges) BlockStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ReturnStatement(Line4)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line6)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(7));

        ReturnStatementVertex returnStatementVertexLine4 =
                TestUtil.getVertexOnLine(g, ReturnStatementVertex.class, 4);
        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        ApexPath positivePath = TestUtil.getPathEndingAtVertex(paths, returnStatementVertexLine4);
        MatcherAssert.assertThat(positivePath.verticesInCurrentMethod(), hasSize(equalTo(6)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), hasItem(returnStatementVertexLine4));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod(), not(hasItem(expressionStatementLine6)));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Positive.class));
        MatcherAssert.assertThat(
                positivePath.verticesInCurrentMethod().get(5), equalTo(returnStatementVertexLine4));

        ApexPath negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine6);
        MatcherAssert.assertThat(negativePath.verticesInCurrentMethod(), hasSize(equalTo(6)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine6));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(returnStatementVertexLine4)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(5), equalTo(expressionStatementLine6));

        paths = ApexPathUtil.getReversePaths(g, expressionStatementLine6);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        negativePath = paths.get(0);
        MatcherAssert.assertThat(negativePath.verticesInCurrentMethod(), hasSize(equalTo(6)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine6));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(returnStatementVertexLine4)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(5), equalTo(expressionStatementLine6));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // return
        assertEndScopes(BLOCK_IF_BLOCK, ReturnStatementVertex.class, 4);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 3, 3);

        // System.debug('Hello');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 6);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithException() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       if (exception) {\n"
                        + "           throw new Exception();\n"
                        + "       }\n"
                        + "       System.debug('Hello');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='7' Image='' RealLoc='true'>
        //    <IfElseBlockStatement BeginLine='3' DefiningType='MyClass' ElseStatement='false'
        // EndLine='3' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //                <VariableExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='exception' RealLoc='true'>
        //                    <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //                </VariableExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true'>
        //                <ThrowStatement BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true'>
        //                    <NewObjectExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true' Type='Exception' />
        //                </ThrowStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        //    <ExpressionStatement BeginLine='6' DefiningType='MyClass' EndLine='6' Image=''
        // RealLoc='true'>
        //        <MethodCallExpression BeginLine='6' DefiningType='MyClass' EndLine='6'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //            <ReferenceExpression BeginLine='6' Context='' DefiningType='MyClass'
        // EndLine='6' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //            <LiteralExpression BeginLine='6' DefiningType='MyClass' EndLine='6'
        // Image='Hello' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //        </MethodCallExpression>
        //    </ExpressionStatement>
        // </BlockStatement>
        //

        // (3 edges) BlockStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ThrowStatement(Line4)
        // (2 edges) StandardCondition->NegativeBlockStatement->ExpressionStatement(Line6)
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(7));

        ThrowStatementVertex throwStatementVertexLine4 =
                TestUtil.getVertexOnLine(g, ThrowStatementVertex.class, 4);
        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);
        BlockStatementVertex emptyBlockStatement =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3, 3);

        List<ApexPath> paths;

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath negativePath;
        negativePath = TestUtil.getPathEndingAtVertex(paths, expressionStatementLine6);
        MatcherAssert.assertThat(negativePath.verticesInCurrentMethod(), hasSize(equalTo(6)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine6));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(throwStatementVertexLine4)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(4), equalTo(emptyBlockStatement));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(5), equalTo(expressionStatementLine6));

        paths = ApexPathUtil.getReversePaths(g, expressionStatementLine6);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        negativePath = paths.get(0);
        MatcherAssert.assertThat(negativePath.verticesInCurrentMethod(), hasSize(equalTo(6)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), hasItem(expressionStatementLine6));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod(), not(hasItem(throwStatementVertexLine4)));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(3),
                instanceOf(StandardConditionVertex.Negative.class));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(4), equalTo(emptyBlockStatement));
        MatcherAssert.assertThat(
                negativePath.verticesInCurrentMethod().get(5), equalTo(expressionStatementLine6));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // throw
        assertEndScopes(BLOCK_IF_BLOCK, ThrowStatementVertex.class, 4);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 3, 3);

        // System.debug('Hello');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 6);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMultipleEarlyReturns() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (!m.get('Name').getDescribe().isCreateable()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + "        if (!m.get('Phone').getDescribe().isCreateable()) {\n"
                        + "           return;\n"
                        + "        }\n"
                        + "        insert new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='11' Image=''
        // RealLoc='true'>
        //    <VariableDeclarationStatements BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='' RealLoc='true'>
        //        <ModifierNode Abstract='false' BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Final='false' Global='false' Image='' InheritedSharing='false' Modifiers='0'
        // Override='false' Private='false' Protected='false' Public='false' RealLoc='false'
        // Static='false' Test='false' TestOrTestSetup='false' Transient='false' WebService='false'
        // WithSharing='false' WithoutSharing='false' />
        //        <VariableDeclaration BeginLine='3' DefiningType='MyClass' EndLine='3' Image='m'
        // RealLoc='true' Type='Map&amp;lt;String,Schema.SObjectField>'>
        //            <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='Schema.SObjectType.Account.fields.getMap' Image='' MethodName='getMap'
        // RealLoc='true'>
        //                <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='Schema' Names='[Schema, SObjectType, Account, fields]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //            </MethodCallExpression>
        //            <VariableExpression BeginLine='3' DefiningType='MyClass' EndLine='3' Image='m'
        // RealLoc='true'>
        //                <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //            </VariableExpression>
        //        </VariableDeclaration>
        //    </VariableDeclarationStatements>
        //    <IfElseBlockStatement BeginLine='4' DefiningType='MyClass' ElseStatement='false'
        // EndLine='4' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true'>
        //                <PrefixExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' Operator='!' RealLoc='true'>
        //                    <MethodCallExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // FullMethodName='isCreateable' Image='' MethodName='isCreateable' RealLoc='true'>
        //                        <ReferenceExpression BeginLine='4' Context=''
        // DefiningType='MyClass' EndLine='4' Image='' Names='[]' RealLoc='false'
        // ReferenceType='METHOD' SafeNav='false'>
        //                            <MethodCallExpression BeginLine='4' DefiningType='MyClass'
        // EndLine='4' FullMethodName='getDescribe' Image='' MethodName='getDescribe'
        // RealLoc='true'>
        //                                <ReferenceExpression BeginLine='4' Context=''
        // DefiningType='MyClass' EndLine='4' Image='' Names='[]' RealLoc='false'
        // ReferenceType='METHOD' SafeNav='false'>
        //                                    <MethodCallExpression BeginLine='4'
        // DefiningType='MyClass' EndLine='4' FullMethodName='m.get' Image='' MethodName='get'
        // RealLoc='true'>
        //                                        <ReferenceExpression BeginLine='4' Context=''
        // DefiningType='MyClass' EndLine='4' Image='m' Names='[m]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                                        <LiteralExpression BeginLine='4'
        // DefiningType='MyClass' EndLine='4' Image='Name' LiteralType='STRING' Long='false' Name=''
        // Null='false' RealLoc='true' />
        //                                    </MethodCallExpression>
        //                                </ReferenceExpression>
        //                            </MethodCallExpression>
        //                        </ReferenceExpression>
        //                    </MethodCallExpression>
        //                </PrefixExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='6' Image=''
        // RealLoc='true'>
        //                <ReturnStatement BeginLine='5' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true' />
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        //    <IfElseBlockStatement BeginLine='7' DefiningType='MyClass' ElseStatement='false'
        // EndLine='7' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='7' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='7' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true'>
        //                <PrefixExpression BeginLine='7' DefiningType='MyClass' EndLine='7'
        // Image='' Operator='!' RealLoc='true'>
        //                    <MethodCallExpression BeginLine='7' DefiningType='MyClass' EndLine='7'
        // FullMethodName='isCreateable' Image='' MethodName='isCreateable' RealLoc='true'>
        //                        <ReferenceExpression BeginLine='7' Context=''
        // DefiningType='MyClass' EndLine='7' Image='' Names='[]' RealLoc='false'
        // ReferenceType='METHOD' SafeNav='false'>
        //                            <MethodCallExpression BeginLine='7' DefiningType='MyClass'
        // EndLine='7' FullMethodName='getDescribe' Image='' MethodName='getDescribe'
        // RealLoc='true'>
        //                                <ReferenceExpression BeginLine='7' Context=''
        // DefiningType='MyClass' EndLine='7' Image='' Names='[]' RealLoc='false'
        // ReferenceType='METHOD' SafeNav='false'>
        //                                    <MethodCallExpression BeginLine='7'
        // DefiningType='MyClass' EndLine='7' FullMethodName='m.get' Image='' MethodName='get'
        // RealLoc='true'>
        //                                        <ReferenceExpression BeginLine='7' Context=''
        // DefiningType='MyClass' EndLine='7' Image='m' Names='[m]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                                        <LiteralExpression BeginLine='7'
        // DefiningType='MyClass' EndLine='7' Image='Phone' LiteralType='STRING' Long='false'
        // Name='' Null='false' RealLoc='true' />
        //                                    </MethodCallExpression>
        //                                </ReferenceExpression>
        //                            </MethodCallExpression>
        //                        </ReferenceExpression>
        //                    </MethodCallExpression>
        //                </PrefixExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='7' DefiningType='MyClass' EndLine='9' Image=''
        // RealLoc='true'>
        //                <ReturnStatement BeginLine='8' DefiningType='MyClass' EndLine='8' Image=''
        // RealLoc='true' />
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='7' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        //    <DmlInsertStatement BeginLine='10' DefiningType='MyClass' EndLine='10' Image=''
        // RealLoc='true'>
        //        <NewKeyValueObjectExpression BeginLine='10' DefiningType='MyClass' EndLine='10'
        // Image='' ParameterCount='2' RealLoc='true' Type='Account'>
        //            <LiteralExpression BeginLine='10' DefiningType='MyClass' EndLine='10'
        // Image='Acme Inc.' LiteralType='STRING' Long='false' Name='Name' Null='false'
        // RealLoc='true' />
        //            <LiteralExpression BeginLine='10' DefiningType='MyClass' EndLine='10'
        // Image='415-555-1212' LiteralType='STRING' Long='false' Name='Phone' Null='false'
        // RealLoc='true' />
        //        </NewKeyValueObjectExpression>
        //    </DmlInsertStatement>
        // </BlockStatement>
        // (4 edges)
        // BlockStatement->VariableDeclarationStatements->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ReturnStatementLine5
        // (1 edges) StandardCondition->EmptyBlockStatement
        // (3 edges) EmptyBlockStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->PositiveBlockStatement->ReturnStatementLine7
        // (2 edges) StandardCondition->EmptyBlockStatement->DmlInsertStatement
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(14));

        List<ApexPath> paths;
        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));

        DmlInsertStatementVertex dmlInsertStatementVertex =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 10);
        paths = ApexPathUtil.getReversePaths(g, dmlInsertStatementVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(5)));

        // return
        assertEndScopes(BLOCK_IF_BLOCK, ReturnStatementVertex.class, 5);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 4, 4);

        // return
        assertEndScopes(BLOCK_IF_BLOCK, ReturnStatementVertex.class, 8);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 7, 7);

        // insert
        assertEndScopes(BLOCK, DmlInsertStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testInsertGuardedByExceptionInOtherMethodWithReturn() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "        verifyCreateable();\n"
                    + "        insert new Account(Name = 'Acme Inc.');\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        if (Schema.sObjectType.Account.fields.Name.isCreateable()) {\n"
                    + "           return;\n"
                    + "        }\n"
                    + "        throw new MyException();\n"
                    + "    }\n"
                    + "}\n"
        };

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        List<ApexPath> paths;
        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        DmlInsertStatementVertex dmlInsertStatementVertex =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 4);
        paths = ApexPathUtil.getReversePaths(g, dmlInsertStatementVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(4)));

        // insert
        assertEndScopes(BLOCK, DmlInsertStatementVertex.class, 4);

        // return
        assertEndScopes(BLOCK_IF_BLOCK, ReturnStatementVertex.class, 8);

        // Implicit else
        assertEndScopes(BLOCK_IF, BlockStatementVertex.class, 7, 7);

        // insert
        assertEndScopes(BLOCK, ThrowStatementVertex.class, 10);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithSingleTryCatch() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       try {\n"
                        + "           System.debug('Hello');\n"
                        + "       } catch (Exception ex) {\n"
                        + "           System.debug(ex);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='8' Image='' RealLoc='true'>
        //    <TryCatchFinallyBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='' RealLoc='true'>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true'>
        //            <ExpressionStatement BeginLine='4' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true'>
        //                <MethodCallExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                    <ReferenceExpression BeginLine='4' Context='' DefiningType='MyClass'
        // EndLine='4' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //                    <LiteralExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='Hello' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //        <CatchBlockStatement BeginLine='5' DefiningType='MyClass' EndLine='7'
        // ExceptionType='Exception' Image='' RealLoc='true' VariableName='ex'>
        //            <BlockStatement BeginLine='5' DefiningType='MyClass' EndLine='7' Image=''
        // RealLoc='true'>
        //                <ExpressionStatement BeginLine='6' DefiningType='MyClass' EndLine='6'
        // Image='' RealLoc='true'>
        //                    <MethodCallExpression BeginLine='6' DefiningType='MyClass' EndLine='6'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                        <ReferenceExpression BeginLine='6' Context=''
        // DefiningType='MyClass' EndLine='6' Image='System' Names='[System]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                        <VariableExpression BeginLine='6' DefiningType='MyClass'
        // EndLine='6' Image='ex' RealLoc='true'>
        //                            <EmptyReferenceExpression BeginLine='6' DefiningType=''
        // EndLine='6' Image='' RealLoc='false' />
        //                        </VariableExpression>
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </CatchBlockStatement>
        //    </TryCatchFinallyBlockStatement>
        // </BlockStatement>

        // (1 edges) BlockStatement->TryCatchFinallyBlockStatement
        // (2 edges) TryCatchFinallyBlockStatement->BlockStatement(Try)->ExpressionStatement
        // (3 edges)
        // TryCatchFinallyBlockStatement->CatchBlockStatement->BlockStatement(Catch)->ExpressionStatement
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(6));

        BlockStatementVertex blockStatementLine2 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        TryCatchFinallyBlockStatementVertex tryCatchFinallyBlockStatement =
                TestUtil.getVertexOnLine(g, TryCatchFinallyBlockStatementVertex.class, 3);
        BlockStatementVertex blockStatementLine3 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 3);
        ExpressionStatementVertex expressionStatementLine4 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 4);
        CatchBlockStatementVertex catchBlockStatement =
                TestUtil.getVertexOnLine(g, CatchBlockStatementVertex.class, 5);
        BlockStatementVertex blockStatementLine5 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 5);
        ExpressionStatementVertex expressionStatementLine6 =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 6);

        assertPathEdgesExist(
                Schema.CFG_PATH,
                Pair.of(blockStatementLine2, tryCatchFinallyBlockStatement),
                Pair.of(tryCatchFinallyBlockStatement, blockStatementLine3),
                Pair.of(blockStatementLine3, expressionStatementLine4),
                Pair.of(tryCatchFinallyBlockStatement, catchBlockStatement),
                Pair.of(catchBlockStatement, blockStatementLine5),
                Pair.of(blockStatementLine5, expressionStatementLine6));

        MethodVertex methodVertex = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        MatcherAssert.assertThat(TestUtil.getPathSizes(paths), contains(4, 5));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(2)));

        // System.debug('Hello');
        assertEndScopes(
                new String[] {
                    NodeType.BLOCK_STATEMENT,
                    NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT,
                    NodeType.BLOCK_STATEMENT
                },
                ExpressionStatementVertex.class,
                4);

        // System.debug(ex);
        assertEndScopes(
                new String[] {
                    NodeType.BLOCK_STATEMENT,
                    NodeType.CATCH_BLOCK_STATEMENT,
                    NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT,
                    NodeType.BLOCK_STATEMENT
                },
                ExpressionStatementVertex.class,
                6);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testMethodWithSingleTryCatchAndExpressionBeforeAndAfter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething(boolean log) {\n"
                        + "       System.debug('Before');\n"
                        + "       try {\n"
                        + "           System.debug('Hello');\n"
                        + "       } catch (Exception ex) {\n"
                        + "           System.debug(ex);\n"
                        + "       }\n"
                        + "       System.debug('After');\n"
                        + "   }\n"
                        + "}";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='10' Image=''
        // RealLoc='true'>
        //    <ExpressionStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //        <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //            <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //            <LiteralExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='Before' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //        </MethodCallExpression>
        //    </ExpressionStatement>
        //    <TryCatchFinallyBlockStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true'>
        //        <BlockStatement BeginLine='4' DefiningType='MyClass' EndLine='6' Image=''
        // RealLoc='true'>
        //            <ExpressionStatement BeginLine='5' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true'>
        //                <MethodCallExpression BeginLine='5' DefiningType='MyClass' EndLine='5'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                    <ReferenceExpression BeginLine='5' Context='' DefiningType='MyClass'
        // EndLine='5' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //                    <LiteralExpression BeginLine='5' DefiningType='MyClass' EndLine='5'
        // Image='Hello' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //                </MethodCallExpression>
        //            </ExpressionStatement>
        //        </BlockStatement>
        //        <CatchBlockStatement BeginLine='6' DefiningType='MyClass' EndLine='8'
        // ExceptionType='Exception' Image='' RealLoc='true' VariableName='ex'>
        //            <BlockStatement BeginLine='6' DefiningType='MyClass' EndLine='8' Image=''
        // RealLoc='true'>
        //                <ExpressionStatement BeginLine='7' DefiningType='MyClass' EndLine='7'
        // Image='' RealLoc='true'>
        //                    <MethodCallExpression BeginLine='7' DefiningType='MyClass' EndLine='7'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //                        <ReferenceExpression BeginLine='7' Context=''
        // DefiningType='MyClass' EndLine='7' Image='System' Names='[System]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                        <VariableExpression BeginLine='7' DefiningType='MyClass'
        // EndLine='7' Image='ex' RealLoc='true'>
        //                            <EmptyReferenceExpression BeginLine='7' DefiningType=''
        // EndLine='7' Image='' RealLoc='false' />
        //                        </VariableExpression>
        //                    </MethodCallExpression>
        //                </ExpressionStatement>
        //            </BlockStatement>
        //        </CatchBlockStatement>
        //    </TryCatchFinallyBlockStatement>
        //    <ExpressionStatement BeginLine='9' DefiningType='MyClass' EndLine='9' Image=''
        // RealLoc='true'>
        //        <MethodCallExpression BeginLine='9' DefiningType='MyClass' EndLine='9'
        // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
        //            <ReferenceExpression BeginLine='9' Context='' DefiningType='MyClass'
        // EndLine='9' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //            <LiteralExpression BeginLine='9' DefiningType='MyClass' EndLine='9'
        // Image='Before' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
        //        </MethodCallExpression>
        //    </ExpressionStatement>
        // </BlockStatement>

        // (2 edges) BlockStatement->ExpressionStatementLine3->TryCatchFinallyBlockStatement
        // (3 edges)
        // TryCatchFinallyBlockStatement->BlockStatement(Try)->ExpressionStatementLine5->ExpressionStatementLine9
        // (4 edges)
        // TryCatchFinallyBlockStatement->CatchBlockStatement->BlockStatement(Catch)->ExpressionStatementLine7->ExpressionStatementLine9
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(9));

        MatcherAssert.assertThat(getVerticesWithEndScope(), hasSize(equalTo(3)));

        // System.debug('Hello');
        assertEndScopes(
                new String[] {NodeType.BLOCK_STATEMENT, NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT},
                ExpressionStatementVertex.class,
                5);

        // System.debug(ex);
        assertEndScopes(
                new String[] {
                    NodeType.BLOCK_STATEMENT,
                    NodeType.CATCH_BLOCK_STATEMENT,
                    NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT
                },
                ExpressionStatementVertex.class,
                7);

        // System.debug('After');
        assertEndScopes(BLOCK, ExpressionStatementVertex.class, 9);

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testIfWithMethodInStandardCondition() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       if (verifyCreateable()) {\n"
                        + "           insert new Account(Name = 'Acme Inc.');\n"
                        + "       }\n"
                        + "    }\n"
                        + "    public void verifyCreateable() {\n"
                        + "       return Schema.sObjectType.Account.fields.Name.isCreateable();\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // doSomething
        // <BlockStatement BeginLine='2' DefiningType='MyClass' EndLine='6' Image='' RealLoc='true'>
        //    <IfElseBlockStatement BeginLine='3' DefiningType='MyClass' ElseStatement='false'
        // EndLine='3' Image='' RealLoc='true'>
        //        <IfBlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //            <StandardCondition BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true'>
        //                <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='verifyCreateable' Image='' MethodName='verifyCreateable' RealLoc='true'>
        //                    <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3'
        // Image='' RealLoc='false' />
        //                </MethodCallExpression>
        //            </StandardCondition>
        //            <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='5' Image=''
        // RealLoc='true'>
        //                <DmlInsertStatement BeginLine='4' DefiningType='MyClass' EndLine='4'
        // Image='' RealLoc='true'>
        //                    <NewKeyValueObjectExpression BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='' ParameterCount='1' RealLoc='true' Type='Account'>
        //                        <LiteralExpression BeginLine='4' DefiningType='MyClass'
        // EndLine='4' Image='Acme Inc.' LiteralType='STRING' Long='false' Name='Name' Null='false'
        // RealLoc='true' />
        //                    </NewKeyValueObjectExpression>
        //                </DmlInsertStatement>
        //            </BlockStatement>
        //        </IfBlockStatement>
        //        <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='3' Image=''
        // RealLoc='true' />
        //    </IfElseBlockStatement>
        // </BlockStatement>
        // verifyCreateable
        // <BlockStatement BeginLine='7' DefiningType='MyClass' EndLine='9' Image='' RealLoc='true'>
        //    <ReturnStatement BeginLine='8' DefiningType='MyClass' EndLine='8' Image=''
        // RealLoc='true'>
        //        <MethodCallExpression BeginLine='8' DefiningType='MyClass' EndLine='8'
        // FullMethodName='Schema.sObjectType.Account.fields.Name.isCreateable' Image=''
        // MethodName='isCreateable' RealLoc='true'>
        //            <ReferenceExpression BeginLine='8' Context='' DefiningType='MyClass'
        // EndLine='8' Image='Schema' Names='[Schema, sObjectType, Account, fields, Name]'
        // RealLoc='true' ReferenceType='METHOD' SafeNav='false' />
        //        </MethodCallExpression>
        //    </ReturnStatement>
        // </BlockStatement>

        // doSomething
        // (3 edges) BlockStatement->IfElseBlockStatement->IfBlockStatement->StandardCondition
        // (2 edges) StandardCondition->BlockStatement->DmlInsertStatement(Line4)
        // (1 edges) StandardCondition->BlockStatement(Implicit Else)

        // verifyCreateable
        // (1 edge) BlockStatement->ReturnStatement

        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(7));

        GraphBuildTestUtil.walkAllPaths(g, "doSomething");
    }

    @Test
    public void testWhileStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       int x = 10;\n"
                        + "       while (x > 0) {\n"
                        + "           System.debug(x);\n"
                        + "           x--;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        // (6 edges)
        // BlockStatement->VariableDeclarationStatements->WhileLoopStatement->StandardCondition->BlockStatement->ExpressionStatement->ExpressionStatement
        List<Edge> edges = g.V().outE(Schema.CFG_PATH).toList();
        MatcherAssert.assertThat(edges, hasSize(6));
        assertEndScopes(
                new String[] {NodeType.BLOCK_STATEMENT, NodeType.BLOCK_STATEMENT},
                ExpressionStatementVertex.class,
                6);

        List<ApexPath> paths = GraphBuildTestUtil.walkAllPaths(g, "doSomething");
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
    }

    /**
     * Walk the path in another method that contains a while loop. This was previously confusing the
     * scope stack
     */
    @Test
    public void testWhileStatementInOtherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       withWhileLoop();\n"
                        + "    }\n"
                        + "    public void withWhileLoop() {\n"
                        + "       int x = 10;\n"
                        + "       while (x > 0) {\n"
                        + "           System.debug(x);\n"
                        + "           x--;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        List<ApexPath> paths = GraphBuildTestUtil.walkAllPaths(g, "doSomething");
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
    }

    // <Method Arity="1" BeginLine="2" Constructor="false" Name="doSomething" ReturnType="void">
    //	<ModifierNode Abstract="false" BeginLine="2"/>
    //	<Parameter BeginLine="2" Name="dt" Type="DisplayType">
    //		<ModifierNode Abstract="false" BeginLine="2"/>
    //	</Parameter>
    //	<BlockStatement BeginLine="2">
    //		<VariableDeclarationStatements BeginLine="3">
    //			<ModifierNode Abstract="false" BeginLine="3"/>
    //			<VariableDeclaration BeginLine="3" Name="s" Type="String">
    //				<VariableExpression BeginLine="3" Name="s">
    //					<EmptyReferenceExpression BeginLine="3"/>
    //				</VariableExpression>
    //			</VariableDeclaration>
    //		</VariableDeclarationStatements>
    //		<SwitchStatement BeginLine="4">
    //			<VariableExpression BeginLine="4" Name="dt">
    //				<EmptyReferenceExpression BeginLine="4"/>
    //			</VariableExpression>
    //			<ValueWhenBlock BeginLine="4">
    //				<IdentifierCase BeginLine="5" Identifier="ADDRESS"/>
    //				<IdentifierCase BeginLine="5" Identifier="CURRENCY"/>
    //				<BlockStatement BeginLine="5">
    //					<ExpressionStatement BeginLine="6" EndScopes="[BlockStatement, ValueWhenBlock,
    // SwitchStatement, BlockStatement]">
    //						<MethodCallExpression BeginLine="6" MethodName="debug">
    //							<ReferenceExpression BeginLine="6" Name="System" Names="[System]"
    // ReferenceType="METHOD"/>
    //							<LiteralExpression BeginLine="6" LiteralType="STRING" Value="address or currency"/>
    //						</MethodCallExpression>
    //					</ExpressionStatement>
    //				</BlockStatement>
    //			</ValueWhenBlock>
    //			<ValueWhenBlock BeginLine="4">
    //				<IdentifierCase BeginLine="8" Identifier="ANYTYPE"/>
    //				<BlockStatement BeginLine="8">
    //					<ExpressionStatement BeginLine="9" EndScopes="[BlockStatement, ValueWhenBlock,
    // SwitchStatement, BlockStatement]">
    //						<MethodCallExpression BeginLine="9" MethodName="debug">
    //							<ReferenceExpression BeginLine="9" Name="System" Names="[System]"
    // ReferenceType="METHOD"/>
    //							<LiteralExpression BeginLine="9" LiteralType="STRING" Value="anytype"/>
    //						</MethodCallExpression>
    //					</ExpressionStatement>
    //				</BlockStatement>
    //			</ValueWhenBlock>
    //			<ElseWhenBlock BeginLine="4">
    //				<BlockStatement BeginLine="11">
    //					<ExpressionStatement BeginLine="12" EndScopes="[BlockStatement, ElseWhenBlock,
    // SwitchStatement, BlockStatement]">
    //						<MethodCallExpression BeginLine="12" MethodName="debug">
    //							<ReferenceExpression BeginLine="12" Name="System" Names="[System]"
    // ReferenceType="METHOD"/>
    //							<LiteralExpression BeginLine="12" LiteralType="STRING" Value="unknown"/>
    //						</MethodCallExpression>
    //					</ExpressionStatement>
    //				</BlockStatement>
    //			</ElseWhenBlock>
    //		</SwitchStatement>
    //	</BlockStatement>
    // </Method>

    @Test
    public void testEnumSwitchStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(DisplayType dt) {\n"
                        + "		String s;\n"
                        + "       switch on dt {\n"
                        + "       	when ADDRESS, CURRENCY {\n"
                        + "           	System.debug('address or currency');\n"
                        + "           }\n"
                        + "       	when ANYTYPE {\n"
                        + "           	System.debug('anytype');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        BlockStatementVertex blockStatementLine2 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        VariableDeclarationStatementsVertex variableDeclarationStatement =
                TestUtil.getVertexOnLine(g, VariableDeclarationStatementsVertex.class, 3);
        SwitchStatementVertex switchStatement =
                TestUtil.getVertexOnLine(g, SwitchStatementVertex.class, 4);
        VariableExpressionVertex variableExpression =
                TestUtil.getVertexOnLine(g, VariableExpressionVertex.class, 4);
        List<ValueWhenBlockVertex> valueWhenBlocks =
                SFVertexFactory.loadVertices(
                        g,
                        g.V()
                                .hasLabel(NodeType.VALUE_WHEN_BLOCK)
                                .has(Schema.DEFINING_TYPE, "MyClass")
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
        MatcherAssert.assertThat(valueWhenBlocks, hasSize(equalTo(2)));
        ValueWhenBlockVertex firstValueWhenBlock = valueWhenBlocks.get(0);
        List<IdentifierCaseVertex> firstValueWhenBlockIdentifierCases =
                firstValueWhenBlock.getChildren(NodeType.IDENTIFIER_CASE);
        MatcherAssert.assertThat(firstValueWhenBlockIdentifierCases, hasSize(equalTo(2)));
        BlockStatementVertex firstValueWhenBlockBlockStatement =
                firstValueWhenBlockIdentifierCases.get(1).getNextSibling();
        ExpressionStatementVertex firstValueWhenExpressionStatement =
                firstValueWhenBlockBlockStatement.getOnlyChild();

        ValueWhenBlockVertex secondValueWhenBlock = valueWhenBlocks.get(1);
        List<IdentifierCaseVertex> secondValueWhenBlockIdentifierCases =
                secondValueWhenBlock.getChildren(NodeType.IDENTIFIER_CASE);
        MatcherAssert.assertThat(secondValueWhenBlockIdentifierCases, hasSize(equalTo(1)));
        BlockStatementVertex secondValueWhenBlockBlockStatement =
                secondValueWhenBlockIdentifierCases.get(0).getNextSibling();
        ExpressionStatementVertex secondValueWhenExpressionStatement =
                secondValueWhenBlockBlockStatement.getOnlyChild();

        ElseWhenBlockVertex elseWhenBlock =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(NodeType.ELSE_WHEN_BLOCK)
                                .has(Schema.DEFINING_TYPE, "MyClass"));
        BlockStatementVertex elseWhenBlockBlockStatement = elseWhenBlock.getOnlyChild();
        ExpressionStatementVertex elseWhenBlockExpressionStatement =
                elseWhenBlockBlockStatement.getOnlyChild();

        assertPathEdgesExist(
                Schema.CFG_PATH,
                // All paths go through the block statement and variable expression
                Pair.of(blockStatementLine2, variableDeclarationStatement),
                Pair.of(variableDeclarationStatement, switchStatement),
                Pair.of(switchStatement, variableExpression),

                // The variable expression connects to the first case
                Pair.of(variableExpression, firstValueWhenBlock),
                Pair.of(firstValueWhenBlock, firstValueWhenBlockIdentifierCases.get(0)),
                Pair.of(
                        firstValueWhenBlockIdentifierCases.get(0),
                        firstValueWhenBlockIdentifierCases.get(1)),
                Pair.of(
                        firstValueWhenBlockIdentifierCases.get(1),
                        firstValueWhenBlockBlockStatement),
                Pair.of(firstValueWhenBlockBlockStatement, firstValueWhenExpressionStatement),

                // The variable expression connects to the second case
                Pair.of(variableExpression, secondValueWhenBlock),
                Pair.of(secondValueWhenBlock, secondValueWhenBlockIdentifierCases.get(0)),
                Pair.of(
                        secondValueWhenBlockIdentifierCases.get(0),
                        secondValueWhenBlockBlockStatement),
                Pair.of(secondValueWhenBlockBlockStatement, secondValueWhenExpressionStatement),

                // The variable expression connects to the else condition
                Pair.of(variableExpression, elseWhenBlock),
                Pair.of(elseWhenBlock, elseWhenBlockBlockStatement),
                Pair.of(elseWhenBlockBlockStatement, elseWhenBlockExpressionStatement));

        List<ApexPath> paths = GraphBuildTestUtil.walkAllPaths(g, "doSomething");
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));
    }

    @Test
    public void testIntegerSwitchStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(Integer i) {\n"
                        + "		String s;\n"
                        + "       switch on i {\n"
                        + "       	when 1, 2 {\n"
                        + "           	System.debug('1 or 2');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);

        BlockStatementVertex blockStatementLine2 =
                TestUtil.getVertexOnLine(g, BlockStatementVertex.class, 2);
        VariableDeclarationStatementsVertex variableDeclarationStatement =
                TestUtil.getVertexOnLine(g, VariableDeclarationStatementsVertex.class, 3);
        SwitchStatementVertex switchStatement =
                TestUtil.getVertexOnLine(g, SwitchStatementVertex.class, 4);
        VariableExpressionVertex variableExpression =
                TestUtil.getVertexOnLine(g, VariableExpressionVertex.class, 4);
        List<ValueWhenBlockVertex> valueWhenBlocks =
                SFVertexFactory.loadVertices(
                        g,
                        g.V()
                                .hasLabel(NodeType.VALUE_WHEN_BLOCK)
                                .has(Schema.DEFINING_TYPE, "MyClass")
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
        MatcherAssert.assertThat(valueWhenBlocks, hasSize(equalTo(2)));
        ValueWhenBlockVertex firstValueWhenBlock = valueWhenBlocks.get(0);
        List<LiteralCaseVertex> firstValueWhenBlockLiteralCases =
                firstValueWhenBlock.getChildren(NodeType.LITERAL_CASE);
        MatcherAssert.assertThat(firstValueWhenBlockLiteralCases, hasSize(equalTo(2)));
        BlockStatementVertex firstValueWhenBlockBlockStatement =
                firstValueWhenBlockLiteralCases.get(1).getNextSibling();
        ExpressionStatementVertex firstValueWhenExpressionStatement =
                firstValueWhenBlockBlockStatement.getOnlyChild();

        ValueWhenBlockVertex secondValueWhenBlock = valueWhenBlocks.get(1);
        List<LiteralCaseVertex> secondValueWhenBlockLiteralCases =
                secondValueWhenBlock.getChildren(NodeType.LITERAL_CASE);
        MatcherAssert.assertThat(secondValueWhenBlockLiteralCases, hasSize(equalTo(1)));
        BlockStatementVertex secondValueWhenBlockBlockStatement =
                secondValueWhenBlockLiteralCases.get(0).getNextSibling();
        ExpressionStatementVertex secondValueWhenExpressionStatement =
                secondValueWhenBlockBlockStatement.getOnlyChild();

        ElseWhenBlockVertex elseWhenBlock =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(NodeType.ELSE_WHEN_BLOCK)
                                .has(Schema.DEFINING_TYPE, "MyClass"));
        BlockStatementVertex elseWhenBlockBlockStatement = elseWhenBlock.getOnlyChild();
        ExpressionStatementVertex elseWhenBlockExpressionStatement =
                elseWhenBlockBlockStatement.getOnlyChild();

        assertPathEdgesExist(
                Schema.CFG_PATH,
                // All paths go through the block statement and variable expression
                Pair.of(blockStatementLine2, variableDeclarationStatement),
                Pair.of(variableDeclarationStatement, switchStatement),
                Pair.of(switchStatement, variableExpression),

                // The variable expression connects to the first case
                Pair.of(variableExpression, firstValueWhenBlock),
                Pair.of(firstValueWhenBlock, firstValueWhenBlockLiteralCases.get(0)),
                Pair.of(
                        firstValueWhenBlockLiteralCases.get(0),
                        firstValueWhenBlockLiteralCases.get(1)),
                Pair.of(firstValueWhenBlockLiteralCases.get(1), firstValueWhenBlockBlockStatement),
                Pair.of(firstValueWhenBlockBlockStatement, firstValueWhenExpressionStatement),

                // The variable expression connects to the second case
                Pair.of(variableExpression, secondValueWhenBlock),
                Pair.of(secondValueWhenBlock, secondValueWhenBlockLiteralCases.get(0)),
                Pair.of(
                        secondValueWhenBlockLiteralCases.get(0),
                        secondValueWhenBlockBlockStatement),
                Pair.of(secondValueWhenBlockBlockStatement, secondValueWhenExpressionStatement),

                // The variable expression connects to the else condition
                Pair.of(variableExpression, elseWhenBlock),
                Pair.of(elseWhenBlock, elseWhenBlockBlockStatement),
                Pair.of(elseWhenBlockBlockStatement, elseWhenBlockExpressionStatement));

        List<ApexPath> paths = GraphBuildTestUtil.walkAllPaths(g, "doSomething");
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));
    }

    @Test
    public void testMethodSwitchStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		String s;\n"
                        + "       switch on getInteger() {\n"
                        + "       	when 1, 2 {\n"
                        + "           	System.debug('1 or 2');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "    public static Integer getInteger() {\n"
                        + "    	return 1;\n"
                        + "    }\n"
                        + "}\n";

        GraphBuildTestUtil.buildGraph(g, sourceCode);
        List<ApexPath> paths = GraphBuildTestUtil.walkAllPaths(g, "doSomething");
        // There are 3 paths since #walkPaths does not use any excluders
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));
    }

    @Test
    public void testUnreachableCodeDetection() {
        final String sourceCode =
                "public class MyClass {\n"
                        + "   String doSomething() {\n"
                        + "       return 'hello';\n"
                        + "       System.debug('This line is unreachable');\n"
                        + "   }\n"
                        + "}\n";

        UserActionException thrown =
                assertThrows(
                        UserActionException.class,
                        () -> GraphBuildTestUtil.buildGraph(g, sourceCode),
                        "UserActionException should've been thrown before this point");

        MatcherAssert.assertThat(thrown.getMessage(), containsString("MyClass:4"));
    }

    /**
     * Asserts that the number of {@link Schema#CFG_PATH} edges matches the number of vertex pairs
     * provided and that the edges match.
     */
    private void assertPathEdgesExist(
            String edgeName, Pair<BaseSFVertex, BaseSFVertex>... vertices) {
        List<Edge> edges = g.V().outE(edgeName).toList();
        Set<Pair<Long, Long>> edgeSet = new HashSet<>();
        List<Pair<BaseSFVertex, BaseSFVertex>> duplicates = new ArrayList<>();
        for (Edge edge : edges) {
            Pair<Long, Long> idPair =
                    Pair.of((Long) edge.outVertex().id(), (Long) edge.inVertex().id());
            if (!edgeSet.add(idPair)) {
                BaseSFVertex vertex1 = SFVertexFactory.load(g, g.V(idPair.getLeft()));
                BaseSFVertex vertex2 = SFVertexFactory.load(g, g.V(idPair.getRight()));
                duplicates.add(Pair.of(vertex1, vertex2));
            }
        }
        // Ensure there aren't any duplicate edges
        MatcherAssert.assertThat(duplicates, empty());

        // MatcherAssert.assertThat(edges, hasSize(equalTo(vertices.length)));

        // Map the vertices to their ids
        for (Pair<BaseSFVertex, BaseSFVertex> vertex : vertices) {
            Pair<Long, Long> remove = Pair.of(vertex.getLeft().getId(), vertex.getRight().getId());
            if (!edgeSet.remove(remove)) {
                BaseSFVertex from = SFVertexFactory.load(g, g.V(remove.getLeft()));
                BaseSFVertex to = SFVertexFactory.load(g, g.V(remove.getRight()));
                fail("Specified edge was not found.\nfrom=" + from + "\n" + "to=" + to);
            }
        }

        // Ensure that all edges were specified by the caller
        if (!edgeSet.isEmpty()) {
            for (Pair<Long, Long> edge : edgeSet) {
                BaseSFVertex from = SFVertexFactory.load(g, edge.getLeft());
                BaseSFVertex to = SFVertexFactory.load(g, edge.getRight());
                fail("Edge was not specified by caller.\nfrom=" + from + "\n" + "to=" + to);
            }
        }
    }

    private List<BaseSFVertex> getVerticesWithEndScope() {
        return SFVertexFactory.loadVertices(
                g, g.V().has(Schema.END_SCOPES).not(has(Schema.IS_STANDARD, true)));
    }

    private void assertEndScopes(
            String[] endScopes, Class<? extends BaseSFVertex> clazz, int beginLine) {
        BaseSFVertex vertex = TestUtil.getVertexOnLine(g, clazz, beginLine);
        MatcherAssert.assertThat(
                vertex.getEndScopes().toString(),
                vertex.getEndScopes(),
                hasSize(equalTo(endScopes.length)));
        MatcherAssert.assertThat(vertex.getEndScopes(), contains(endScopes));
    }

    private void assertEndScopes(
            String[] endScopes, Class<? extends BaseSFVertex> clazz, int beginLine, int endLine) {
        BaseSFVertex vertex = TestUtil.getVertexOnLine(g, clazz, beginLine, endLine);
        MatcherAssert.assertThat(
                vertex.getEndScopes().toString(),
                vertex.getEndScopes(),
                hasSize(equalTo(endScopes.length)));
        MatcherAssert.assertThat(vertex.getEndScopes(), contains(endScopes));
    }
}
