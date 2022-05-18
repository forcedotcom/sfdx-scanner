package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class UserEnumVertex extends BaseSFVertex {
    UserEnumVertex(Map<Object, Object> properties) {
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

    /** Not caching the list, this is currently only called once */
    public List<String> getValues() {
        final List<FieldVertex> fields = getChildren(ASTConstants.NodeType.FIELD);
        return fields.stream().map(f -> f.getName()).collect(Collectors.toList());
    }
}
