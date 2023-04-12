package com.salesforce.rules.unusedmethod.operations;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link UsageTrackerDataProvider}. Allows
 * instantiation of unique instances of {@link UsageTrackerData} in each test.
 */
public class UsageTrackerDataTestProvider {
    /**
     * Create a new {@link UsageTrackerData} for the current thread. This is invoked from {@link
     * com.salesforce.SfgeTestExtension#beforeTestExecution(ExtensionContext)} for all tests.
     */
    public static void initializeForTest() {
        UsageTrackerDataProvider.USAGE_TRACKER.set(new UsageTrackerData());
    }

    /**
     * Remove the {@link UsageTrackerData} from the current thread. This is invoked from {@link
     * com.salesforce.SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} for all tests.
     */
    public static void remove() {
        UsageTrackerDataProvider.USAGE_TRACKER.remove();
    }
}
