package com.salesforce.rules.unusedmethod.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Set;

/**
 * Abstract helper class for use in {@link com.salesforce.rules.UnusedMethodRule}. Allows tracking
 * of which methods have or have not been used in a path.
 */
public abstract class BaseUsageTracker {
    protected final Set<String> encounteredUsageKeys;

    protected BaseUsageTracker() {
        encounteredUsageKeys = CollectionUtil.newTreeSet();
    }

    /** Mark the provided {@link MethodVertex} as one with a confirmed usage. */
    public void markAsUsed(MethodVertex methodVertex) {
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
