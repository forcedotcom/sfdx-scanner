package com.salesforce.graph.ops.expander;

import com.google.common.collect.ImmutableMap;
import com.salesforce.graph.ops.registry.AbstractRegistryData;
import com.salesforce.graph.ops.registry.Registry;
import java.util.Map;
import java.util.function.Supplier;

public class PathExpansionRegistry extends Registry {

    private static final Map<Class, Supplier> REGISTRY_SUPPLIER =
            ImmutableMap.of(
                    ApexPathCollapser.class, () -> new PathCollapserRegistryData(),
                    ForkEvent.class, () -> new ForkEventRegistryData(),
                    ApexPathExpander.class, () -> new ApexPathExpanderRegistryData());

    @Override
    protected Map<Class, Supplier> getRegistrySupplier() {
        return REGISTRY_SUPPLIER;
    }

    private static class PathCollapserRegistryData extends AbstractRegistryData<ApexPathCollapser> {
        // Nothing new to add
    }

    private static class ForkEventRegistryData extends AbstractRegistryData<ForkEvent> {
        // Nothing new to add
    }

    private static class ApexPathExpanderRegistryData
            extends AbstractRegistryData<ApexPathExpander> {
        // Nothing new to add
    }

    /** ApexPathCollapser * */
    public void registerApexPathCollapser(ApexPathCollapser apexPathCollapser) {
        register(ApexPathCollapser.class, apexPathCollapser);
    }

    public void validateApexPathCollapser(ApexPathCollapser apexPathCollapser) {
        validate(ApexPathCollapser.class, apexPathCollapser);
    }

    public ApexPathCollapser lookupApexPathCollapser(Long pathCollapserId) {
        return (ApexPathCollapser) lookup(ApexPathCollapser.class, pathCollapserId);
    }

    public ApexPathCollapser deregisterApexPathCollapser(Long pathCollapserId) {
        return (ApexPathCollapser) deregister(ApexPathCollapser.class, pathCollapserId);
    }

    /** ApexPathExpander * */
    public void registerApexPathExpander(ApexPathExpander apexPathExpander) {
        register(ApexPathExpander.class, apexPathExpander);
    }

    public void validateApexPathExpander(ApexPathExpander apexPathExpander) {
        validate(ApexPathExpander.class, apexPathExpander);
    }

    public ApexPathExpander lookupApexPathExpander(Long apexPathExpanderId) {
        return (ApexPathExpander) lookup(ApexPathExpander.class, apexPathExpanderId);
    }

    public ApexPathExpander deregisterApexPathExpander(Long apexPathExpanderId) {
        return (ApexPathExpander) deregister(ApexPathExpander.class, apexPathExpanderId);
    }

    /** ForkEvent * */
    public void registerForkEvent(ForkEvent forkEvent) {
        register(ForkEvent.class, forkEvent);
    }

    public void validateForkEvent(ForkEvent forkEvent) {
        validate(ForkEvent.class, forkEvent);
    }

    public ForkEvent lookupForkEvent(Long forkEventId) {
        return (ForkEvent) lookup(ForkEvent.class, forkEventId);
    }

    public ForkEvent deregisterForkEvent(Long forkEventId) {
        return (ForkEvent) deregister(ForkEvent.class, forkEventId);
    }
}
