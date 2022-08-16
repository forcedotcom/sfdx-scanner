package com.salesforce.rules.ops;

import com.salesforce.SfgeTestExtension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link ProgressListenerProvider}. Initializes a
 * test with a unique {@link ProgressListener} implementation for the current thread.
 */
public class ProgressListenerTestProvider {
    /**
     * Create a new ProgressListenerImpl for the current thread. This is invoked from {@link
     * SfgeTestExtension#beforeTestExecution(ExtensionContext)} for all tests.
     */
    public static void initializeForTest() {
        ProgressListenerProvider.PROGRESS_LISTENER.set(new TestProgressListenerImpl());
    }

    /**
     * Remove the ProgressListener from the current thread. This is invoked from {@link
     * SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} for all tests.
     */
    public static void remove() {
        ProgressListenerProvider.PROGRESS_LISTENER.remove();
    }
}
