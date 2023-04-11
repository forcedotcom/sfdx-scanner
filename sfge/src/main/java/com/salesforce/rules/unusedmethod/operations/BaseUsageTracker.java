package com.salesforce.rules.unusedmethod.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract helper class for use in {@link com.salesforce.rules.UnusedMethodRule}. Allows tracking
 * of which methods have or have not been used in a path.
 */
public abstract class BaseUsageTracker implements PathExpansionObserver {
    protected final Set<String> encounteredUsageKeys;

    protected BaseUsageTracker() {
        encounteredUsageKeys = CollectionUtil.newTreeSet();
    }

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
        encounteredUsageKeys.add(generateUsageKey(methodVertex));
    }

    /** Return true if a confirmed usage of {@code methodVertex} has been found. */
    public boolean isUsed(MethodVertex methodVertex) {
        return encounteredUsageKeys.contains(generateUsageKey(methodVertex));
    }

    /** Generate a unique key associated with this method, to track its usage. */
    private static String generateUsageKey(MethodVertex methodVertex) {
        // Format of keys is "definingType#methodName@beginLine".
        return methodVertex.getDefiningType()
                + "#"
                + methodVertex.getName()
                + "@"
                + methodVertex.getBeginLine();
    }
}
