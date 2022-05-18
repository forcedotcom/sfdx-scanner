package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class FieldVertex extends FieldWithModifierVertex implements NamedVertex, Typeable {
    FieldVertex(Map<Object, Object> properties) {
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

    public boolean hasGetterBlock() {
        return getBoolean(Schema.HAS_GETTER_METHOD_BLOCK);
    }

    public boolean hasSetterBlock() {
        return getBoolean(Schema.HAS_SETTER_METHOD_BLOCK);
    }
}
