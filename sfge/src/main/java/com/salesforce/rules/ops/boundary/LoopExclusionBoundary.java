package com.salesforce.rules.ops.boundary;

import com.salesforce.graph.vertex.SFVertex;

/**
 * Indicates parts of the loop that get run only once. For example:
 *
 * <ul>
 *   <li>getValues() method in: <code>for (String s: getValues())</code>
 *   <li>Static initialization of a class
 * </ul>
 */
public class LoopExclusionBoundary extends LoopBoundary {
    public LoopExclusionBoundary(SFVertex loopVertex) {
        super(loopVertex);
    }
}
