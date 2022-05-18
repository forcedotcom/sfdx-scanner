package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class FieldDeclarationVertex extends DeclarationVertex implements Typeable {
    FieldDeclarationVertex(Map<Object, Object> properties) {
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

    public ModifierNodeVertex getModifierNode() {
        return getPreviousSibling();
    }

    public boolean isStatic() {
        ModifierNodeVertex modifierNode = getPreviousSibling();
        return modifierNode.isStatic();
    }

    @Override
    public String getCanonicalType() {
        return ApexStandardLibraryUtil.getCanonicalName(getString(Schema.TYPE));
    }
}
