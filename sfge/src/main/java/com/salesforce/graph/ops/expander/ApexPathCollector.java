package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures only the ApexPath of interest, allows the rest of the ApexPathExpander objects to be
 * garbage collected.
 */
final class ApexPathCollector extends ResultCollector {
    /**
     * Keep a map of the Expander Id to the ApexPath. ApexPath prevents equality comparisons to
     * avoid bugs
     */
    private final Map<Long, ApexPath> expanderIdToPath;

    ApexPathCollector() {
        this.expanderIdToPath = new HashMap<>();
    }

    List<ApexPath> getResults() {
        return new ArrayList<>(expanderIdToPath.values());
    }

    @Override
    boolean remove(ApexPathExpander pathExpander) {
        Long key = pathExpander.getId();
        return expanderIdToPath.remove(key) != null;
    }

    @Override
    void _collect(ApexPathExpander pathExpander) {
        Long key = pathExpander.getId();
        ApexPath path = pathExpander.getTopMostPath();
        expanderIdToPath.put(key, path);
        pathExpander.finished();
    }

    @Override
    int size() {
        return expanderIdToPath.size();
    }
}
