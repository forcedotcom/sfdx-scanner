package com.salesforce.graph;

import com.salesforce.SfgeTestExtension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link MetadataInfoProvider}. Initializes a test
 * with a unique {@link MetadataInfo} implementation for the current thread.
 */
public final class MetadataInfoTestProvider {
    /**
     * Create a new MetadataInfo for the current thread. This is invoked from {@link
     * SfgeTestExtension#beforeTestExecution(ExtensionContext)} for all tests.
     */
    public static void initializeForTest() {
        MetadataInfoProvider.METADATA_INFOS.set(new TestMetadataInfo());
    }

    /**
     * Remove the MetadataInfo from the current thread. This is invoked from {@link
     * SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} for all tests.
     */
    public static void remove() {
        MetadataInfoProvider.METADATA_INFOS.remove();
    }
}
