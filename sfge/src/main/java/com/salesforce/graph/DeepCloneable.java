package com.salesforce.graph;

/**
 * Classes that implement this interface should be marked as final in order to avoid subtle bugs.
 */
public interface DeepCloneable<T> {
    /** Clone the current object, any mutable objects should be copied */
    T deepClone();
}
