package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Optional;

public class ParameterVertex extends TODO_FIX_HIERARCHY_ChainedVertex
        implements NamedVertex, Typeable {
    ParameterVertex(Map<Object, Object> properties) {
        super(properties);
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
        return ApexStandardLibraryUtil.getCanonicalName(getString(Schema.TYPE));
    }

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    public Optional<String> getSymbolicName() {
        return Optional.of(getName());
    }
}
