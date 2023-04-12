package com.salesforce.rules.unusedmethod.operations;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Optional;

/**
 * Helper class for use in {@link com.salesforce.rules.UnusedMethodRule}. Allows tracking of which
 * methods have or have not been used in a path.
 */
public class UsageTracker implements PathExpansionObserver {

    /**
     * Every time a new {@link ApexPath} is visited, mark the path's corresponding {@link
     * MethodVertex} as used.
     */
    public void onPathVisit(ApexPath path) {
        Optional<MethodVertex> methodOptional = path.getMethodVertex();
        methodOptional.ifPresent(this::markAsUsed);
    }

    /** Mark the provided {@link MethodVertex} as one with a confirmed usage. */
    private void markAsUsed(MethodVertex methodVertex) {
        UsageTrackerDataProvider.add(methodVertex.generateUniqueKey());
    }

    /** Return true if a confirmed usage of {@code methodVertex} has been found. */
    public boolean isUsed(String key) {
        return UsageTrackerDataProvider.contains(key);
    }
}
