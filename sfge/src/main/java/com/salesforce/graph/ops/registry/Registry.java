package com.salesforce.graph.ops.registry;

import com.salesforce.graph.ops.expander.PathExpansionRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry framework can be used as an optimization mechanism to store smaller data in collections.
 * If you find that an object is particularly large and requires many instances to be created and
 * stored in collections, follow these steps:
 *
 * <ul>
 *   <li>1. Make the object implement {@link Registrable} and provide an Id form.
 *   <li>2. Extend {@link RegistryData} for this object type.
 *   <li>3. Extend {@link Registry} and provide a Supplier with a mapping of the object type to a
 *       method to create its {@link RegistryData} implementation.
 *   <li>4. Modify usages of the object type to store its {@link Long} Id in the required
 *       collections and look up from Registry wherever the object's instance value is required.
 * </ul>
 *
 * <p>See {@link PathExpansionRegistry} for examples on how this framework can be used.
 */
public abstract class Registry {

    protected final Map<Class, RegistryData> registryHolderMap;

    public Registry() {
        registryHolderMap = new HashMap<>();
        for (Map.Entry<Class<? extends Registrable>, Supplier> entry :
                this.getRegistrySupplier().entrySet()) {
            registryHolderMap.put(entry.getKey(), (RegistryData) entry.getValue().get());
        }
    }

    /**
     * @return a Map of Class that implements {@link Registrable} to a Supplier to get an instance of
     *     its corresponding implementation of {@link RegistryData}.
     */
    protected abstract Map<Class<? extends Registrable>, Supplier> getRegistrySupplier();

    /** Clear all the data in registry. */
    public void clear() {
        for (RegistryData registry : registryHolderMap.values()) {
            registry.clear();
        }

        registryHolderMap.clear();
    }

    /**
     * Register an instance.
     *
     * @param registrableClass Class that the Registrable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param registrable instance to register.
     */
    public void register(Class<? extends Registrable> registrableClass, Registrable registrable) {
        registryHolderMap.get(registrableClass).validateAndPut(registrable);
    }

    /**
     * Lookup an instance in the registry given its Id.
     *
     * @param registrableClass Class that the Registrable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param id Long Id that the lookedup instance's {@link Registrable#getId()} would return.
     * @return instance of Registrable that was previously registered.
     */
    public Registrable lookup(Class<? extends Registrable> registrableClass, Long id) {
        return registryHolderMap.get(registrableClass).get(id);
    }

    /**
     * Removed registered instance from Registry.
     *
     * @param registrableClass Class that the Registrable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param id Long Id that the lookedup instance's {@link Registrable#getId()} would return.
     * @return instance of Registrable that was previously registered and now required to be
     *     deregistered.
     */
    public Registrable deregister(Class<? extends Registrable> registrableClass, Long id) {
        return registryHolderMap.get(registrableClass).remove(id);
    }

    /**
     * Verify that a given instance is in the Registry.
     *
     * @param registrableClass Class that the Registrable is registered with in {@link
     *     #getRegistrySupplier()}.
     * @param registrableInstance instance of Registrable that we expect to have already been
     *     registered.
     */
    public void verifyExists(
        Class<? extends Registrable> registrableClass, Registrable registrableInstance) {
        registryHolderMap.get(registrableClass).verifyExists(registrableInstance);
    }
}
