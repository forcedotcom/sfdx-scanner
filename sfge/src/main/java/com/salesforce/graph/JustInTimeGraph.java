package com.salesforce.graph;

/**
 * Implemented by classes that provide an optimized graph which only contains a subset of the full
 * graph that was imported when the process originally started.
 */
public interface JustInTimeGraph {
    /** Load the Apex class identified by {@code definingType} into the just in time graph. */
    void loadUserClass(String definingType);
}
