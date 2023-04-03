package com.salesforce.rules;

import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Interface that can be optionally implemented by subclasses of {@link AbstractPathBasedRule}. The
 * {@link #postProcess} hook will be invoked during rule execution after all path entry-points have
 * been fully evaluated. This allows the rule to gather information across all its path executions
 * and then process that information at the very end. <br>
 * An example of this interface in action is {@link UnusedMethodRule}, whose {@code run()} method
 * notes every unique method invoked in any path, and whose {@link #postProcess} method creates
 * violations for methods that were never invoked.
 */
public interface PostProcessingRule {

    /**
     * This method will be invoked after every entrypoint has been fully evaluated, allowing for
     * violations to be created based on the run as a whole, rather than against individual paths.
     */
    List<Violation> postProcess(GraphTraversalSource g);
}
