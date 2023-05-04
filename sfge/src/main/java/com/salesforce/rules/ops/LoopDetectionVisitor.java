package com.salesforce.rules.ops;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;

/** Visitor that gets notified when a loop vertex is invoked in the path. */
public abstract class LoopDetectionVisitor extends DefaultNoOpPathVertexVisitor {

    protected abstract void execAfterLoopVertexVisit(BaseSFVertex vertex, SymbolProvider symbols);

    @Override
    public void afterVisit(DoLoopStatementVertex vertex, SymbolProvider symbols) {
        execAfterLoopVertexVisit(vertex, symbols);
    }

    @Override
    public void afterVisit(ForEachStatementVertex vertex, SymbolProvider symbols) {
        execAfterLoopVertexVisit(vertex, symbols);
    }

    @Override
    public void afterVisit(ForLoopStatementVertex vertex, SymbolProvider symbols) {
        execAfterLoopVertexVisit(vertex, symbols);
    }

    @Override
    public void afterVisit(WhileLoopStatementVertex vertex, SymbolProvider symbols) {
        execAfterLoopVertexVisit(vertex, symbols);
    }
}
