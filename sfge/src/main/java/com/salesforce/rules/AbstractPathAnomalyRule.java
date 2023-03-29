package com.salesforce.rules;

import com.salesforce.graph.ops.expander.PathExpansionException;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Abstract parent class for rules that act on anomalous events that occurred during path expansion.
 */
public abstract class AbstractPathAnomalyRule extends AbstractPathBasedRule {
    public abstract List<RuleThrowable> run(
            GraphTraversalSource g,
            MethodVertex methodVertex,
            List<PathExpansionException> anomalies);
}
