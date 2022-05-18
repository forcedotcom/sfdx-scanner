package com.salesforce.graph.ops.expander;

import java.util.Stack;

/** Captures the entire ApexPathExpander object */
final class ApexPathExpanderCollector extends ResultCollector {
    private final Stack<ApexPathExpander> results;

    ApexPathExpanderCollector() {
        this.results = new Stack<>();
    }

    Stack<ApexPathExpander> getResults() {
        return results;
    }

    @Override
    boolean remove(ApexPathExpander pathExpander) {
        return results.remove(pathExpander);
    }

    @Override
    void _collect(ApexPathExpander pathExpander) {
        results.push(pathExpander);
    }

    @Override
    int size() {
        return results.size();
    }
}
