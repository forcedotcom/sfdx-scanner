package com.salesforce.rules.unusedmethod.operations;

/**
 * Singleton extension of {@link BaseUsageTracker}, for use in {@link
 * com.salesforce.rules.UnusedMethodRule}.
 */
public final class SingletonUsageTracker extends BaseUsageTracker {
    private SingletonUsageTracker() {
        super();
    }

    static SingletonUsageTracker getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final SingletonUsageTracker INSTANCE = new SingletonUsageTracker();
    }
}
