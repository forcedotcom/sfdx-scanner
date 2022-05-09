package com.salesforce.rules;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.visitor.VertexPredicateVisitor;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public abstract class AbstractPathBasedRule extends AbstractRule implements PathBasedRule {
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
