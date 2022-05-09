package com.salesforce.graph;

/** Mock implementation for cases where the JustInTime graph has not been configured. */
public final class NoOpJustInTimeGraph implements JustInTimeGraph {
    @Override
    public void loadUserClass(String definingType) {
        // Intentionally left blank
    }

    public static NoOpJustInTimeGraph getInstance() {
        return NoOpJustInTimeGraph.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final NoOpJustInTimeGraph INSTANCE = new NoOpJustInTimeGraph();
    }

    private NoOpJustInTimeGraph() {}
}
