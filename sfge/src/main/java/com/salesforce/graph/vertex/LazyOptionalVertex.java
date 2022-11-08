package com.salesforce.graph.vertex;

import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Lazily queries for a vertex and caches the result, this differs from {@link LazyVertex} in that
 * the vertex IS NOT guaranteed to exist.
 *
 * @param <T> type of vertex that will be returned by the supplied gremlin query.
 */
public class LazyOptionalVertex<T extends BaseSFVertex>
        extends UncheckedLazyInitializer<Optional<T>> {
    private final Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier;

    /**
     * @param traversalSupplier a supplier that runs the desired gremlin query
     */
    LazyOptionalVertex(Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier) {
        this.traversalSupplier = traversalSupplier;
    }

    @Override
    protected Optional<T> initialize() throws ConcurrentException {
        final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
        return Optional.ofNullable(SFVertexFactory.loadSingleOrNull(g, traversalSupplier.get()));
    }

    @Override
    public String toString() {
        return "LazyVertex{" + "result=" + get() + '}';
    }
}
