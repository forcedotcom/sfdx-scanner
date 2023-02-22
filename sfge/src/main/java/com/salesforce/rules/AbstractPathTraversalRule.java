package com.salesforce.rules;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.visitor.VertexPredicateVisitor;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Abstract parent class for rules that execute during the traversal of already-expanded paths. */
public abstract class AbstractPathTraversalRule extends AbstractPathBasedRule
        implements PathTraversalRule {
    @Override
    public final List<RuleThrowable> run(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        return _run(g, path, vertex);
    }

    protected abstract List<RuleThrowable> _run(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex);

    @Override
    public void accept(VertexPredicateVisitor visitor) {
        visitor.visit(this);
    }
}
