package com.salesforce.graph.visitor;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.Objects;

/**
 * Represents a {@link ApexPath}/{@link BaseSFVertex} pair that uniquely identifies the
 * relationship. A given vertex may be visited more than once, but it will always correspond to a
 * unique ApexPath.
 */
public final class PathVertex implements DeepCloneable<PathVertex> {
    private final Long stableId;
    private final BaseSFVertex vertex;
    private final int hash;

    public PathVertex(ApexPath path, BaseSFVertex vertex) {
        this.stableId = path.getStableId();
        this.vertex = vertex;
        this.hash = Objects.hash(this.stableId, this.vertex);
    }

    public BaseSFVertex getVertex() {
        return vertex;
    }

    @Override
    public PathVertex deepClone() {
        // It's immutable reuse
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathVertex that = (PathVertex) o;
        return stableId.equals(that.stableId) && vertex.equals(that.vertex);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
