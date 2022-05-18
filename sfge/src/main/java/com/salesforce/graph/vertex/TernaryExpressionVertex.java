package com.salesforce.graph.vertex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class TernaryExpressionVertex extends ChainedVertex {
    TernaryExpressionVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    TernaryExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
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

    /**
     * String x = y ? 'y is true' : 'y is false;
     *
     * @return 'y is true' in the example
     */
    public ChainedVertex getTrueValue() {
        return getChild(1);
    }

    /**
     * String x = y ? 'y is true' : 'y is false;
     *
     * @return 'y is false' in the example
     */
    public ChainedVertex getFalseValue() {
        return getChild(2);
    }
}
