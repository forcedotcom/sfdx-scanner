package com.salesforce.graph.ops.registry;

import com.salesforce.exception.ProgrammingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class that individual types can implement to maintain an Id to Instance mapping to have
 * an outline of registry support.
 *
 * @param <T> type that requires the registry setup.
 */
public abstract class AbstractRegistryData<T extends Indexable> {
    private final Map<Long, Indexable> idToInstance;

    protected AbstractRegistryData() {
        idToInstance = new HashMap<>();
    }

    public void validateAndPut(T instance) {
        if (hasKey(instance.getId())) {
            throw new ProgrammingException("Id already exists on registry for " + instance);
        }

        if (hasValue(instance)) {
            throw new ProgrammingException("Instance already exists on registry: " + instance);
        }

        put(instance);
    }

    public void put(T instance) {
        idToInstance.put(instance.getId(), instance);
    }

    public void validate(T instance) {
        if (!idToInstance.containsValue(instance)) {
            throw new ProgrammingException("Instance not found in the registry: " + instance);
        }
    }

    public Indexable get(Long id) {
        return idToInstance.get(id);
    }

    public Indexable remove(Long id) {
        return idToInstance.remove(id);
    }

    public boolean hasKey(Long id) {
        return idToInstance.containsKey(id);
    }

    public boolean hasValue(T instance) {
        return idToInstance.containsValue(instance);
    }

    public void clear() {
        idToInstance.clear();
    }
}
