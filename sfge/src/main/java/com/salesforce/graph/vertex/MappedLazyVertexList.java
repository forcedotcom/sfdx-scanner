package com.salesforce.graph.vertex;

import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Lazily queries for a list of vertices and caches the result, this differs from {@link
 * LazyVertexList} in that the function accepts a single parameter of type T
 *
 * @param <T> type of parameter passed to the {@code traversalFunction}
 * @param <U> type of vertices returned by {@code traversalFunction}
 */
public class MappedLazyVertexList<T, U extends BaseSFVertex> {
    private final Function<T, GraphTraversal<Vertex, Vertex>> traversalFunction;
    private final Map<T, List<U>> map;

    /**
     * @param traversalFunction a function that runs the desired gremlin query accepting a single
     *     parameter of T
     */
    MappedLazyVertexList(Function<T, GraphTraversal<Vertex, Vertex>> traversalFunction) {
        this.traversalFunction = traversalFunction;
        this.map = new ConcurrentHashMap<>();
    }

    /** @return the list that was loaded from the graph */
    public List<U> get(T parameter) {
        final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
        return map.computeIfAbsent(
                parameter,
                k ->
                        Collections.unmodifiableList(
                                SFVertexFactory.loadVertices(
                                        g, traversalFunction.apply(parameter))));
    }

    @Override
    public String toString() {
        return "LazyVertex{" + "map=" + map + '}';
    }
}
