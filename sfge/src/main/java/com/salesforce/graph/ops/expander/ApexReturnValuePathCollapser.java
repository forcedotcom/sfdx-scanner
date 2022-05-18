package com.salesforce.graph.ops.expander;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Optional;

/**
 * Determines if a Path is valid based on the Method and the type that a path returned. This is used
 * to remove indeterminant paths.
 */
public interface ApexReturnValuePathCollapser {
    /**
     * Allow collapsers to filter out methods with indeterminant results
     *
     * @param vertex method vertex that is up for consideration
     * @param returnValue the value that was returned after the method was walked
     * @throws ReturnValueInvalidException if the path is not valid and should be excluded by the
     *     expander
     */
    void checkValid(MethodVertex vertex, Optional<ApexValue<?>> returnValue)
            throws ReturnValueInvalidException;
}
