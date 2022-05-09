package com.salesforce.graph.cache;

import com.salesforce.CollectibleObject;
import com.salesforce.NullCollectibleObject;
import com.salesforce.graph.JustInTimeGraph;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Caches instances of {@link BaseSFVertex}. The cache provides two mechanisms caching.
 *
 * <ol>
 *   <li>Cache by (id,supplementalParam) pair
 *   <li>Cache by called defined {@link CacheKey} implementation
 * </ol>
 *
 * The general production workflow is the following.
 *
 * <ol>
 *   <li>A Tinkerpop graph is created and {@link #initialize(GraphTraversalSource)} is invoked on
 *       single VertexCache instance
 *   <li>All classes are compiled and loaded into a Tinkerpop graph(the fullGraph)
 *   <li>Each rule starts with a {@link JustInTimeGraph} which is a subset of the full graph
 *   <li>Each rule is only given access to the JITG. All queries are performed against the JITG.
 *       Vertices and edges are copied over from the fullGraph to the JITG whenever a missing vertex
 *       is referenced in a query
 *   <li>{@link com.salesforce.graph.vertex.SFVertexFactory} obtains the ids from the JITG but
 *       ALWAYS populates the {@link BaseSFVertex} from the fullGraph.
 * </ol>
 */
public interface VertexCache {
    /** Initialize the cache with the full graph. This should only be called once */
    void initialize(GraphTraversalSource fullGraph);

    /**
     * @return value previously passed to {@link #initialize(GraphTraversalSource)}. All {@link
     *     BaseSFVertex} classes are loaded from this graph. Even if their ids were previously found
     *     via a JustInTimeGraph.
     */
    GraphTraversalSource getFullGraph();

    /**
     * Retrieve the {@link BaseSFVertex} implementations that correspond to the (vertex,
     * supplementalParam) pair from the cache. The cache will be populated if the pair was not found
     *
     * @param vertices that were retrieved via a query on a just in time graph or the full graph
     * @param supplementalParam the supplemental parameter that distinguishes different subclasses
     *     where the vertices have the same id but different semantics depending on the path that
     *     contains them. See {@link com.salesforce.graph.vertex.StandardConditionVertex}
     * @param cacheMissFunction invoked if there is a cache miss to load the vertices
     * @return list of {@code T} that matches {@code vertices}
     */
    <T extends BaseSFVertex> List<T> get(
            List<Vertex> vertices,
            Object supplementalParam,
            Function<Map<Object, Object>, T> cacheMissFunction);

    /**
     * Generic cache function that stores the result of a query. This should be used in cases where
     * a given query is found to execute continuously and caching will help with performance.
     *
     * @param key created by the caller, there should be one CacheKey subclass for each use case
     * @param cacheMissFunction invoked if there is a cache miss to load the vertices. This function
     *     returns a {@link CollectibleObject} in order to support null values in the concurrent
     *     map. The {@code cacheMissFunction} must return a {@link NullCollectibleObject} instance
     *     instead of null.
     * @return vertex if it exists in the graph, else Optional.empty.
     */
    <T extends BaseSFVertex & CollectibleObject> Optional<T> get(
            CacheKey key, Supplier<CollectibleObject> cacheMissFunction);

    /**
     * Marker interface for keys used in {@link #get(CacheKey, Supplier)}. The caller should create
     * a new unique class with an overridden #equals and #hashCode method. See {@link
     * CaseInsensitiveCacheKey} and {@link VertexCacheKey}
     */
    interface CacheKey {}
}
