package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

/** Example Apex: {@code List<SelectOption> options = new List<SelectOption>(); } */
public class NewListInitExpressionVertex extends AbstractCollectionExpressionVertex {
    NewListInitExpressionVertex(Map<Object, Object> properties) {
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
    public String getTypePrefix() {
        return ASTConstants.TypePrefix.LIST;
    }
}
