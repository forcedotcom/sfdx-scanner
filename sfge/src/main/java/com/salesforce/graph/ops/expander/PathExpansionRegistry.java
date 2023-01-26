package com.salesforce.graph.ops.expander;

import com.google.common.collect.ImmutableMap;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.registry.Registrable;
import com.salesforce.graph.ops.registry.Registry;
import com.salesforce.graph.ops.registry.RegistryData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
 * <p>{@link PathExpansionRegistry} uses instances of {@link RegistryData} rather than {@link
 * ThreadLocal} so that these hashmaps only grow as needed per path expansion of an ApexPath. Once
 * cleared, all heap usage will be released back. This makes sure:
 *
 * <ul>
 *   <li>1. Registry data lives for a shorted duration of time and doesn't move to Old Gen of heap
 *       space until later. This allows earlier clean up from heap space when they are not used.
 *   <li>2. Internally-used HashMaps can start with smaller sizes and can grow as needed, as opposed
 *       to starting with a much larger HashMap that was needed in a previous path expansion.
 * </ul>
 *
 * <p>Create a new instance of this registry when a path expansion for an ApexPath is initiated at
 * {@link ApexPathExpanderUtil#expand(GraphTraversalSource, ApexPath, ApexPathExpanderConfig)}.
 * Clear the registry when the operation completes to avoid accidental memory leaks.
 */
public class PathExpansionRegistry extends Registry {

    private static final Map<Class<? extends Registrable>, Supplier> REGISTRY_SUPPLIER =
            ImmutableMap.of(
                    ForkEvent.class, () -> new ForkEventRegistryData(),
                    ApexPathExpander.class, () -> new ApexPathExpanderRegistryData());

    @Override
    protected Map<Class<? extends Registrable>, Supplier> getRegistrySupplier() {
        return REGISTRY_SUPPLIER;
    }

    // Keeping these as concrete classes so that their names show up
    // in performance profiler tools. This makes performance and heap usage analysis easier.

    /** Registry data structure to hold {@link ForkEvent}. */
    private static class ForkEventRegistryData extends RegistryData<ForkEvent> {
        // Nothing new to add
    }

    /** Registry data structure to hold {@link ApexPathExpander}. */
    private static class ApexPathExpanderRegistryData extends RegistryData<ApexPathExpander> {
        // Nothing new to add
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

    /** Convert a list of ApexPathExpander Ids to a list of ApexPathExpander instances. */
    public List<ApexPathExpander> convertIdsToApexPathExpanders(List<Long> apexPathExpanderIds) {
        if (apexPathExpanderIds.isEmpty()) {
            return new ArrayList<>();
        }
        return apexPathExpanderIds.stream()
                .map(id -> lookupApexPathExpander(id))
                .collect(Collectors.toList());
    }

    /** Convert a list of ApexPathExpander instances to a list of ApexPathExpander Ids. */
    public List<Long> convertApexPathExpandersToIds(List<ApexPathExpander> apexPathExpanders) {
        if (apexPathExpanders.isEmpty()) {
            return new ArrayList<>();
        }
        return apexPathExpanders.stream()
                .map(
                        apexPathExpander -> {
                            validateApexPathExpander(apexPathExpander);
                            return apexPathExpander.getId();
                        })
                .collect(Collectors.toList());
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

    /** Convert a list of ForkEvent Ids to a list of ForkEvent instances. */
    public List<ForkEvent> convertIdsToForkEvents(List<Long> forkEventIds) {
        if (forkEventIds.isEmpty()) {
            return new ArrayList<>();
        }
        return forkEventIds.stream().map(id -> lookupForkEvent(id)).collect(Collectors.toList());
    }

    /** Convert a list of ForkEvent instances to a list of ForkEvent Ids. */
    public List<Long> convertForkEventsToIds(List<ForkEvent> forkEvents) {
        if (forkEvents.isEmpty()) {
            return new ArrayList<>();
        }
        return forkEvents.stream()
                .map(
                        forkEvent -> {
                            validateForkEvent(forkEvent);
                            return forkEvent.getId();
                        })
                .collect(Collectors.toList());
    }
}
