package com.salesforce.graph.ops.expander;

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
    private PathExpansionRegistry() {}

    private static final ThreadLocal<PathCollapserRegistry> PATH_COLLAPSER_REGISTRY_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new PathCollapserRegistry());

    private static final ThreadLocal<ForkEventRegistry> FORK_EVENT_REGISTRY_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new ForkEventRegistry());

    private static final ThreadLocal<ApexPathExpanderRegistry>
            APEX_PATH_EXPANDER_REGISTRY_THREAD_LOCAL =
                    ThreadLocal.withInitial(() -> new ApexPathExpanderRegistry());

    public static void clear() {
        PATH_COLLAPSER_REGISTRY_THREAD_LOCAL.get().clear();
        FORK_EVENT_REGISTRY_THREAD_LOCAL.get().clear();
        APEX_PATH_EXPANDER_REGISTRY_THREAD_LOCAL.get().clear();
    }

    public static void registerPathCollapser(
            Long pathExpansionId, ApexPathCollapser pathCollapser) {
        PATH_COLLAPSER_REGISTRY_THREAD_LOCAL.get().put(pathExpansionId, pathCollapser);
    }

    public static ApexPathCollapser lookupPathCollapser(Long pathExpansionId) {
        return PATH_COLLAPSER_REGISTRY_THREAD_LOCAL.get().get(pathExpansionId);
    }

    public static ApexPathCollapser deregisterPathCollapser(Long pathExpansionId) {
        return PATH_COLLAPSER_REGISTRY_THREAD_LOCAL.get().remove(pathExpansionId);
    }

    public static void registerForkEvent(ForkEvent forkEvent) {
        FORK_EVENT_REGISTRY_THREAD_LOCAL.get().put(forkEvent.getId(), forkEvent);
    }

    public static ForkEvent lookupForkEvent(Long forkEventId) {
        return FORK_EVENT_REGISTRY_THREAD_LOCAL.get().get(forkEventId);
    }

    public static ForkEvent deregisterForkEvent(Long forkEventId) {
        return FORK_EVENT_REGISTRY_THREAD_LOCAL.get().remove(forkEventId);
    }

    public static void registerApexPathExpander(ApexPathExpander apexPathExpander) {
        APEX_PATH_EXPANDER_REGISTRY_THREAD_LOCAL
                .get()
                .put(apexPathExpander.getId(), apexPathExpander);
    }

    public static ApexPathExpander lookupApexPathExpander(Long apexPathExpanderId) {
        return APEX_PATH_EXPANDER_REGISTRY_THREAD_LOCAL.get().get(apexPathExpanderId);
    }

    public static ApexPathExpander deregisterApexPathExpander(Long apexPathExpanderId) {
        return APEX_PATH_EXPANDER_REGISTRY_THREAD_LOCAL.get().remove(apexPathExpanderId);
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

        void put(Long id, T instance) {
            idToInstance.put(id, instance);
        }

        T get(Long id) {
            return idToInstance.get(id);
        }

        T remove(Long id) {
            return idToInstance.remove(id);
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
