package com.salesforce.rules;

import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Implemented by rules that either operate on the graph independent of a path. Or generated their
 * own paths to traverse.
 */
public interface StaticRule {
    /** Return a list of violations sorted by line number, then message. */
    List<Violation> run(GraphTraversalSource g);
}
