package com.salesforce.graph.cache;

import com.google.common.annotations.VisibleForTesting;

public final class VertexCacheProvider {
    @VisibleForTesting
    static final ThreadLocal<VertexCache> VERTEX_CACHES =
            ThreadLocal.withInitial(() -> VertexCacheImpl.getInstance());

    /** Get the VertexCache for the current thread */
    public static VertexCache get() {
        return VERTEX_CACHES.get();
    }

    private VertexCacheProvider() {}
}
