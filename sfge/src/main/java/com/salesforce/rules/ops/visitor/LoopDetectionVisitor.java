package com.salesforce.rules.ops.visitor;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.rules.ops.boundary.LoopBoundary;
import com.salesforce.rules.ops.boundary.LoopBoundaryDetector;

/** Visitor that gets notified when a loop vertex is invoked in the path. */
public abstract class LoopDetectionVisitor extends DefaultNoOpPathVertexVisitor {
    protected final LoopBoundaryDetector loopBoundaryDetector;

    public LoopDetectionVisitor() {
        loopBoundaryDetector = new LoopBoundaryDetector();
    }

    @Override
    public boolean visit(DoLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return true;
    }

    @Override
    public void afterVisit(DoLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.popBoundary(vertex);
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return true;
    }

    @Override
    public void afterVisit(ForEachStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.popBoundary(vertex);
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return true;
    }

    @Override
    public void afterVisit(ForLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.popBoundary(vertex);
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.pushBoundary(new LoopBoundary(vertex));
        return true;
    }

    @Override
    public void afterVisit(WhileLoopStatementVertex vertex, SymbolProvider symbols) {
        loopBoundaryDetector.popBoundary(vertex);
    }
}
