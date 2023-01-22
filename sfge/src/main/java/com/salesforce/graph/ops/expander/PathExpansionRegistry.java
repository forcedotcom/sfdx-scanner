package com.salesforce.graph.ops.expander;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import java.util.HashMap;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Registry that maintains Id to Instance for a few large objects involved in Apex path expansions.
 * There's a unique instance of each of the individual registries per thread. The values are
 * typically scoped within the expansion of a single ApexPath initiated at {@link
 * ApexPathExpanderUtil#expand(GraphTraversalSource, ApexPath, ApexPathExpanderConfig)}. The
 * registry requires to be reset at the end of every ApexPath expansion so that it can be reused for
 * the next path.
 */
public final class PathExpansionRegistry {

    private final PathCollapserRegistry pathCollapserRegistry;
    private final ForkEventRegistry forkEventRegistry;
    private final ApexPathExpanderRegistry apexPathExpanderRegistry;

    public PathExpansionRegistry() {
        pathCollapserRegistry = new PathCollapserRegistry();
        forkEventRegistry = new ForkEventRegistry();
        apexPathExpanderRegistry = new ApexPathExpanderRegistry();
    }

    public void clear() {
        pathCollapserRegistry.clear();
        forkEventRegistry.clear();
        apexPathExpanderRegistry.clear();
    }

    public void registerPathCollapser(Long pathExpansionId, ApexPathCollapser pathCollapser) {
        pathCollapserRegistry.validateAndPut(pathExpansionId, pathCollapser);
    }

    public ApexPathCollapser lookupPathCollapser(Long pathExpansionId) {
        return pathCollapserRegistry.get(pathExpansionId);
    }

    public ApexPathCollapser deregisterPathCollapser(Long pathExpansionId) {
        return pathCollapserRegistry.remove(pathExpansionId);
    }

    public void validatePathCollapser(ApexPathCollapser pathCollapser) {
        pathCollapserRegistry.validate(pathCollapser);
    }

    public void registerForkEvent(ForkEvent forkEvent) {
        forkEventRegistry.validateAndPut(forkEvent.getId(), forkEvent);
    }

    public ForkEvent lookupForkEvent(Long forkEventId) {
        return forkEventRegistry.get(forkEventId);
    }

    public ForkEvent deregisterForkEvent(Long forkEventId) {
        return forkEventRegistry.remove(forkEventId);
    }

    public void validateForkEvent(ForkEvent forkEvent) {
        forkEventRegistry.validate(forkEvent);
    }

    public void registerApexPathExpander(ApexPathExpander apexPathExpander) {
        apexPathExpanderRegistry.validateAndPut(apexPathExpander.getId(), apexPathExpander);
    }

    public ApexPathExpander lookupApexPathExpander(Long apexPathExpanderId) {
        return apexPathExpanderRegistry.get(apexPathExpanderId);
    }

    public ApexPathExpander deregisterApexPathExpander(Long apexPathExpanderId) {
        return apexPathExpanderRegistry.remove(apexPathExpanderId);
    }

    public void validateApexPathExpander(ApexPathExpander apexPathExpander) {
        apexPathExpanderRegistry.validate(apexPathExpander);
    }

    /**
     * Abstract class that individual types can implement to maintain an Id to Instance mapping to
     * have an outline of registry support.
     *
     * @param <T> type that requires the registry setup.
     */
    private abstract static class Registry<T> {
        private final Map<Long, T> idToInstance;

        protected Registry() {
            idToInstance = new HashMap<>();
        }

        void validateAndPut(Long id, T instance) {
            if (hasKey(id)) {
                throw new ProgrammingException("Id already exists on registry for " + instance);
            }

            if (hasValue(instance)) {
                throw new ProgrammingException("Instance already exists on registry: " + instance);
            }

            put(id, instance);
        }

        void put(Long id, T instance) {
            idToInstance.put(id, instance);
        }

        void validate(T instance) {
            if (!idToInstance.containsValue(instance)) {
                throw new ProgrammingException("Instance not found in the registry: " + instance);
            }
        }

        T get(Long id) {
            return idToInstance.get(id);
        }

        T remove(Long id) {
            return idToInstance.remove(id);
        }

        boolean hasKey(Long id) {
            return idToInstance.containsKey(id);
        }

        boolean hasValue(T instance) {
            return idToInstance.containsValue(instance);
        }

        void clear() {
            idToInstance.clear();
        }
    }

    private static class PathCollapserRegistry extends Registry<ApexPathCollapser> {
        // Nothing new to add
    }

    private static class ForkEventRegistry extends Registry<ForkEvent> {
        // Nothing new to add
    }

    private static class ApexPathExpanderRegistry extends Registry<ApexPathExpander> {
        // Nothing new to add
    }
}
