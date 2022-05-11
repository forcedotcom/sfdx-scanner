package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.vertex.InvocableVertex;
import java.util.Objects;

/**
 * Represents a {@link ApexPath}/{@link InvocableVertex} pair that uniquely identifies the
 * relationship. A given TODO may be visited more than once, but it will always correspond to a
 * unique ApexPath.
 */
final class PathInvocableCall implements DeepCloneable<PathInvocableCall> {
    private final Long pathId;
    private final InvocableVertex vertex;
    private final int hash;

    PathInvocableCall(ApexPath path, InvocableVertex vertex) {
        this.pathId = path.getStableId();
        this.vertex = vertex;
        this.hash = Objects.hash(this.pathId, this.vertex);
    }

    @Override
    public PathInvocableCall deepClone() {
        // It's immutable, reuse it.
        return this;
    }

    Long getPathId() {
        return pathId;
    }

    InvocableVertex getVertex() {
        return vertex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathInvocableCall that = (PathInvocableCall) o;
        return pathId.equals(that.pathId) && vertex.equals(that.vertex);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
