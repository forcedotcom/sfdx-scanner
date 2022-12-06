package com.salesforce.graph.cache;

import com.salesforce.graph.ops.ClassUtil;
import java.util.Locale;
import java.util.Objects;

/**
 * Cache key when the query result was obtained using a case-insensitive value, such as a class
 * name. Each cache use case needs to create a unique class that represents the specific use case.
 * Key subclasses should not be shared across use cases. See {@link ClassUtil}
 */
public abstract class CaseInsensitiveCacheKey implements VertexCache.CacheKey {
    private final String key;
    private final int hash;

    /**
     * @param key that was used to query the graph
     */
    protected CaseInsensitiveCacheKey(String key) {
        this.key = key.toLowerCase(Locale.ROOT);
        // IMPORTANT: This was modified to include the class
        this.hash = Objects.hash(getClass(), this.key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseInsensitiveCacheKey that = (CaseInsensitiveCacheKey) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "key='" + key + '\'' + '}';
    }
}
