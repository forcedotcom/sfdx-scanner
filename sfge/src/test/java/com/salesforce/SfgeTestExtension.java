package com.salesforce;

import com.salesforce.graph.MetadataInfoTestProvider;
import com.salesforce.graph.TestMetadataInfo;
import com.salesforce.graph.cache.VertexCacheTestProvider;
import com.salesforce.metainfo.MetaInfoCollectorTestProvider;
import com.salesforce.rules.ops.ProgressListenerTestProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

/**
 * Extension class that has callbacks which are executed before and after each test. This class is
 * registered as a Java service and configured in the <code>META-INF/services</code> directory.
 */
public final class SfgeTestExtension
        implements BeforeTestExecutionCallback, TestInstancePreDestroyCallback {
    private static final Logger LOGGER = LogManager.getLogger(SfgeTestExtension.class);

    /**
     * Provide additional behavior to tests immediately before an individual test is executed but
     * after any user-defined setup methods (e.g., @BeforeEach methods) have been executed for that
     * test. See {@link BeforeTestExecutionCallback}
     *
     * <p>Initializes each test with a unique thread local {@link TestMetadataInfo}
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        LOGGER.info("beforeTestExecution name={}", context.getDisplayName());
        MetadataInfoTestProvider.initializeForTest();
        VertexCacheTestProvider.initializeForTest();
        MetaInfoCollectorTestProvider.initializeForTest();
        ProgressListenerTestProvider.initializeForTest();
    }

    /**
     * Callback for processing test instances before they are destroyed. See {@link
     * TestInstancePreDestroyCallback}
     *
     * <p>Removes the thread local {@link TestMetadataInfo}
     */
    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
        LOGGER.info("preDestroyTestInstance name={}", context.getDisplayName());
        MetadataInfoTestProvider.remove();
        VertexCacheTestProvider.remove();
        MetaInfoCollectorTestProvider.remove();
        ProgressListenerTestProvider.remove();
    }
}
