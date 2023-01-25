package com.salesforce.graph.ops.expander;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.PathVertex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApexPathCollapserImpl implements ApexPathCollapser {
    private static final Logger LOGGER = LogManager.getLogger(ApexPathCollapserImpl.class);

    private final PathExpansionRegistry registry;
    private final Long pathExpansionId;

    /** This is the list of collapsers that the user has requested */
    private final List<ApexDynamicPathCollapser> dynamicPathCollapsers;

    /**
     * Maps a ForkEvent Id to all of the ApexPathExpander Ids that have the same ForkEvent and
     * returned a result. The ForkEvent passed to {@link #resultReturned(ApexPathExpander,
     * ForkEvent, Optional)} are used to find all other ApexPathExpanders that might potentially be
     * collapsed.
     */
    private final Map<Long, List<Long>> forkEventIdToApexExpanderIdsWithResults;

    /**
     * The list of Ids of all expanders that ApexPathExpanderUtil should remove on its next
     * iteration
     */
    private final List<Long> collapsedApexPathExpanderIds;

    ApexPathCollapserImpl(
            Long pathExpansionId,
            List<ApexDynamicPathCollapser> dynamicPathCollapsers,
            PathExpansionRegistry registry) {
        this.pathExpansionId = pathExpansionId;
        this.dynamicPathCollapsers = dynamicPathCollapsers;
        this.registry = registry;
        this.forkEventIdToApexExpanderIdsWithResults = new HashMap<>();
        this.collapsedApexPathExpanderIds = new ArrayList<>();
        if (dynamicPathCollapsers.isEmpty()) {
            throw new UnexpectedException("Use NoOpApexPathCollapser");
        }
    }

    @Override
    public Long getId() {
        return pathExpansionId;
    }

    @Override
    public List<ApexPathExpander> clearCollapsedExpanders() {
        List<ApexPathExpander> result =
                PathExpansionRegistryUtil.convertIdsToApexPathExpanders(
                        registry, collapsedApexPathExpanderIds);
        collapsedApexPathExpanderIds.clear();
        return result;
    }

    @Override
    public void pathForked(
            ForkEvent forkEvent,
            ApexPathExpander originalExpander,
            List<ApexPathExpander> newExpanders) {
        if (collapsedApexPathExpanderIds.contains(originalExpander.getId())) {
            throw new UnexpectedException("Pending removal was forked");
        }

        MethodVertex methodVertex = forkEvent.getMethodVertex();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Path forked. method="
                            + methodVertex.toSimpleString()
                            + ", forks="
                            + newExpanders.size());
        }

        removeExistingExpander(originalExpander);
        addNewExpanders(newExpanders);
    }

    @Override
    public void resultReturned(
            ApexPathExpander apexPathExpander, ForkEvent forkEvent, Optional<?> apexValue)
            throws PathCollapsedException {
        if (forkEvent == null) {
            throw new UnexpectedException(apexValue);
        }

        if (apexValue == null) {
            throw new UnexpectedException(forkEvent);
        }

        PathVertex pathVertex = forkEvent.getPathVertex();
        MethodVertex methodVertex = forkEvent.getMethodVertex();

        // Keep track of the result that was added
        final List<Long> apexPathExpanderIds =
                forkEventIdToApexExpanderIdsWithResults.computeIfAbsent(
                        forkEvent.getId(), k -> new ArrayList<>());
        final List<ApexPathExpander> apexPathExpanders =
                PathExpansionRegistryUtil.convertIdsToApexPathExpanders(
                        registry, apexPathExpanderIds);
        apexPathExpanders.add(apexPathExpander);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Result returned. method="
                            + methodVertex.toSimpleString()
                            + ", result="
                            + apexValue.orElse(null)
                            + ", apexPathExpanders="
                            + apexPathExpanders.size());
        }

        // Find all expanders that have a non-null result. These will be considered for collapsing
        List<ApexPathExpander> apexPathExpandersWithNonNullResults =
                apexPathExpanders.stream()
                        .filter(ape -> ape.getForkResult(pathVertex) != null)
                        .collect(Collectors.toList());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Result returned. method="
                            + methodVertex.toSimpleString()
                            + ", result="
                            + apexValue.orElse(null)
                            + ", apexPathExpanders="
                            + apexPathExpanders.size()
                            + ", apexPathExpandersWithNonNullResults="
                            + apexPathExpandersWithNonNullResults.size());
        }

        if (apexPathExpandersWithNonNullResults.size() == 1) {
            return;
        }

        List<ApexPathCollapseCandidate> candidates = new ArrayList<>();
        for (ApexPathExpander forkedApexPathExpander : apexPathExpandersWithNonNullResults) {
            Optional<ApexValue<?>> returnValue = forkedApexPathExpander.getForkResult(pathVertex);
            candidates.add(new ApexPathCollapseCandidate(forkedApexPathExpander, returnValue));
        }
        int originalCandidateSize = candidates.size();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Result returned. method="
                            + methodVertex.toSimpleString()
                            + ", candidates="
                            + originalCandidateSize);
        }
        if (originalCandidateSize < 2) {
            return;
        }

        for (ApexDynamicPathCollapser collapser : dynamicPathCollapsers) {
            if (!candidates.isEmpty() && collapser.mightCollapse(methodVertex)) {
                int size = candidates.size();
                candidates = collapser.collapse(methodVertex, candidates);
                if (size != candidates.size()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Collapsed originalSize="
                                        + size
                                        + ", newSize="
                                        + candidates.size()
                                        + ", collapser="
                                        + collapser.getClass().getSimpleName()
                                        + ", method="
                                        + methodVertex.toSimpleString());
                    }
                }
            }
        }

        if (candidates.size() != originalCandidateSize) {
            List<ApexPathExpander> retained =
                    candidates.stream()
                            .map(ApexPathCollapseCandidate::getApexPathExpander)
                            .collect(Collectors.toList());
            List<ApexPathExpander> collapsed =
                    apexPathExpandersWithNonNullResults.stream()
                            .filter(a -> !retained.contains(a))
                            .collect(Collectors.toList());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Collapsed. retained="
                                + candidates.stream()
                                        .map(c -> c.getReturnValue().orElse(null))
                                        .collect(Collectors.toList())
                                + ", collapsed="
                                + collapsed.size());
            }
            // Clean up
            if (retained.isEmpty()) {
                forkEventIdToApexExpanderIdsWithResults.remove(forkEvent.getId());
            } else {
                forkEventIdToApexExpanderIdsWithResults.put(
                        forkEvent.getId(),
                        PathExpansionRegistryUtil.convertApexPathExpandersToIds(
                                registry, retained));
            }

            // Throw an exception if the collapsed item is the currently executing one
            boolean needToThrow = false;
            for (ApexPathExpander collapsedPathExpander : collapsed) {
                if (collapsedPathExpander.equals(apexPathExpander)) {
                    needToThrow = true;
                } else {
                    collapsedApexPathExpanderIds.add(collapsedPathExpander.getId());
                }
                removeExistingExpander(collapsedPathExpander);
            }

            if (needToThrow) {
                throw new PathCollapsedException(apexPathExpander);
            }
        }
    }

    @Override
    public void removeExistingExpander(ApexPathExpander apexPathExpander) {
        // Remove the apexPathExpander the list of all returned results
        for (ForkEvent forkEvent : apexPathExpander.getForkEvents().values()) {
            final List<Long> apexPathExpanderIds =
                    forkEventIdToApexExpanderIdsWithResults.get(forkEvent.getId());

            if (apexPathExpanderIds != null) {
                final List<ApexPathExpander> apexPathExpanders =
                        PathExpansionRegistryUtil.convertIdsToApexPathExpanders(
                                registry, apexPathExpanderIds);
                apexPathExpanders.remove(apexPathExpander);
                if (apexPathExpanders.isEmpty()) {
                    // Remove the list if it is empty
                    forkEventIdToApexExpanderIdsWithResults.remove(forkEvent.getId());
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Removed apexPathExpander. newSize=" + apexPathExpanders.size());
                }
            }
        }
    }

    /** Adds new expanders that were added by a fork */
    private void addNewExpanders(List<ApexPathExpander> newExpanders) {
        for (ApexPathExpander newExpander : newExpanders) {
            for (ForkEvent forkEvent : newExpander.getForkEvents().values()) {

                List<Long> apexPathExpanderIds =
                        forkEventIdToApexExpanderIdsWithResults.computeIfAbsent(
                                forkEvent.getId(), k -> new ArrayList<>());

                List<ApexPathExpander> apexPathExpanders =
                        PathExpansionRegistryUtil.convertIdsToApexPathExpanders(
                                registry, apexPathExpanderIds);
                apexPathExpanders.add(newExpander);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "Added apexPathExpander. method="
                                    + forkEvent.getMethodVertex().toSimpleString()
                                    + ", newSize="
                                    + apexPathExpanders.size());
                }
            }
        }
    }
}
