package com.salesforce.graph.ops.expander;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helps translate between lists of instances and their Ids while looking up {@link
 * PathExpansionRegistry}.
 */
public final class PathExpansionRegistryUtil {
    private PathExpansionRegistryUtil() {}

    /** Convert a list of ApexPathExpander Ids to a list of ApexPathExpander instances. */
    public static List<ApexPathExpander> convertIdsToApexPathExpanders(
            PathExpansionRegistry registry, List<Long> apexPathExpanderIds) {
        if (apexPathExpanderIds.isEmpty()) {
            return new ArrayList<>();
        }
        return apexPathExpanderIds.stream()
                .map(id -> registry.lookupApexPathExpander(id))
                .collect(Collectors.toList());
    }

    /** Convert a list of ApexPathExpander instances to a list of ApexPathExpander Ids. */
    public static List<Long> convertApexPathExpandersToIds(
            PathExpansionRegistry registry, List<ApexPathExpander> apexPathExpanders) {
        if (apexPathExpanders.isEmpty()) {
            return new ArrayList<>();
        }
        return apexPathExpanders.stream()
                .map(
                        apexPathExpander -> {
                            registry.validateApexPathExpander(apexPathExpander);
                            return apexPathExpander.getId();
                        })
                .collect(Collectors.toList());
    }

    /** Convert a list of ForkEvent Ids to a list of ForkEvent instances. */
    public static List<ForkEvent> convertIdsToForkEvents(
            PathExpansionRegistry registry, List<Long> forkEventIds) {
        if (forkEventIds.isEmpty()) {
            return new ArrayList<>();
        }
        return forkEventIds.stream()
                .map(id -> registry.lookupForkEvent(id))
                .collect(Collectors.toList());
    }

    /** Convert a list of ForkEvent instances to a list of ForkEvent Ids. */
    public static List<Long> convertForkEventsToIds(
            PathExpansionRegistry registry, List<ForkEvent> forkEvents) {
        if (forkEvents.isEmpty()) {
            return new ArrayList<>();
        }
        return forkEvents.stream()
                .map(
                        forkEvent -> {
                            registry.validateForkEvent(forkEvent);
                            return forkEvent.getId();
                        })
                .collect(Collectors.toList());
    }
}
