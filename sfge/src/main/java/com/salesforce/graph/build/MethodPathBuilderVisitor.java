package com.salesforce.graph.build;

import com.google.common.collect.ImmutableSet;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.config.UserFacingErrorMessages;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.exception.UserActionException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/** Visits a method and draws all control flow edges. */
public class MethodPathBuilderVisitor {
    private static final Logger LOGGER = LogManager.getLogger(MethodPathBuilderVisitor.class);
    /** Load the vertices as SFVertices and log more information. Will impact performance. */
    private static boolean SF_VERTEX_LOGGING = true;

    /**
     * These are the node types that the visitor descends into. All other node types are considered
     * top level and their children aren't visited. TODO: StandardCondition children aren't visited,
     * it should not affect the path
     */
    private static final Set<String> VISIT_CHILDREN_LABELS =
            new HashSet<>(
                    Arrays.asList(
                            NodeType.BLOCK_STATEMENT,
                            NodeType.CATCH_BLOCK_STATEMENT,
                            NodeType.ELSE_WHEN_BLOCK,
                            NodeType.FOR_EACH_STATEMENT,
                            NodeType.FOR_LOOP_STATEMENT,
                            NodeType.IF_ELSE_BLOCK_STATEMENT,
                            NodeType.IF_BLOCK_STATEMENT,
                            NodeType.SWITCH_STATEMENT,
                            NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT,
                            NodeType.TYPE_WHEN_BLOCK,
                            NodeType.WHILE_LOOP_STATEMENT,
                            NodeType.VALUE_WHEN_BLOCK));

    /**
     * These siblings should not be called after each other. They are discrete blocks that are
     * mutually exclusive of their sibling.
     */
    private static final Set<String> NEXT_SIBLINGS_NOT_CONNECTED =
            ImmutableSet.of(
                    NodeType.CATCH_BLOCK_STATEMENT,
                    NodeType.ELSE_WHEN_BLOCK,
                    NodeType.TYPE_WHEN_BLOCK,
                    NodeType.VALUE_WHEN_BLOCK);

    private final GraphTraversalSource g;

    /**
     * Stack that keeps track of the next vertex that is in the outer scope. An edge is drawn from
     * the last edge in an inner path to the nextVertexInOuterScope. In the following example, edges
     * will be drawn from. System.debug('log1')->System.debug('next-vertex-in-outer-scope);
     * ImplicitLog1EmptyElseBlockStatement->System.debug('next-vertex-in-outer-scope);
     * System.debug('log2')->System.debug('next-vertex-in-outer-scope);
     * ImplicitLog2EmptyElseBlockStatement->System.debug('next-vertex-in-outer-scope); public void
     * doSomething() { if (log1) { System.debug('log1'); if (log2) { System.debug('log2'); } }
     * System.debug('next-vertex-in-outer-scope);
     */
    private final Stack<Optional<Vertex>> nextVertexInOuterScope;

    /** Most recent inner scope that was started. */
    private final Stack<Vertex> currentInnerScope;

    public MethodPathBuilderVisitor(GraphTraversalSource g) {
        this.g = g;
        this.nextVertexInOuterScope = new Stack<>();
        // Avoid needing to always check for size
        this.nextVertexInOuterScope.push(Optional.empty());
        this.currentInnerScope = new Stack<>();
    }

    public static void apply(GraphTraversalSource g, Vertex methodVertex) {
        if (!methodVertex.label().equals(NodeType.METHOD)) {
            throw new UnexpectedException(methodVertex);
        }

        Vertex blockStatement =
                GremlinUtil.getOnlyChild(g, methodVertex, NodeType.BLOCK_STATEMENT).orElse(null);
        // This can be null in cases such as default constructors that don't have an implementation
        if (blockStatement != null) {
            MethodPathBuilderVisitor visitor = new MethodPathBuilderVisitor(g);
            visitor._visit(blockStatement, methodVertex, true);
        }
    }

    private void _visit(Vertex vertex, Vertex parent, boolean lastChild) {
        try {
            String label = vertex.label();

            boolean isScopeBoundary = isScopeBoundary(vertex);
            if (isScopeBoundary) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Pushing Scope Boundary. vertex=" + vertexToString(vertex));
                }
                // Push the vertex to indicate we are entering a new scope boundary
                currentInnerScope.push(vertex);
                // Push the next sibling of the current vertex onto the stack. The inner vertices
                // will use this to
                // connect their outgoing edges.
                Optional<Vertex> nextSibling = GremlinUtil.getNextSibling(g, vertex);
                if (nextSibling.isPresent()
                        && NEXT_SIBLINGS_NOT_CONNECTED.contains(nextSibling.get().label())) {
                    nextSibling = Optional.empty();
                }
                nextVertexInOuterScope.push(nextSibling);
            }

