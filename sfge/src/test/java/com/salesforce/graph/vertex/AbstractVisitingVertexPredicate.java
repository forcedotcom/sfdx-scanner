package com.salesforce.graph.vertex;

import com.salesforce.graph.visitor.VertexPredicateVisitor;

/**
 * Useful when you want to implement {@link #test}, but don't want any special behavior for {@link
 * #accept(VertexPredicateVisitor)}
 */
public abstract class AbstractVisitingVertexPredicate implements VertexPredicate {
    @Override
    public final void accept(VertexPredicateVisitor visitor) {
        visitor.visit(this);
    }
}
