package com.salesforce.rules;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Implemented by rules that execute during path traversal. These rules typically have an interest
 * in a specific vertex. Whether or not that vertex violates the rule is dependent on the path that
 * leads up to the vertex.
 *
 * <p>The caller finds all paths, and invokes {@link #test(BaseSFVertex)} on the rule. The rule
 * indicates its interest in teh vertex by returning true. TODO: May need to extend this to provide
 * a way for the rule to provide the query for paths. We don't want each rule to search for paths
 * individually as this would be inefficient. For now, the paths are determined by the caller.
 */
public interface PathTraversalRule extends VertexPredicate {
    /**
     * Run the rule against {@code path}. The rule should only return errors related to {@code
     * vertex}. This method is only called when {@link VertexPredicate#test(BaseSFVertex)} returns
     * true. This method is called once for each path that contains the vertex.
     */
    List<RuleThrowable> run(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex);
}
