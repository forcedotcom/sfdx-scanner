package com.salesforce.graph.vertex;

import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Lazily queries for a vertex and caches the result, this differs from {@link MappedLazyVertex} in
 * that the vertex IS NOT guaranteed to exist.
 *
 * @param <T> type of parameter passed to the {@code traversalFunction}
 * @param <U> type of vertex returned from {@code traversalFunction}
 */
public class MappedLazyOptionalVertex<T, U extends BaseSFVertex> {
    private final Function<T, GraphTraversal<Vertex, Vertex>> traversalFunction;
    private final Map<T, Optional<U>> map;

    /**
     * @param traversalFunction a function that runs the desired gremlin query accepting a single
     *     parameter of T
     */
    MappedLazyOptionalVertex(Function<T, GraphTraversal<Vertex, Vertex>> traversalFunction) {
        this.traversalFunction = traversalFunction;
        this.map = new ConcurrentHashMap<>();
    }

    /** @return the vertex that was loaded from the graph */
    public Optional<U> get(T parameter) {
        final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
        return map.computeIfAbsent(
                parameter,
                k ->
                        Optional.ofNullable(
                                SFVertexFactory.loadSingleOrNull(
                                        g, traversalFunction.apply(parameter))));
    }

    @Override
    public String toString() {
        return "MappedLazyOptionalVertex{" + "map=" + map + '}';
    }
}
