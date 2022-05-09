package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

public final class ValueWhenBlockVertex extends WhenBlockVertex {
    private final LazyVertexList<CaseVertex> caseVertices;

    ValueWhenBlockVertex(Map<Object, Object> properties) {
        super(properties);
        this.caseVertices = _getCaseVertices();
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

    /** The list of all cases covered in the case statement */
    public List<CaseVertex> getCaseVertices() {
        return caseVertices.get();
    }

    private LazyVertexList<CaseVertex> _getCaseVertices() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(
                                        ASTConstants.NodeType.IDENTIFIER_CASE,
                                        ASTConstants.NodeType.LITERAL_CASE)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }
}
