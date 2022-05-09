package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.Map;

/** Case statement used in an "instanceof" style switch statement */
public final class TypeWhenBlockVertex extends WhenBlockVertex {
    TypeWhenBlockVertex(Map<Object, Object> properties) {
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
    public <T> T accept(TypedVertexVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getType() {
        return getString(Schema.TYPE);
    }

    /** Returns "a" in the following example when Account a { System.debug('account ' + a); } */
    public String getVariableName() {
        return getString(Schema.NAME);
    }
}
