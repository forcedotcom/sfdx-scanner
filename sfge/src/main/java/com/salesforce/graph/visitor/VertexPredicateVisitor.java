package com.salesforce.graph.visitor;

import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.PathBasedRule;

/** Use this class to avoid "instanceof" pattern */
public class VertexPredicateVisitor {
    public void defaultVisit(VertexPredicate predicate) {
        // Intentionally left blank
    }

    public void visit(VertexPredicate predicate) {
        defaultVisit(predicate);
    }

    public void visit(PathBasedRule rule) {
        defaultVisit(rule);
    }

    public void visit(AbstractPathBasedRule rule) {
        defaultVisit(rule);
    }
}
