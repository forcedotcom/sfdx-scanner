package com.salesforce.rules.unusedmethod.operations;

import com.google.common.annotations.VisibleForTesting;

/** Provides threadsafe access to a singleton instance of {@link UsageTrackerData}. */
public class UsageTrackerDataProvider {
    // Default usage tracker instance can be replaced with a different one for testing.
    @VisibleForTesting
    static final ThreadLocal<UsageTrackerData> USAGE_TRACKER =
            ThreadLocal.withInitial(UsageTrackerData::getInstance);

    /** Add a key to the underlying {@link UsageTrackerData}. */
    public static void add(String key) {
        USAGE_TRACKER.get().add(key);
    }

    /** Indicate whether the underlying {@link UsageTrackerData} contains the desired key. */
    public static boolean contains(String key) {
        return USAGE_TRACKER.get().contains(key);
    }

    private UsageTrackerDataProvider() {}
}
