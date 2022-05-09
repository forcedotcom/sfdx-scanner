package com.salesforce.graph.cache;

import com.salesforce.CollectibleObject;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.JustInTimeGraphProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Stores {@link BaseSFVertex} instances that were previously loaded from the Tinkerpop graph. All
 * vertices across the fullGraph and JustInTimeGraphs share the same ids and can be shared across
 * threads and rules.
 */
abstract class AbstractVertexCacheImpl implements VertexCache {
    /**
     * Mapping of (id,supplemental) parameter to vertex. {@link SFVertexFactory} performs queries on
     * the JustInTimeGraph in order to obtain the ids of the vertices. SFVertexFactory invokes this
     * class to obtains the BaseSFVertex that corresponds to the pair.
     */
    private final Map<Pair<Long, Object>, BaseSFVertex> vertexByIdAndParamCache;

    /** A generic cache that allows expensive queries to be cached */
    private final Map<CacheKey, CollectibleObject> genericCache;

    /**
     * The full graph that contains the complete set of Standard Apex and Custom Apex vertices. This
     * graph is used to retrieve all vertices in the {@link #get(List, Object, Function)}. In theory
     * these vertices could be loaded from any graph since the ids are identical. But keeping a hold
     * of the full graph simplifies the code and makes it a little easier to debug.
     */
    private GraphTraversalSource fullGraph;

    protected AbstractVertexCacheImpl() {
        this.vertexByIdAndParamCache = new ConcurrentHashMap<>();
        this.genericCache = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void initialize(GraphTraversalSource fullGraph) {
        if (this.fullGraph != null) {
            throw new UnexpectedException("Already initialized");
        }
        this.fullGraph = fullGraph;
    }

    /** {@inheritDoc} */
    @Override
    public GraphTraversalSource getFullGraph() {
        return this.fullGraph;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends BaseSFVertex> List<T> get(
            List<Vertex> vertices,
            Object supplementalParam,
            Function<Map<Object, Object>, T> cacheMissFunction) {
        // The list of results that will contain final results
        final TreeMap<Integer, T> orderedResults = new TreeMap<>();

        // List of all cache misses and their index in the orderedResults array. All of the misses
        // will be loaded in a
        // single call. The results will be merged into the orderedResults array.
        final TreeMap<Integer, Long> cacheMiss = new TreeMap<>();

        for (int i = 0; i < vertices.size(); i++) {
            // The key consists of (the id of the vertex, and the supplemental param).
            final Long id = (Long) vertices.get(i).id();
            final Pair<Long, Object> key = Pair.of(id, supplementalParam);
            final T cached = (T) vertexByIdAndParamCache.get(key);
            if (cached == null) {
                // This will create list with nulls mixed into the vertices. The nulls will be
                // filled in with a bulk
                // request below. This preserves the original order of the traversal
                cacheMiss.put(i, id);
                orderedResults.put(i, null);
            } else {
                orderedResults.put(i, cached);
            }
        }

        if (!cacheMiss.isEmpty()) {
            // Bulk load the cache misses and fill in the null values
            final List<Map<Object, Object>> propertyMaps =
                    fullGraph.V(cacheMiss.values()).elementMap().toList();
            int i = 0;
            for (Map.Entry<Integer, Long> entry : cacheMiss.entrySet()) {
                Pair<Long, Object> key = Pair.of(entry.getValue(), supplementalParam);
                // Invoke the cacheMissFunction that converts the map to the correct vertex type
                T vertex = cacheMissFunction.apply(propertyMaps.get(i));
                // We don't need to call JustInTimeGraph#loadUserClass here because the original
                // query executed against
                // the JITG and we are guaranteed the vertices are already present.
                vertexByIdAndParamCache.put(key, vertex);
                // Fill in the sparse map with the value
                orderedResults.put(entry.getKey(), vertex);
                i++;
            }
        }

        return new ArrayList<>(orderedResults.values());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends BaseSFVertex & CollectibleObject> Optional<T> get(
            CacheKey key, Supplier<CollectibleObject> cacheMissFunction) {
        final CollectibleObject result =
                genericCache.computeIfAbsent(key, k -> cacheMissFunction.get());
        final T t = (T) result.getCollectibleObject();
        if (t != null) {
            // The caller is interested in the DefiningType identified by the result. Seed the JITG
            // with this type in
            // case the caller queries for the vertex by id in the future.
            JustInTimeGraphProvider.get().loadUserClass(t.getDefiningType());
        }
        return Optional.ofNullable(t);
    }
}
