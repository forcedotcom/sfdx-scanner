package com.salesforce.rules.unusedmethod.operations;

/** Non-singleton subclass of {@link BaseUsageTracker} for use in tests. */
public final class TestUsageTracker extends BaseUsageTracker {
    public TestUsageTracker() {
        super();
    }

    public boolean isUsed(String key) {
        return encounteredUsageKeys.contains(key);
    }
}