            if (label.equals(NodeType.BLOCK_STATEMENT)) {
                visitBlockStatement(vertex);
            } else if (label.equals(NodeType.CATCH_BLOCK_STATEMENT)) {
                visitCatchBlockStatement(vertex, parent);
            } else if (label.equals(NodeType.ELSE_WHEN_BLOCK)) {
                visitElseWhenBlock(vertex);
            } else if (label.equals(NodeType.FOR_EACH_STATEMENT)) {
                visitForEachStatement(vertex);
            } else if (label.equals(NodeType.FOR_LOOP_STATEMENT)) {
                visitForLoopStatement(vertex);
            } else if (label.equals(NodeType.IF_ELSE_BLOCK_STATEMENT)) {
                visitIfElseBlockStatement(vertex);
            } else if (label.equals(NodeType.IF_BLOCK_STATEMENT)) {
                visitIfBlockStatement(vertex);
            } else if (label.equals(NodeType.STANDARD_CONDITION)) {
                visitStandardCondition(vertex, parent);
            } else if (label.equals(NodeType.SWITCH_STATEMENT)) {
                visitSwitchStatement(vertex);
            } else if (label.equals(NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT)) {
                visitTryCatchFinallyBlockStatement(vertex);
            } else if (label.equals(NodeType.TYPE_WHEN_BLOCK)) {
                visitTypeWhenBlock(vertex);
            } else if (label.equals(NodeType.VALUE_WHEN_BLOCK)) {
                visitValueWhenBlock(vertex);
            } else if (label.equals(NodeType.WHILE_LOOP_STATEMENT)) {
                visitWhileLoopStatement(vertex);
            } else {
                visit(vertex, lastChild);
            }

            boolean visitChildren = shouldVisitChildren(vertex);

            if (visitChildren) {
                List<Vertex> children = GremlinUtil.getChildren(g, vertex);
                for (int i = 0; i < children.size(); i++) {
                    Vertex child = children.get(i);
                    _visit(child, vertex, (i == (children.size() - 1)));
                }
            }

