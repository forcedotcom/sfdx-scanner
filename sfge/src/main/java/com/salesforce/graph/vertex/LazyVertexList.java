package com.salesforce.graph.vertex;

import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Lazily queries for a list of vertices and caches the result.
 *
 * @param <T> type of vertex that will be returned by the supplied gremlin query.
 */
final class LazyVertexList<T extends BaseSFVertex> extends UncheckedLazyInitializer<List<T>> {
    private final Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier;
    private final Object supplementalParam;

    /** @param traversalSupplier a supplier that runs the desired gremlin query */
    LazyVertexList(Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier) {
        this(traversalSupplier, null);
    }

    /**
     * @param traversalSupplier a supplier that runs the desired gremlin query
     * @param supplementalParam parameter passed to {@link
     *     SFVertexFactory#loadVertices(GraphTraversalSource, GraphTraversal, Object)}
     */
    LazyVertexList(
            Supplier<GraphTraversal<Vertex, Vertex>> traversalSupplier, Object supplementalParam) {
        this.traversalSupplier = traversalSupplier;
        this.supplementalParam = supplementalParam;
    }

    @Override
    protected List<T> initialize() throws ConcurrentException {
        final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
        if (supplementalParam == null) {
            return Collections.unmodifiableList(
                    SFVertexFactory.loadVertices(g, traversalSupplier.get()));
        } else {
            return Collections.unmodifiableList(
                    SFVertexFactory.loadVertices(g, traversalSupplier.get(), supplementalParam));
        }
    }

    @Override
    public String toString() {
        return "LazyVertexList{" + "result=" + get() + '}';
    }
}
