package com.salesforce.rules.unusedmethod.operations;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link UsageTrackerProvider}. Allows instantiation
 * of unique instances of {@link BaseUsageTracker} in each test.
 */
public class UsageTrackerTestProvider {
    /**
     * Create a new {@link BaseUsageTracker} for the current thread. This is invoked from {@link
     * com.salesforce.SfgeTestExtension#beforeTestExecution(ExtensionContext)} for all tests.
     */
    public static void initializeForTest() {
        UsageTrackerProvider.USAGE_TRACKER.set(new TestUsageTracker());
    }

    /**
     * Remove the {@link BaseUsageTracker} from the current thread. This is invoked from {@link
     * com.salesforce.SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} for all tests.
     */
    public static void remove() {
        UsageTrackerProvider.USAGE_TRACKER.remove();
    }
}
