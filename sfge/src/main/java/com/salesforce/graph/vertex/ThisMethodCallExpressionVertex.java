package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public final class ThisMethodCallExpressionVertex extends InvocableWithParametersVertex {
    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            // This differs from a regular method call. It doesn't have a reference
                            // expression
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    ThisMethodCallExpressionVertex(Map<Object, Object> properties) {
        super(properties, TRAVERSAL_CONSUMER);
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
}
