package com.salesforce.graph.ops.registry;

/**
 * Represents a type that has an id and can be added to the registry. See {@link Registry} for mroe
 * information.
 */
public interface Indexable {
    /**
     * @return unique identity of the instance. This can be either implemented using an AtomicLong
     *     ID_GENERATOR or through the hash code if the object is immutable.
     */
    Long getId();
}
