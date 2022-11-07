package com.salesforce.metainfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Maintains per-thread access to {@link MetaInfoCollector} instances. */
public final class MetaInfoCollectorProvider {

    static final ThreadLocal<Map<Class<?>, MetaInfoCollector>> ALL_INFO_COLLECTORS =
            ThreadLocal.withInitial(() -> buildAllInfoCollectorsMap());

    private static Map<Class<?>, MetaInfoCollector> buildAllInfoCollectorsMap() {
        final Map<Class<?>, MetaInfoCollector> classToInstanceMap = new HashMap<>();
        classToInstanceMap.put(VisualForceHandlerImpl.class, VisualForceHandlerImpl.getInstance());
        classToInstanceMap.put(
                CustomSettingInfoCollector.class, CustomSettingInfoCollector.getInstance());
        return classToInstanceMap;
    }

    /** Get VisualForceHandlerImpl for the current thread. */
    public static MetaInfoCollector getVisualForceHandler() {
        return ALL_INFO_COLLECTORS.get().get(VisualForceHandlerImpl.class);
    }

    /** Get CustomSettingsInfoCollector for the current thread. */
    public static MetaInfoCollector getCustomSettingsInfoCollector() {
        return ALL_INFO_COLLECTORS.get().get(CustomSettingInfoCollector.class);
    }

    /**
     * @return all MetaInfoCollectors
     */
    public static Collection<? extends MetaInfoCollector> getAllCollectors() {
        return ALL_INFO_COLLECTORS.get().values();
    }

    private MetaInfoCollectorProvider() {}
}
