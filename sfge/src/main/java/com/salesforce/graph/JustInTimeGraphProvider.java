package com.salesforce.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Maintains per thread access to {@link JustInTimeGraph} instances. The code that initially starts
 * should invoke {@link #create(GraphTraversalSource, String)}. ALl other code should call {@link
 * #get()}
 */
public final class JustInTimeGraphProvider {
    private static final ThreadLocal<JustInTimeGraph> graphs =
            ThreadLocal.withInitial(() -> NoOpJustInTimeGraph.getInstance());

    /**
     * Initialize a JustInTimeGraph with the full graph, seeding it with {@code definingType}.
     * Classes are copied over from the fullGraph
     *
     * @param fullGraph that contains all of the source code
     * @param definingType initial type that will be imported into the graph, all other vertices are
     *     imported as needed
     * @return the graph that should be used by the rule
     */
    public static GraphTraversalSource create(GraphTraversalSource fullGraph, String definingType) {
        InMemoryJustInTimeGraph jitg = new InMemoryJustInTimeGraph(fullGraph, definingType);
        graphs.set(jitg);
        return jitg.getRuleGraph();
    }

    /**
     * Get the JustInTimeGraph for the current thread
     *
     * @return object that implements {@link JustInTimeGraph}. This will be a mock for threads which
     *     have not invoked #create
     */
    public static JustInTimeGraph get() {
        return graphs.get();
    }

    /** Remove the JustInTimeGraph from the current thread */
    public static void remove() {
        graphs.remove();
    }

    private JustInTimeGraphProvider() {}
}
