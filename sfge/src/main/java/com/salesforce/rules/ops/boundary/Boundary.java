package com.salesforce.rules.ops.boundary;

/**
 * Defines a boundary of influence within code. Use with {@link BoundaryDetector} to mark the
 * beginning and end of a boundary.
 */
public interface Boundary<T> {
    /**
     * @return the object that governs this boundary.
     */
    T getBoundaryItem();
}
