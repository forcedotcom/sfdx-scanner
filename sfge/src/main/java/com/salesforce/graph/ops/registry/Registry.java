package com.salesforce.graph.ops.registry;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.expander.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Registry that maintains Id to Instance for a few large objects involved in Apex path expansions.
 * There's a unique instance of each of the individual registries per thread. The values are
 * typically scoped within the expansion of a single ApexPath initiated at {@link
 * ApexPathExpanderUtil#expand(GraphTraversalSource, ApexPath, ApexPathExpanderConfig)}. The
 * registry requires to be reset at the end of every ApexPath expansion so that it can be reused for
 * the next path.
 */
public abstract class Registry {

    protected final Map<Class, AbstractRegistryData> registryHolderMap;

    public Registry() {
        registryHolderMap = new HashMap<>();
        for (Map.Entry<Class, Supplier> entry : this.getRegistrySupplier().entrySet()) {
            registryHolderMap.put(entry.getKey(), (AbstractRegistryData) entry.getValue().get());
        }
    }

    protected abstract Map<Class, Supplier> getRegistrySupplier();

    public void clear() {
        for (AbstractRegistryData registry : registryHolderMap.values()) {
            registry.clear();
        }

        registryHolderMap.clear();
    }

    public void register(Class<? extends Indexable> indexableClass, Indexable indexable) {
        registryHolderMap.get(indexableClass).validateAndPut(indexable);
    }

    public Indexable lookup(Class<? extends Indexable> indexableClass, Long id) {
        return registryHolderMap.get(indexableClass).get(id);
    }

    public Indexable deregister(Class<? extends Indexable> indexableClass, Long id) {
        return registryHolderMap.get(indexableClass).remove(id);
    }

    public void validate(Class<? extends Indexable> indexableClass, Indexable indexableInstance) {
        registryHolderMap.get(indexableClass).validate(indexableInstance);
    }
}
