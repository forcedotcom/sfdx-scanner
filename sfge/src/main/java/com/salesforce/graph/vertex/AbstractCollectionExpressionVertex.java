package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/** Holds type-matching logic for collection objects defined through "new" keyword. */
abstract class AbstractCollectionExpressionVertex extends InvocableWithParametersVertex
        implements NewCollectionExpressionVertex {
    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    protected AbstractCollectionExpressionVertex(Map<Object, Object> properties) {
        super(properties, TRAVERSAL_CONSUMER);
    }

    @Override
    public final String getCanonicalType() {
        return getString(Schema.TYPE);
    }
}
