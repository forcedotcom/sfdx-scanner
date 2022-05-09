package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.PathVertex;
import java.util.List;

/** Exception thrown when a path needs to be forked because a method contains branches */
final class MethodPathForkedException extends ApexPathExpanderException {
    /** The expander that was executing at the time of the fork */
    private final ApexPathExpander apexPathExpander;

    /** The individual paths that correspond to the invoked method */
    private final List<ApexPath> paths;

    /**
     * The path that contains the fork. This can be different than the topLevelPath of the
     * ApexPathExpander.
     */
    private final ApexPath pathWithFork;

    /**
     * The vertex that contains the invocable call which caused the fork. This is vertex that is
     * present in the top level list of vertices in the path.
     */
    private final BaseSFVertex topLevelVertex;

    /** The vertex that invokes the method with multiple paths */
    private final InvocableVertex invocable;

    /** The event that caused the fork */
    private final ForkEvent forkEvent;

    MethodPathForkedException(
            ApexPathExpander apexPathExpander,
            ApexPath pathWithFork,
            BaseSFVertex topLevelVertex,
            InvocableVertex invocable,
            List<ApexPath> paths) {
        this.apexPathExpander = apexPathExpander;
        this.paths = paths;
        this.pathWithFork = pathWithFork;
        this.topLevelVertex = topLevelVertex;
        this.invocable = invocable;
        PathVertex pathVertex = new PathVertex(pathWithFork, (BaseSFVertex) invocable);
        MethodVertex methodVertex = paths.get(0).getMethodVertex().get();
        this.forkEvent = new ForkEvent(apexPathExpander.getId(), pathVertex, methodVertex);
    }

    ApexPathExpander getApexPathExpander() {
        return apexPathExpander;
    }

    List<ApexPath> getPaths() {
        return paths;
    }

    ApexPath getPathWithFork() {
        return pathWithFork;
    }

    BaseSFVertex getTopLevelVertex() {
        return topLevelVertex;
    }

    InvocableVertex getInvocable() {
        return invocable;
    }

    ForkEvent getForkEvent() {
        return forkEvent;
    }

    @Override
    public String toString() {
        return "MethodPathForkedException{"
                + "size="
                + getPaths().size()
                + ", pathWithFork="
                + pathWithFork
                + ", topLevelVertex="
                + topLevelVertex
                + ", invocable="
                + invocable
                + ", forkEvent="
                + forkEvent
                + "} "
                + super.toString();
    }
}
