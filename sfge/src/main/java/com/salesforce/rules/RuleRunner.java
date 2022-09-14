package com.salesforce.rules;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Default AbstractRuleRunner implementation that loads .cls files from disk. */
public class RuleRunner extends AbstractRuleRunner {
    public RuleRunner(GraphTraversalSource g) {
        super(g);
    }
}
