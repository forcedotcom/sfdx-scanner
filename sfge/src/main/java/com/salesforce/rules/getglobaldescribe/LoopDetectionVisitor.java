package com.salesforce.rules.getglobaldescribe;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;

abstract class LoopDetectionVisitor extends DefaultNoOpPathVertexVisitor {

    abstract void execAfterLoopVertexVisit(BaseSFVertex vertex, SymbolProvider symbols);

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
