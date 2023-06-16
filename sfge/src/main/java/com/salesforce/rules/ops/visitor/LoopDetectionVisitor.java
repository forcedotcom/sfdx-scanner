package com.salesforce.rules.ops.visitor;

import com.google.common.collect.ImmutableSet;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.build.StaticBlockUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.rules.ops.boundary.LoopBoundary;
import com.salesforce.rules.ops.boundary.LoopBoundaryDetector;
import com.salesforce.rules.ops.boundary.OverridableLoopExclusionBoundary;
import com.salesforce.rules.ops.boundary.PermanentLoopExclusionBoundary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Visitor that gets notified when a loop vertex is invoked in the path. */
public abstract class LoopDetectionVisitor extends DefaultNoOpPathVertexVisitor {
    private final LoopBoundaryDetector loopBoundaryDetector;
    private static final ImmutableSet<String> LOOP_VERTICES_LABELS = ImmutableSet.of(
        ASTConstants.NodeType.DO_LOOP_STATEMENT,
        ASTConstants.NodeType.FOR_EACH_STATEMENT,
        ASTConstants.NodeType.FOR_LOOP_STATEMENT,
        ASTConstants.NodeType.WHILE_LOOP_STATEMENT);

    public LoopDetectionVisitor() {
        loopBoundaryDetector = new LoopBoundaryDetector();
    }

    @Override
    public boolean visit(DoLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return false;
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return false;
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return false;
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return false;
    }

    /**
     * This case is specific to method calls on ForEach loop definition. These methods are called
     * only once even though they are technically under a loop definition. We create this boundary
     * to show that calls here are not actually called multiple times.
     *
     * <p>For example, <code>getValues()</code> in this forEach gets called only once: <code>
     * for (String s: getValues())</code>
     *
     * @param vertex Method call in question
     * @param symbols SymbolProvider at this state
     * @return true to visit the children
     */
    @Override
    public boolean visit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        // If already within a loop's boundary, get the loop item
        final Optional<LoopBoundary> currentLoopBoundaryOpt = loopBoundaryDetector.peek();
        if (currentLoopBoundaryOpt.isPresent()) {
            final SFVertex loopBoundaryItem = currentLoopBoundaryOpt.get().getBoundaryItem();

            createPermanentLoopExclusionIfApplicable(vertex, loopBoundaryItem);

            // If permanent loop exclusion was added, overridable loop exclusion will never get
            // added.
            // Leaving the conditional check here to be more obvious.
            if (loopBoundaryItem instanceof ForEachStatementVertex) {
                createOverridableLoopExclusion(vertex, loopBoundaryItem);
            }
        }
        return true;
    }

    private void createPermanentLoopExclusionIfApplicable(
            MethodCallExpressionVertex vertex, SFVertex loopBoundaryItem) {
        if (StaticBlockUtil.isStaticBlockMethodCall(vertex)) {
            // All nested loops before this don't get counted as a loop context.
            loopBoundaryDetector.pushBoundary(new PermanentLoopExclusionBoundary(vertex));
        }
    }

    private void createOverridableLoopExclusion(
            MethodCallExpressionVertex vertex, SFVertex loopBoundaryItem) {
        // We are within a ForEach statement.
        // Check if the method calls parent is the same as this ForEach statement.
        // If they are the same, this method would get invoked only once.
        BaseSFVertex parentVertex = vertex.getParent();
        if (parentVertex instanceof ForEachStatementVertex
                && parentVertex.equals(loopBoundaryItem)) {
            // This method would get invoked only once within the immediate surrounding loop
            // context
            loopBoundaryDetector.pushBoundary(new OverridableLoopExclusionBoundary(vertex));
        }
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        // If within a method call loop exclusion, pop boundary here.
        final Optional<LoopBoundary> currentLoopBoundaryOpt = loopBoundaryDetector.peek();
        if (currentLoopBoundaryOpt.isPresent()) {
            final LoopBoundary loopBoundary = currentLoopBoundaryOpt.get();
            if (loopBoundary instanceof OverridableLoopExclusionBoundary
                    || loopBoundary instanceof PermanentLoopExclusionBoundary) {
                // We are in exclusion zone. Check if the method call on the loop exclusion boundary
                // is the same as the current method call.
                if (vertex.equals(loopBoundary.getBoundaryItem())) {
                    // Pop the method call
                    loopBoundaryDetector.popBoundary(vertex);
                }
            }
        }
    }

    @Override
    public void afterVisit(BaseSFVertex vertex, SymbolProvider symbols) {
        // If the vertex has endScopes, pop out the loop items that match the end scopes.
        final List<String> vertexEndScopes = vertex.getEndScopes();

        // Look at the list in reverse order to get the newest innermost scope first.
        for (int i = vertexEndScopes.size() - 1; i >= 0; i--) {
            final String endScopeLabel = vertexEndScopes.get(i);
            // Continue processing only if this is a loop scope.
            if (LOOP_VERTICES_LABELS.contains(endScopeLabel)) {
                // Validate that the loop detector and end scopes information match.
                final Optional<LoopBoundary> currentLoopBoundaryOpt = loopBoundaryDetector.peek();

                // Check if a boundary is actually in place
                if (!currentLoopBoundaryOpt.isPresent()) {
                    throw new ProgrammingException("Invalid scenario. No loop boundary is available to pop. vertex=" + vertex);
                }
                final LoopBoundary currentLoopBoundary = currentLoopBoundaryOpt.get();

                // Check if the existing boundary has the same boundary item as the end item
                if (!currentLoopBoundary.getBoundaryItem().getLabel().equalsIgnoreCase(endScopeLabel)) {
                    throw new ProgrammingException("Current Loop Boundary does not match current end scope. currentLoopBoundaryItem=" + currentLoopBoundary.getBoundaryItem().getLabel() + ", currentEndScope=" + endScopeLabel + ", vertex=" + vertex);
                }
                loopBoundaryDetector.popBoundary(null, false);
            }
        }
    }



    protected Optional<? extends SFVertex> isInsideLoop() {
        return loopBoundaryDetector.isInsideLoop();
    }
}
