package com.salesforce.graph.vertex;

import com.salesforce.graph.visitor.VertexPredicateVisitor;

public interface VertexPredicate {
    /** Return true if the implementer is interested in this vertex. */
    boolean test(BaseSFVertex vertex);

    /** Dispatch to a {@link VertexPredicateVisitor} */
    void accept(VertexPredicateVisitor visitor);
}
