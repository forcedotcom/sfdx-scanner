package com.salesforce.graph.cache;

import com.salesforce.SfgeTestExtension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link VertexCacheProvider}. Initializes a test
 * with a unique {@link VertexCache} implementation for the current thread.
 */
public final class VertexCacheTestProvider {
    /**
     * Create a new VertexCache for the current thread. This is invoked from {@link
     * SfgeTestExtension#beforeTestExecution(ExtensionContext)} for all tests.
     */
    public static void initializeForTest() {
        initializeForTest(new TestVertexCacheImpl());
    }

    /**
     * Set <code>vertexCache</code> for the current thread. This is used in more advanced use cases
     * where the value initialized by {@link
     * SfgeTestExtension#beforeTestExecution(ExtensionContext)} is not sufficient. Such as when the
     * vertexCache must be shared across threads.
     */
    public static void initializeForTest(VertexCache vertexCache) {
        VertexCacheProvider.VERTEX_CACHES.set(vertexCache);
    }

    /**
     * Remove the VertexCache from the current thread. This is invoked from {@link
     * SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} for all tests.
     */
    public static void remove() {
        VertexCacheProvider.VERTEX_CACHES.remove();
    }
}
