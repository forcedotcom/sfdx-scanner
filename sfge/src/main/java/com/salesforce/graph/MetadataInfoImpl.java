package com.salesforce.graph;

/** Extension of {@link AbstractMetadataInfoImpl} that guarantees it is a singleton */
public final class MetadataInfoImpl extends AbstractMetadataInfoImpl {
    static MetadataInfo getInstance() {
        return MetadataInfoImpl.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final MetadataInfoImpl INSTANCE = new MetadataInfoImpl();
    }

    private MetadataInfoImpl() {}
}
