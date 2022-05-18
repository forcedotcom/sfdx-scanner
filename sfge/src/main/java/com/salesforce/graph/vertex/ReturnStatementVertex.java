package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Optional;

public class ReturnStatementVertex extends BaseSFVertex {
    private final LazyOptionalVertex<ChainedVertex> returnValue;

    ReturnStatementVertex(Map<Object, Object> properties) {
        super(properties);
        this.returnValue = _getReturnValue();
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

    public Optional<ChainedVertex> getReturnValue() {
        return returnValue.get();
    }

    private LazyOptionalVertex<ChainedVertex> _getReturnValue() {
        return new LazyOptionalVertex<>(() -> g().V(getId()).out(Schema.CHILD));
    }
}
