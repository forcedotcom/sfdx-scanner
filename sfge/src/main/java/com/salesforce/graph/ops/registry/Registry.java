package com.salesforce.graph.ops.registry;

import com.salesforce.graph.ops.expander.PathExpansionRegistry;
import com.salesforce.graph.ops.expander.PathExpansionRegistryUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry framework can be used as an optimization mechanism to store smaller data in collections.
 * If you find that an object is particularly large and requires many instances to be created and
 * stored in collections: 1. Make the object implement {@link Indexable} and provide an Id form. 2.
 * Extend {@link AbstractRegistryData} for this object type. 3. Extend {@link Registry} and provide
 * a Supplier with a mapping of the object type to a method to create its {@link
 * AbstractRegistryData} implementation. 4. Modify usages of the object type to store its {@link
 * Long} Id in the required collections and look up from Registry wherever the object's instance
 * value is required.
 *
 * <p>See {@link PathExpansionRegistry} and {@link PathExpansionRegistryUtil} for examples on how
 * this framework can be used.
 */
public abstract class Registry {

    protected final Map<Class, AbstractRegistryData> registryHolderMap;

    public Registry() {
        registryHolderMap = new HashMap<>();
        for (Map.Entry<Class<? extends Indexable>, Supplier> entry :
                this.getRegistrySupplier().entrySet()) {
            registryHolderMap.put(entry.getKey(), (AbstractRegistryData) entry.getValue().get());
        }
    }

    /**
     * @return a Map of Class that implements {@link Indexable} to a Supplier to get an instance of
     *     its corresponding implementation of {@link AbstractRegistryData}.
     */
    protected abstract Map<Class<? extends Indexable>, Supplier> getRegistrySupplier();

    /** Clear all the data in registry. */
    public void clear() {
        for (AbstractRegistryData registry : registryHolderMap.values()) {
            registry.clear();
        }

        registryHolderMap.clear();
    }

    /**
     * Register an Indexable instance.
     *
     * @param indexableClass Class that the Indexable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param indexable instance to register.
     */
    public void register(Class<? extends Indexable> indexableClass, Indexable indexable) {
        registryHolderMap.get(indexableClass).validateAndPut(indexable);
    }

    /**
     * Lookup an instance in the registry given its Id.
     *
     * @param indexableClass Class that the Indexable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param id Long Id that the lookedup instance's {@link Indexable#getId()} would return.
     * @return instance of Indexable that was previously registered.
     */
    public Indexable lookup(Class<? extends Indexable> indexableClass, Long id) {
        return registryHolderMap.get(indexableClass).get(id);
    }

    /**
     * Removed registered instance from Registry.
     *
     * @param indexableClass Class that the Indexable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param id Long Id that the lookedup instance's {@link Indexable#getId()} would return.
     * @return instance of Indexable that was previously registered and now required to be
     *     deregistered.
     */
    public Indexable deregister(Class<? extends Indexable> indexableClass, Long id) {
        return registryHolderMap.get(indexableClass).remove(id);
    }

    /**
     * Verify that a given instance is in the Registry.
     *
     * @param indexableClass Class that the Indexable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param indexableInstance instance of Indexable that we expect to have already been
     *     registered.
     */
    public void verifyExists(
            Class<? extends Indexable> indexableClass, Indexable indexableInstance) {
        registryHolderMap.get(indexableClass).verifyExists(indexableInstance);
    }
}
