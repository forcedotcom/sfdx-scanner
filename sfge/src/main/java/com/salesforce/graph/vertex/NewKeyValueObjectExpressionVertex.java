package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

/** Typically represents an object such as Account which is created with a set of values. */
// TODO: This is common with NewListLiteral
public class NewKeyValueObjectExpressionVertex extends ChainedVertex implements Typeable {
    private final LazyVertexList<ChainedVertex> items;

    NewKeyValueObjectExpressionVertex(Map<Object, Object> properties) {
        super(properties, null);
        this.items = _getItems();
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
    public boolean isResolvable() {
        return false;
    }

    public List<ChainedVertex> getItems() {
        return this.items.get();
    }

    private LazyVertexList<ChainedVertex> _getItems() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc),
                ExpressionType.KEY_VALUE);
    }
}
