package com.salesforce.graph.ops.registry;

/**
 * Represents a type that has an id and can be added to the registry. See {@link Registry} for more
 * information. To add to the registry, the implementer needs to have a reference to their registry
 * instance and invoke the register() method probably from the constructor.
 */
public interface Registrable {
    /**
     * @return unique identity of the instance. This can be either implemented using an {@link
     *     java.util.concurrent.atomic.AtomicLong} ID_GENERATOR or through the hash code if the
     *     object is immutable.
     */
    Long getId();
}
