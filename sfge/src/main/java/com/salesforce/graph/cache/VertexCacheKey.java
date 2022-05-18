package com.salesforce.graph.cache;

import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.Objects;

/**
 * Cache key when the query result was obtained using a vertex, such as trying to find the class
 * that corresponds to a {@link com.salesforce.graph.vertex.NewObjectExpressionVertex}. Each cache
 * use case needs to create a unique class that represents the specific use case. Key subclasses
 * should not be shared across use cases. See {@link ClassUtil}
 */
public abstract class VertexCacheKey implements VertexCache.CacheKey {
    private final BaseSFVertex vertex;
    private final int hash;

    protected VertexCacheKey(BaseSFVertex vertex) {
        this.vertex = vertex;
        // IMPORTANT: This was modified to include the class
        this.hash = Objects.hash(getClass(), this.vertex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertexCacheKey that = (VertexCacheKey) o;
        return Objects.equals(vertex, that.vertex);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "vertex=" + vertex + '}';
    }
}
