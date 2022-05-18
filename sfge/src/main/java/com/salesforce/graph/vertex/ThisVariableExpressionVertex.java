package com.salesforce.graph.vertex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class ThisVariableExpressionVertex extends TODO_FIX_HIERARCHY_ChainedVertex
        implements Typeable {
    ThisVariableExpressionVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    ThisVariableExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, supplementalParam);
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return visitor.visit(this, symbols);
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
        visitor.afterVisit(this, symbols);
    }

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {
        visitor.afterVisit(this);
    }

    @Override
    public String getCanonicalType() {
        return getDefiningType();
    }
}
