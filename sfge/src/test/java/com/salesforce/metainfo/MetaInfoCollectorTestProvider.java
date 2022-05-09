package com.salesforce.metainfo;

import com.salesforce.SfgeTestExtension;
import java.util.HashMap;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test implementation that overrides behavior of {@link MetaInfoCollectorProvider}. Allows a test
 * to override the {@link MetaInfoCollector} object for the current thread. Tests should always call
 * {@link #removeVisualForceHandler()} after the test has run.
 */
public class MetaInfoCollectorTestProvider {

    /**
     * Called by {@link SfgeTestExtension#beforeTestExecution(ExtensionContext)} to initialize state
     * of {@link MetaInfoCollectorProvider} to support individual tests
     */
    public static void initializeForTest() {
        final HashMap<Class<?>, MetaInfoCollector> infoCollectorMap = new HashMap<>();
        MetaInfoCollectorProvider.ALL_INFO_COLLECTORS.set(infoCollectorMap);
        setCustomSettingsInfoCollector(new CustomSettingInfoCollector());
        setVisualForceHandler(new VisualForceHandlerImpl());
    }

    /**
     * Called by {@link SfgeTestExtension#preDestroyTestInstance(ExtensionContext)} to destroy
     * test-specific singleton at the end of running each test.
     */
    public static void remove() {
        MetaInfoCollectorProvider.ALL_INFO_COLLECTORS.remove();
    }

    /**
     * Set {@link VisualForceHandlerImpl} for the current thread. Currently only to be used by
     * tests. {@link #removeVisualForceHandler()} should be called when the test ends.
     */
    public static void setVisualForceHandler(VisualForceHandlerImpl visualForceHandler) {
        MetaInfoCollectorProvider.ALL_INFO_COLLECTORS
                .get()
                .put(VisualForceHandlerImpl.class, visualForceHandler);
    }

    /**
     * Set {@link CustomSettingInfoCollector} for the current thread. {@link
     * #removeCustomSettingsInfoCollector()} should be called when the test ends.
     */
    public static void setCustomSettingsInfoCollector(
            CustomSettingInfoCollector customSettingsInfoCollector) {
        MetaInfoCollectorProvider.ALL_INFO_COLLECTORS
                .get()
                .put(CustomSettingInfoCollector.class, customSettingsInfoCollector);
    }

    /** Remove {@link VisualForceHandlerImpl} from the current thread. */
    public static void removeVisualForceHandler() {
        MetaInfoCollectorProvider.getAllCollectors().remove(VisualForceHandlerImpl.class);
    }

    /** Remove {@link CustomSettingInfoCollector} from current thread. */
    public static void removeCustomSettingsInfoCollector() {
        MetaInfoCollectorProvider.getAllCollectors().remove(CustomSettingInfoCollector.class);
    }
}
