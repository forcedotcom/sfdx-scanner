package com.salesforce.graph.vertex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

/** Represents things such as Trigger.operationType */
public final class TriggerVariableExpressionVertex extends TODO_FIX_HIERARCHY_ChainedVertex {
    TriggerVariableExpressionVertex(Map<Object, Object> properties) {
        super(properties);
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return false;
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return false;
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {}

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {}
}