            if (isScopeBoundary) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Popping Scope Boundary. vertex=" + vertexToString(vertex));
                }
                currentInnerScope.pop();
                nextVertexInOuterScope.pop();
            }
        } catch (UserActionException | TodoException | UnexpectedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnexpectedException(g, vertex, ex);
        }
    }

    private void visit(Vertex vertex, boolean lastChild) {
        addEdgeFromPreviousSibling(vertex);
        // If this is the last child and there is an outer vertex on the stack, connect to it
        if (lastChild) {
            Vertex outerVertex = getNextActionableVertexInOuterScope().orElse(null);
            if (outerVertex != null) {
                if (!isVertexTerminal(vertex)) {
                    addEdge(Schema.CFG_PATH, vertex, outerVertex);
                } else {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(
                                "Skipped edge for terminal vertex. vertex="
                                        + vertexToString(vertex));
                    }
                }
            }
            addEndScopes(vertex);
        }
    }

    private void visitBlockStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.BLOCK_STATEMENT);

        // Block statements always flow into their first child
        Vertex firstChild = GremlinUtil.getFirstChild(g, vertex).orElse(null);
        if (firstChild != null) {
            addEdge(Schema.CFG_PATH, vertex, firstChild);
        } else {
            Vertex outerVertex = getNextActionableVertexInOuterScope().orElse(null);
            if (outerVertex != null) {
                // Empty block vertices flow into the next actionable vertex in the scope stack
                addEdge(Schema.CFG_PATH, vertex, outerVertex);
            }
            addEndScopes(vertex);
        }
    }

    // <ForEachStatement BeginLine='4' EndLine='4'>
    //    <VariableExpression BeginLine='4' EndLine='4' Image='fieldsToCheck'>
    //        <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //    </VariableExpression>
    //    <VariableDeclarationStatements BeginLine='4' EndLine='4'>
    //        <ModifierNode BeginLine='4' EndLine='4'/>
    //        <VariableDeclaration BeginLine='4' EndLine='4' Image='fieldToCheck'>
    //            <VariableExpression BeginLine='4' EndLine='4' Image='fieldToCheck'>
    //                <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //            </VariableExpression>
    //        </VariableDeclaration>
    //    </VariableDeclarationStatements>
    //    <VariableExpression BeginLine='4' EndLine='4' Image='fieldToCheck'>
    //        <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //    </VariableExpression>
    //    <BlockStatement BeginLine='4' EndLine='10'>
    private void visitForEachStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.FOR_EACH_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        List<Vertex> children = GremlinUtil.getChildren(g, vertex, 4);

        // Add an edge to the first child
        addEdge(Schema.CFG_PATH, vertex, children.get(0));

        assertVertexLabel(children.get(2), NodeType.VARIABLE_EXPRESSION);
        assertVertexLabel(children.get(3), NodeType.BLOCK_STATEMENT);
        // Add an edge from the last variable expression to the block statement
        addEdge(Schema.CFG_PATH, children.get(2), children.get(3));
    }

    // <ForLoopStatement BeginLine='4' EndLine='4'>
    //    <VariableDeclarationStatements BeginLine='4' EndLine='4'>
    //        <ModifierNode BeginLine='4' EndLine='4'/>
    //        <VariableDeclaration BeginLine='4' EndLine='4' Image='i'>
    //            <LiteralExpression BeginLine='4' EndLine='4' Image='0' />
    //            <VariableExpression BeginLine='4' EndLine='4' Image='i'>
    //                <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //            </VariableExpression>
    //        </VariableDeclaration>
    //    </VariableDeclarationStatements>
    //    <StandardCondition BeginLine='4' EndLine='4'>
    //        <BooleanExpression BeginLine='4' EndLine='4'>
    //            <VariableExpression BeginLine='4' EndLine='4' Image='i'>
    //                <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //            </VariableExpression>
    //            <VariableExpression BeginLine='4' EndLine='4' Image='length'>
    //                <ReferenceExpression BeginLine='4' EndLine='4' Image='fieldsToCheck' />
    //            </VariableExpression>
    //        </BooleanExpression>
    //    </StandardCondition>
    //    <PostfixExpression BeginLine='4' EndLine='4'>
    //        <VariableExpression BeginLine='4' EndLine='4' Image='i'>
    //            <EmptyReferenceExpression BeginLine='4' EndLine='4'/>
    //        </VariableExpression>
    //    </PostfixExpression>
    //    <BlockStatement BeginLine='4' EndLine='11'>
    private void visitForLoopStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.FOR_LOOP_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        List<Vertex> children = GremlinUtil.getChildren(g, vertex);
        // Add an edge to the first child.
        Vertex firstChild = children.get(0);
        addEdge(Schema.CFG_PATH, vertex, firstChild);

        // Guard against the case where for loop to only include the block statement. An example is
        // "for (;;)".
        if (children.size() > 1) {
            Vertex lastChild = children.get(children.size() - 1);
            assertVertexLabel(lastChild, NodeType.BLOCK_STATEMENT);
            // Add an edge from the statement before the block statement to the block statement
            addEdge(Schema.CFG_PATH, children.get(children.size() - 2), lastChild);
        }
    }

    private void visitWhileLoopStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.WHILE_LOOP_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        List<Vertex> children = GremlinUtil.getChildren(g, vertex, 2);

        // Add an edge to the first child.
        Vertex standardCondition = children.get(0);
        assertVertexLabel(standardCondition, NodeType.STANDARD_CONDITION);
        addEdge(Schema.CFG_PATH, vertex, standardCondition);

        Vertex blockStatement = children.get(1);
        assertVertexLabel(blockStatement, NodeType.BLOCK_STATEMENT);
        addEdge(Schema.CFG_PATH, standardCondition, blockStatement);
    }

    private void visitIfElseBlockStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.IF_ELSE_BLOCK_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        addEdge(Schema.CFG_PATH, vertex, GremlinUtil.getFirstChild(g, vertex).get());
    }

    private void visitIfBlockStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.IF_BLOCK_STATEMENT);

        addEdge(Schema.CFG_PATH, vertex, GremlinUtil.getFirstChild(g, vertex).get());
    }

    @SuppressWarnings("PMD.EmptyIfStmt")
    private void visitStandardCondition(Vertex vertex, Vertex parent) {
        assertVertexLabel(vertex, NodeType.STANDARD_CONDITION);

        if (parent.label().equals(NodeType.IF_BLOCK_STATEMENT)) {
            // The next vertex when the standard condition evaluates to TRUE is the sibling of the
            // Standard Condition
            addEdge(Schema.CFG_PATH, vertex, GremlinUtil.getNextSibling(g, vertex).get());

            // The next vertex when the standard condition evaluates to FALSE is the sibling of the
            // Parent IfBlockStatement
            addEdge(Schema.CFG_PATH, vertex, GremlinUtil.getNextSibling(g, parent).get());
        } else if (parent.label().equals(NodeType.FOR_LOOP_STATEMENT)) {
            // The previous sibling did not create an edge because the default visit code wasn't
            // invoked. It is possible
            // there is a previous sibling. The forward edge was not created, because this code
            // superseded it. Create
            // the edge if the sibling exists.
            Vertex previousSibling = GremlinUtil.getPreviousSibling(g, vertex).orElse(null);
            if (previousSibling != null) {
                addEdge(Schema.CFG_PATH, previousSibling, vertex);
            }
        } else if (parent.label().equals(NodeType.WHILE_LOOP_STATEMENT)) {
            // Intentionally left blank. The visitWhileLoopStatement method handles all edges
        } else {
            throw new UnexpectedException(vertex);
        }
    }

    /**
     * *********************************************************************************************************
     * BEGIN TRY/CATCH STATEMENTS
     * *********************************************************************************************************
     */
    private void visitTryCatchFinallyBlockStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        // TryCatchFinallyBlockStatement->BLockStatement(Try)
        Vertex blockStatement =
                GremlinUtil.getFirstChild(g, vertex)
                        .orElseThrow(() -> new UnexpectedException(vertex));
        assertVertexLabel(blockStatement, NodeType.BLOCK_STATEMENT);
        addEdge(Schema.CFG_PATH, vertex, blockStatement);
    }

    private void visitCatchBlockStatement(Vertex vertex, Vertex parent) {
        assertVertexLabel(vertex, NodeType.CATCH_BLOCK_STATEMENT);

        // TryCatchFinallyBlockStatement->CatchBlockStatement->BLockStatement(Catch)
        addEdge(Schema.CFG_PATH, parent, vertex);

        Vertex catchInnerBlockStatement =
                GremlinUtil.getOnlyChild(g, vertex, NodeType.BLOCK_STATEMENT)
                        .orElseThrow(() -> new UnexpectedException(vertex));
        addEdge(Schema.CFG_PATH, vertex, catchInnerBlockStatement);
    }
    /**
     * *********************************************************************************************************
     * END TRY/CATCH STATEMENTS
     * *********************************************************************************************************
     */

    /**
     * *********************************************************************************************************
     * BEGIN SWITCH STATEMENTS
     * *********************************************************************************************************
     */
    private void visitElseWhenBlock(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.ELSE_WHEN_BLOCK);

        // The first child is the BlockStatement
        Vertex identifierCase = GremlinUtil.getFirstChild(g, vertex).get();
        assertVertexLabel(identifierCase, NodeType.BLOCK_STATEMENT);
        addEdge(Schema.CFG_PATH, vertex, identifierCase);
    }

    private void visitTypeWhenBlock(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.TYPE_WHEN_BLOCK);

        // The first child is the BlockStatement
        Vertex identifierCase = GremlinUtil.getFirstChild(g, vertex).get();
        assertVertexLabel(identifierCase, NodeType.BLOCK_STATEMENT);
        addEdge(Schema.CFG_PATH, vertex, identifierCase);
    }

    private void visitValueWhenBlock(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.VALUE_WHEN_BLOCK);

        // The first child is the IdentifierCase
        Vertex identifierCase = GremlinUtil.getFirstChild(g, vertex).get();
        assertVertexLabel(identifierCase, NodeType.IDENTIFIER_CASE, NodeType.LITERAL_CASE);
        addEdge(Schema.CFG_PATH, vertex, identifierCase);

        List<Vertex> vertices = GremlinUtil.getChildren(g, vertex);

        Vertex lastIdentifierCase = vertices.get(vertices.size() - 2);
        assertVertexLabel(lastIdentifierCase, NodeType.IDENTIFIER_CASE, NodeType.LITERAL_CASE);

        Vertex blockStatement = vertices.get(vertices.size() - 1);
        assertVertexLabel(blockStatement, NodeType.BLOCK_STATEMENT);

        addEdge(Schema.CFG_PATH, lastIdentifierCase, blockStatement);
    }

    private void visitSwitchStatement(Vertex vertex) {
        assertVertexLabel(vertex, NodeType.SWITCH_STATEMENT);

        addEdgeFromPreviousSibling(vertex);

        // The first child is the switch expression
        Vertex switchExpression = GremlinUtil.getFirstChild(g, vertex).get();
        assertVertexLabel(
                switchExpression,
                NodeType.METHOD_CALL_EXPRESSION,
                NodeType.TRIGGER_VARIABLE_EXPRESSION,
                NodeType.VARIABLE_EXPRESSION);
        addEdge(Schema.CFG_PATH, vertex, switchExpression);

        // Each of the case statements plus the default case, are siblings of the switch expression
        Vertex caseBlock = GremlinUtil.getNextSibling(g, switchExpression).orElse(null);
        while (caseBlock != null) {
            assertVertexLabel(
                    caseBlock,
                    NodeType.ELSE_WHEN_BLOCK,
                    NodeType.TYPE_WHEN_BLOCK,
                    NodeType.VALUE_WHEN_BLOCK);
            // The paths are from the variable expression to each of the possible case statements
            addEdge(Schema.CFG_PATH, switchExpression, caseBlock);
            caseBlock = GremlinUtil.getNextSibling(g, caseBlock).orElse(null);
        }
    }
    /**
     * *********************************************************************************************************
     * END SWITCH STATEMENTS
     * *********************************************************************************************************
     */
    private boolean isScopeBoundary(Vertex vertex) {
        return NodeType.START_INNER_SCOPE_LABELS.contains(vertex.label());
    }

    private boolean shouldVisitChildren(Vertex vertex) {
        return VISIT_CHILDREN_LABELS.contains(vertex.label());
    }

    private boolean isVertexTerminal(Vertex vertex) {
        return NodeType.TERMINAL_VERTEX_LABELS.contains(vertex.label());
    }

    /**
     * Set applicable end scopes to a vertex. This method is applied to the last executable child in
     * a scope. For instance. In the following example. The following endScopes will be added.
     *
     * <p>public void doSomething() { if (log1) { System.debug('log1'); if (log2) {
     * System.debug('log2'); // Next executable vertex is two scopes away. End
     * Scopes=[IfElseBlockStatement, IfElseBlockStatement] } // ImplicitElse // Next executable
     * vertex is two scopes away. End Scopes=[IfElseBlockStatement, IfElseBlockStatement] } //
     * ImplicitElse // Next executable vertex is one scope away. End Scopes=[IfElseBlockStatement]
     * System.debug('next-vertex-in-outer-scope);
     */
    private List<String> addEndScopes(Vertex vertex) {
        if (currentInnerScope.isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Add End Scopes. currentInnerScope is empty. vertex=" + vertex);
            }
            return Collections.emptyList();
        }

        List<String> endScopes = new ArrayList<>();
        if (NodeType.TERMINAL_VERTEX_LABELS.contains(vertex.label())) {
            // Terminal vertices need to pop all scopes
            for (int i = currentInnerScope.size() - 1; i >= 0; i--) {
                endScopes.add(currentInnerScope.get(i).label());
            }
        } else {
            if (nextVertexInOuterScope.peek().isPresent()) {
                // End the current scope, rely on the nextVertexInOuterScope to end the other scopes
                endScopes.add(currentInnerScope.peek().label());
            } else {
                // Add 1 to the distance to get a number of scopes to end. i.e. a vertex with an
                // offset
                // of requires us to end 1 scope
                int numScopesToEnd = getNextActionableVertexInOuterScopeDistance() + 1;
                if (numScopesToEnd == 0) {
                    // Use all of there vertices if there isn't an outer actionable vertex
                    numScopesToEnd = currentInnerScope.size();
                }

                // Verify that the iteration is within the bounds of the scopes we expect to end
                if (numScopesToEnd > currentInnerScope.size()) {
                    throw new UnexpectedException(vertex);
                }

                for (int i = 0; i < numScopesToEnd; i++) {
                    int index = currentInnerScope.size() - 1 - i;
                    endScopes.add(currentInnerScope.get(index).label());
                }
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Adding End Scopes. vertex="
                            + vertexToString(vertex)
                            + ", endScopes="
                            + endScopes);
        }
        g.V(vertex).property(Schema.END_SCOPES, endScopes).next();
        return endScopes;
    }

    /**
     * Traverses the {@link #nextVertexInOuterScope} from top of the stack to the bottom in order to
     * find the closest scope with an actionable vertex. Returns the index, or -1 if it can't be
     * found.
     */
    private int getNextActionableVertexInOuterScopeDistance() {
        int distance = 0;
        for (int i = nextVertexInOuterScope.size() - 1; i >= 0; i--, distance++) {
            if (nextVertexInOuterScope.get(i).isPresent()) {
                return distance;
            }
        }
        return -1;
    }

    /**
     * Traverses the {@link #nextVertexInOuterScope} from top of the stack to the bottom in order to
     * find the closest scope with an actionable vertex. Returns {@link Optional#empty()} if one
     * can't be found.
     */
    private Optional<Vertex> getNextActionableVertexInOuterScope() {
        for (int i = nextVertexInOuterScope.size() - 1; i >= 0; i--) {
            if (nextVertexInOuterScope.get(i).isPresent()) {
                return nextVertexInOuterScope.get(i);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds an edge between the vertex and its previous sibling, if the previous sibling isn't a
     * type that starts an inner scope. Any vertices that start an inner scope create the outgoing
     * edge from themselves to {@code vertex}.
     *
     * <p>For example, the following example. The PositiveBlockStatement and
     * NegativeEmptyBlockStatement will have forward edges created that point to the second
     * IfElseBlockStatement. The second IfElseBlockStatement should not create another edge.
     *
     * <p>if (x == 10) { // IfElseBlockStatement->IfBlockStatement->StandardCondition
     * System.debug('x is 10'); // PositiveBlockStatement->ExpressionStatement->MethodCallExpression
     * } // NegativeEmptyBlockStatement if (y == 20) {
     * IfElseBlockStatement->IfBlockStatement->StandardCondition System.debug('y is 20'); }
     *
     * <p>However, in this example we want the second IfElseStatement to create the edge from itself
     * to the top level System.debug statement.
     *
     * <p>if (x == 10) { // IfElseBlockStatement->IfBlockStatement->StandardCondition
     * System.debug('x is 10'); // PositiveBlockStatement->ExpressionStatement->MethodCallExpression
     * } // NegativeEmptyBlockStatement System.debug('This is the previous sibling'); if (y == 20) {
     * IfElseBlockStatement->IfBlockStatement->StandardCondition System.debug('y is 20'); }
     */
    private void addEdgeFromPreviousSibling(Vertex vertex) {
        Vertex previousSibling = GremlinUtil.getPreviousSibling(g, vertex).orElse(null);

        // Only connect the previous sibling if it doesn't use the nextVertex stack
        if (previousSibling != null
                && !NodeType.START_INNER_SCOPE_LABELS.contains(previousSibling.label())) {
            addEdge(Schema.CFG_PATH, previousSibling, vertex);
        }
    }

    private void addEdge(String name, Vertex from, Vertex to) {
        if (NodeType.TERMINAL_VERTEX_LABELS.contains(from.label())) {
            // Ask user to fix unreachable code
            throw new UserActionException(
                    String.format(
                            UserFacingErrorMessages.UNREACHABLE_CODE,
                            GremlinUtil.getFileName(g, to),
                            to.value(Schema.DEFINING_TYPE),
                            to.value(Schema.BEGIN_LINE)));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Adding edge. name="
                            + name
                            + ", from="
                            + vertexToString(from)
                            + ", to="
                            + vertexToString(to)
                            + ", g="
                            + g);
        }
        g.addE(name).from(from).to(to).iterate();
    }

    /** Converts a vertex to string based on the value of {@link #SF_VERTEX_LOGGING} */
    private String vertexToString(Vertex vertex) {
        if (SF_VERTEX_LOGGING) {
            // Turn off caching since this class modifies the graph
            return SFVertexFactory.load(g, vertex, SFVertexFactory.CacheBehavior.NO_CACHE)
                    .toMinimalString();
        } else {
            return vertex.label();
        }
    }

    /** Throw an exception if the vertex is not one of the expected labels */
    private void assertVertexLabel(Vertex vertex, String... labels) {
        for (String label : labels) {
            if (vertex.label().equals(label)) {
                return;
            }
        }
        throw new UnexpectedException("vertex=" + vertexToString(vertex) + ", labels=" + labels);
    }
}
