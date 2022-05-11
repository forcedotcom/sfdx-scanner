package com.salesforce.graph;

import com.google.common.annotations.VisibleForTesting;

public final class MetadataInfoProvider {
    @VisibleForTesting
    static final ThreadLocal<MetadataInfo> METADATA_INFOS =
            ThreadLocal.withInitial(() -> MetadataInfoImpl.getInstance());

    /** Get the MetadataInfo for the current thread */
    public static MetadataInfo get() {
        return METADATA_INFOS.get();
    }

    private MetadataInfoProvider() {}
}
