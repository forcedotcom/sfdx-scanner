package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class FieldDeclarationStatementsVertex extends FieldWithModifierVertex {
    // TODO: Make lazy
    private final FieldDeclarationVertex fieldDeclaration;

    FieldDeclarationStatementsVertex(Map<Object, Object> properties) {
        super(properties);
        this.fieldDeclaration = getOnlyChild(ASTConstants.NodeType.FIELD_DECLARATION);
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

    public FieldDeclarationVertex getFieldDeclaration() {
        return fieldDeclaration;
    }
}
