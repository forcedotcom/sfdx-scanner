package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimization to reduce the number of objects that are kept around while expanding paths. When an
 * {@link ApexPathExpander} is accepted, its associated {@link ApexPath} is captured. When it is
 * rejected, the {@link PathExpansionException} that caused the rejection is captured. Either way,
 * the rest of the {@link ApexPathExpander} can be discarded and can be garbage-collected, thus
 * relieving memory pressure.
 */
public final class ApexPathCollector {
    /**
     * Keep a map of the Expander Id to the ApexPath. ApexPath prevents equality comparisons to
     * avoid bugs
     */
    private final Map<Long, ApexPath> expanderIdToPath;
    /**
     * Mapping from the IDs of expanders whose paths were rejected to the reasons for those
     * rejections.
     */
    private final Map<Long, PathExpansionException> expanderIdToRejectionReason;

    ApexPathCollector() {
        this.expanderIdToPath = new HashMap<>();
        this.expanderIdToRejectionReason = new HashMap<>();
    }

    public List<ApexPath> getAcceptedResults() {
        return new ArrayList<>(expanderIdToPath.values());
    }

    public List<PathExpansionException> getRejectionReasons() {
        return new ArrayList<>(expanderIdToRejectionReason.values());
    }

    void collectAccepted(ApexPathExpander pathExpander) {
        // Copy over information to each path which the expander has been accumulating.
        // TODO: This is sharing information between the original path and initialization paths.
        //       We're pretty sure that's fine, but we should keep an eye on it.
        pathExpander.getTopMostPath().setApexPathMetaInfo(pathExpander.getApexPathVertexMetaInfo());
        Long key = pathExpander.getId();
        ApexPath path = pathExpander.getTopMostPath();
        expanderIdToPath.put(key, path);
        pathExpander.finished();
    }

    void collectRejected(ApexPathExpander pathExpander, PathExpansionException reason) {
        Long key = pathExpander.getId();
        expanderIdToRejectionReason.put(key, reason);
        pathExpander.finished();
    }

    /**
     * Remove the {@link ApexPath} corresponding to {@code pathExpander} if it has been previously
     * collected. This is typically because the result has been collapsed and superseded by another
     * result.
     *
     * @return True if the associated path was removed
     */
    boolean removeAccepted(ApexPathExpander pathExpander) {
        Long key = pathExpander.getId();
        return expanderIdToPath.remove(key) != null;
    }

    /**
     * @return The number of paths accepted so far
     */
    int acceptedSize() {
        return expanderIdToPath.size();
    }

    /**
     * @return The number of paths rejected so far
     */
    int rejectedSize() {
        return expanderIdToRejectionReason.size();
    }
}
