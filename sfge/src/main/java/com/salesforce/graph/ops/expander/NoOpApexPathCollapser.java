package com.salesforce.graph.ops.expander;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Implementation used when path collasping is not required */
final class NoOpApexPathCollapser implements ApexPathCollapser {
    @Override
    public void pathForked(
            ForkEvent forkEvent,
            ApexPathExpander originalExpander,
            List<ApexPathExpander> newExpanders) {}

    @Override
    public void resultReturned(
            ApexPathExpander apexPathExpander, ForkEvent forkEvent, Optional<?> apexValue)
            throws PathCollapsedException {}

    @Override
    public void removeExistingExpander(ApexPathExpander apexPathExpander) {}

    @Override
    public List<ApexPathExpander> clearCollapsedExpanders() {
        return Collections.emptyList();
    }

    public static ApexPathCollapser getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final NoOpApexPathCollapser INSTANCE = new NoOpApexPathCollapser();
    }

    private NoOpApexPathCollapser() {}
}
