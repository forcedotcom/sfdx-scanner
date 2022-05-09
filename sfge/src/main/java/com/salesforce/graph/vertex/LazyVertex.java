package com.salesforce.graph.vertex;

import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.function.Supplier;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Lazily queries for a vertex and caches the result, this differs from {@link LazyOptionalVertex}
 * in that the vertex IS guaranteed to exist.
 *
 * @param <T> type of vertex that will be returned by the supplied gremlin query.
 */
final class LazyVertex<T extends BaseSFVertex> extends UncheckedLazyInitializer<T> {
    private final Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier;

    /** @param traversalSupplier a supplier that runs the desired gremlin query */
    LazyVertex(Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier) {
        this.traversalSupplier = traversalSupplier;
    }

    @Override
    protected T initialize() throws ConcurrentException {
        final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
        return SFVertexFactory.load(g, traversalSupplier.get());
    }

    @Override
    public String toString() {
        return "LazyVertex{" + "result=" + get() + '}';
    }
}
