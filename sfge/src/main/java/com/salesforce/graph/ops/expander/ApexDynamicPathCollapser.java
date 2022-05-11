package com.salesforce.graph.ops.expander;

import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;

/**
 * Converts a list of paths to a smaller list if possible. Implementations must be stateless,
 * instances are reused. Implementers of this interface require that the path has been walked before
 * a decision can be made.
 */
public interface ApexDynamicPathCollapser {
    /**
     * Allow the implementer to make decision based on the content of the method vertex. Returning
     * false from this method will prevent the invocation of {@link #collapse}. This method is an
     * optimization to prevent the unnecessary cloning of objects and traversal of paths.
     */
    boolean mightCollapse(MethodVertex method);

    /**
     * Convert a list of paths to a subset of those paths.
     *
     * @param method TODO
     * @param candidates list of candidates that should be considered for collapsing, the paths have
     *     already been walked The implementing method must not modify the path in any way.
     * @return the subset of candidates that the collapser considers valid
     */
    List<ApexPathCollapseCandidate> collapse(
            MethodVertex method, List<ApexPathCollapseCandidate> candidates);
}
