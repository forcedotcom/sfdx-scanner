package com.salesforce.graph.cache;

/** Extension of {@link AbstractVertexCacheImpl} that guarantees it is a singleton */
final class VertexCacheImpl extends AbstractVertexCacheImpl {
    private VertexCacheImpl() {}

    static VertexCache getInstance() {
        return VertexCacheImpl.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final VertexCacheImpl INSTANCE = new VertexCacheImpl();
    }
}
