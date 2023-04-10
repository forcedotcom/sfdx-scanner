package com.salesforce.rules.unusedmethod.operations;

import com.google.common.annotations.VisibleForTesting;

/** Provides threadsafe access to an instance of a {@link BaseUsageTracker} subclass. */
public class UsageTrackerProvider {

    // By default, we use the singleton variant. But tests can replace that instance with a
    // different one if needed.
    @VisibleForTesting
    static final ThreadLocal<BaseUsageTracker> USAGE_TRACKER =
            ThreadLocal.withInitial(SingletonUsageTracker::getInstance);

    /** Get the BaseUsageTracker for the current thread. */
    public static BaseUsageTracker get() {
        return USAGE_TRACKER.get();
    }

    private UsageTrackerProvider() {}
}
