package com.salesforce.rules.unusedmethod.operations;

import com.salesforce.collections.CollectionUtil;
import java.util.Set;

/** Singleton class allowing {@link UsageTracker} to have thread-safe access to usage data. */
class UsageTrackerData {
    final Set<String> encounteredUsageKeys;

    UsageTrackerData() {
        encounteredUsageKeys = CollectionUtil.newTreeSet();
    }

    synchronized void add(String key) {
        encounteredUsageKeys.add(key);
    }

    synchronized boolean contains(String key) {
        return encounteredUsageKeys.contains(key);
    }

    static UsageTrackerData getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final UsageTrackerData INSTANCE = new UsageTrackerData();
    }
}
