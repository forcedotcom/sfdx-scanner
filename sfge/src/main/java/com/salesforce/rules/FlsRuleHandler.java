package com.salesforce.rules;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Interface that defines operations that an FLS handler needs to implement so that they can be
 * invoked through {@link ApexFlsViolationRule}
 */
public interface FlsRuleHandler {

    /**
     * Walks the ApexPath with the {@link com.salesforce.rules.fls.apex.AbstractFlsVisitor} that
     * corresponds to the given BaseSFVertex to detect FLS violations.
     *
     * @return
     */
    Set<FlsViolationInfo> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex);

    /**
     * Returns true if the vertex can be handled by this implementation of {@link FlsRuleHandler}
     */
    boolean test(BaseSFVertex vertex);
}
