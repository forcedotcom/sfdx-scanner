package com.salesforce.graph.ops.expander;

import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.PathVertex;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a fork that occurred while an {@link ApexPathExpander} was walking a path. This event
 * is stored in the newly forked ApexPathExpanders and ties all future forks together.
 */
class ForkEvent implements DeepCloneable<ForkEvent> {
    /** Used to give each object a unique id */
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    /** Dynamically generated id used to establish object equality */
    private final Long id;

    /** Id of the expander tha was executing when the fork occurred */
    private final Long apexPathExpanderId;

    /**
     * The invocation that caused the fork. The contained vertex is typically some form of {@link
     * com.salesforce.graph.vertex.InvocableVertex}
     */
    private final PathVertex pathVertex;

    /** The method which contain multiple paths, causing the fork */
    private final MethodVertex methodVertex;

    private final int hash;

    ForkEvent(
            Long apexPathExpanderId,
            PathVertex pathVertex,
            MethodVertex methodVertex,
            PathExpansionRegistry registry) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.apexPathExpanderId = apexPathExpanderId;
        this.pathVertex = pathVertex;
        this.methodVertex = methodVertex;
        this.hash = Objects.hash(this.apexPathExpanderId, this.pathVertex, this.methodVertex);

        // Register the newly created ForkEvent
        registry.registerForkEvent(this);
    }

    @Override
    public ForkEvent deepClone() {
        // It's immutable reuse
        return this;
    }

    Long getId() {
        return id;
    }

    public PathVertex getPathVertex() {
        return pathVertex;
    }

    public MethodVertex getMethodVertex() {
        return methodVertex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForkEvent forkEvent = (ForkEvent) o;
        return Objects.equals(apexPathExpanderId, forkEvent.apexPathExpanderId)
                && Objects.equals(pathVertex, forkEvent.pathVertex)
                && Objects.equals(methodVertex, forkEvent.methodVertex);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
