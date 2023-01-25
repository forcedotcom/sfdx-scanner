package com.salesforce.graph.ops.expander;

import com.google.common.collect.ImmutableMap;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.registry.AbstractRegistryData;
import com.salesforce.graph.ops.registry.Indexable;
import com.salesforce.graph.ops.registry.Registry;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Optimization to maintain lower heap space usage while expanding paths. See {@link Registry} for
 * information on how the optimization works.
 *
 * <p>This registry implementation holds data for {@link ApexPathCollapser}, {@link
 * ApexPathExpander}, and {@link ForkEvent} instances. Collections that need to hold instances of
 * these types, can hold their {@link Long} Ids instead, and use the corresponding <code>lookup()
 * </code> method to fetch the instance from registry.
 *
 * <p>{@link PathExpansionRegistry} uses instances of {@link AbstractRegistryData} rather than
 * {@link ThreadLocal} so that these hashmaps only grow as needed per path expansion of an ApexPath.
 * Once cleared, all heap usage will be released back. This makes sure: 1. Registry data lives for a
 * shorted duration of time and doesn't move to Old Gen of heap space until later. This allows
 * earlier clean up from heap space when they are not used. 2. Internally-used HashMaps can start
 * with smaller sizes and can grow as needed, as opposed to starting with a much larger HashMap that
 * was needed in a previous path expansion.
 *
 * <p>Create a new instance of this registry when a path expansion for an ApexPath is initiated at
 * {@link ApexPathExpanderUtil#expand(GraphTraversalSource, ApexPath, ApexPathExpanderConfig)}.
 * Clear the registry when the operation completes to avoid accidental memory leaks.
 */
public class PathExpansionRegistry extends Registry {

    private static final Map<Class<? extends Indexable>, Supplier> REGISTRY_SUPPLIER =
            ImmutableMap.of(
                    ApexPathCollapser.class, () -> new PathCollapserRegistryData(),
                    ForkEvent.class, () -> new ForkEventRegistryData(),
                    ApexPathExpander.class, () -> new ApexPathExpanderRegistryData());

    @Override
    protected Map<Class<? extends Indexable>, Supplier> getRegistrySupplier() {
        return REGISTRY_SUPPLIER;
    }

    /** Registry data structure to hold {@link ApexPathCollapser}. */
    private static class PathCollapserRegistryData extends AbstractRegistryData<ApexPathCollapser> {
        // Nothing new to add
    }

    /** Registry data structure to hold {@link ForkEvent}. */
    private static class ForkEventRegistryData extends AbstractRegistryData<ForkEvent> {
        // Nothing new to add
    }

    /** Registry data structure to hold {@link ApexPathExpander}. */
    private static class ApexPathExpanderRegistryData
            extends AbstractRegistryData<ApexPathExpander> {
        // Nothing new to add
    }

    ///// ApexPathCollapser registry methods//////

    public void registerApexPathCollapser(ApexPathCollapser apexPathCollapser) {
        register(ApexPathCollapser.class, apexPathCollapser);
    }

    public void validateApexPathCollapser(ApexPathCollapser apexPathCollapser) {
        verifyExists(ApexPathCollapser.class, apexPathCollapser);
    }

    public ApexPathCollapser lookupApexPathCollapser(Long pathCollapserId) {
        return (ApexPathCollapser) lookup(ApexPathCollapser.class, pathCollapserId);
    }

    public ApexPathCollapser deregisterApexPathCollapser(Long pathCollapserId) {
        return (ApexPathCollapser) deregister(ApexPathCollapser.class, pathCollapserId);
    }

    ///// ApexPathExpander registry methods//////

    public void registerApexPathExpander(ApexPathExpander apexPathExpander) {
        register(ApexPathExpander.class, apexPathExpander);
    }

    public void validateApexPathExpander(ApexPathExpander apexPathExpander) {
        verifyExists(ApexPathExpander.class, apexPathExpander);
    }

    public ApexPathExpander lookupApexPathExpander(Long apexPathExpanderId) {
        return (ApexPathExpander) lookup(ApexPathExpander.class, apexPathExpanderId);
    }

    public ApexPathExpander deregisterApexPathExpander(Long apexPathExpanderId) {
        return (ApexPathExpander) deregister(ApexPathExpander.class, apexPathExpanderId);
    }

    ///// ForkEvent registry methods//////

    public void registerForkEvent(ForkEvent forkEvent) {
        register(ForkEvent.class, forkEvent);
    }

    public void validateForkEvent(ForkEvent forkEvent) {
        verifyExists(ForkEvent.class, forkEvent);
    }

    public ForkEvent lookupForkEvent(Long forkEventId) {
        return (ForkEvent) lookup(ForkEvent.class, forkEventId);
    }

    public ForkEvent deregisterForkEvent(Long forkEventId) {
        return (ForkEvent) deregister(ForkEvent.class, forkEventId);
    }
}
