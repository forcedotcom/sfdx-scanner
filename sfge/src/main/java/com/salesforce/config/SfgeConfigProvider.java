package com.salesforce.config;

import com.google.common.annotations.VisibleForTesting;

/** Maintains per thread access to {@link SfgeConfig} instances. */
public final class SfgeConfigProvider {
    @VisibleForTesting
    static final ThreadLocal<SfgeConfig> SFGE_CONFIGS =
            ThreadLocal.withInitial(() -> SfgeConfigImpl.getInstance());

    /** Get the SfgeConfig for the current thread */
    public static SfgeConfig get() {
        return SFGE_CONFIGS.get();
    }

    private SfgeConfigProvider() {}
}
