package com.salesforce.graph.visitor;

import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.rules.AbstractPathTraversalRule;
import com.salesforce.rules.PathTraversalRule;

/** Use this class to avoid "instanceof" pattern */
public class VertexPredicateVisitor {
    public void defaultVisit(VertexPredicate predicate) {
        // Intentionally left blank
    }

    public void visit(VertexPredicate predicate) {
        defaultVisit(predicate);
    }

    public void visit(PathTraversalRule rule) {
        defaultVisit(rule);
    }

    public void visit(AbstractPathTraversalRule rule) {
        defaultVisit(rule);
    }
}
