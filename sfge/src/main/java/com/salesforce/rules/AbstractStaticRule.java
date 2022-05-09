package com.salesforce.rules;

import com.salesforce.graph.ops.TraversalUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public abstract class AbstractStaticRule extends AbstractRule implements StaticRule {
    @Override
    public final List<Violation> run(GraphTraversalSource g) {
        return run(g, new ArrayList<>());
    }

    public final List<Violation> run(
            GraphTraversalSource g, List<AbstractRuleRunner.RuleRunnerTarget> targets) {
        GraphTraversal<Vertex, Vertex> eligibleVertices = buildBaseTraversal(g, targets);
        return _run(g, eligibleVertices);
    }

    private GraphTraversal<Vertex, Vertex> buildBaseTraversal(
            GraphTraversalSource g, List<AbstractRuleRunner.RuleRunnerTarget> targets) {
        return TraversalUtil.ruleTargetTraversal(g, targets);
    }

    /**
     * @param g
     * @param eligibleVertices - A traversal containing the vertices that are eligible for analysis
     *     by this rule. This traversal should be used as the base for any traversals done during
     *     the rule's evaluation.
     * @return
     */
    protected abstract List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices);
}
