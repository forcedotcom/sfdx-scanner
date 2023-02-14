package com.salesforce.graph.ops.registry;

import com.salesforce.exception.ProgrammingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents data that can be stored in {@link Registry}. Any {@link Registrable} type can be
 * maintained here.
 *
 * <p>Internally, {@link RegistryData} holds a mapping of {@link Long} Id to instance of {@link
 * Registrable} instance.
 *
 * <p>It provides methods to register an instance, lookup using its Id, and deregister when the
 * instance is not needed anymore.
 *
 * @param <T> {@link Registrable} type that requires the registry setup.
 */
public abstract class RegistryData<T extends Registrable> {
    protected final Map<Long, Registrable> idToInstance;

    public RegistryData() {
        idToInstance = new HashMap<>();
    }

    /**
     * Validates that the instance is already not in the registry before adding it.
     *
     * @param instance to be added to registry.
     */
    public void validateAndPut(T instance) {
        if (hasKey(instance.getId())) {
            throw new ProgrammingException("Id already exists on registry for " + instance);
        }

        if (hasValue(instance)) {
            throw new ProgrammingException("Instance already exists on registry: " + instance);
        }

        put(instance);
    }

    private void put(T instance) {
        idToInstance.put(instance.getId(), instance);
    }

    /**
     * Verifies that the instance is already in the registry.
     *
     * @param instance to verify
     */
    public void verifyExists(T instance) {
        if (!idToInstance.containsValue(instance)) {
            throw new ProgrammingException("Instance not found in the registry: " + instance);
        }
    }

    /** Get {@link Registrable} instance given its Id. */
    public Registrable get(Long id) {
        return idToInstance.get(id);
    }

    /** Removes {@link Registrable} instance from registry, given the Id. */
    public Registrable remove(Long id) {
        return idToInstance.remove(id);
    }

    public int size() {
        return idToInstance.size();
    }

    private boolean hasKey(Long id) {
        return idToInstance.containsKey(id);
    }

    private boolean hasValue(T instance) {
        return idToInstance.containsValue(instance);
    }

    /** Clears all the data. */
    public void clear() {
        idToInstance.clear();
    }
}
