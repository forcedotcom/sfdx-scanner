package com.salesforce.rules.ops.boundary;

import com.salesforce.graph.vertex.SFVertex;

/**
 * Indicates parts of the loop that get run only once. However, a nested loop scenario can still
 * execute this portion multiple times. For example:
 *
 * <ul>
 *   <li>getValues() method in: <code>for (String s: getValues())</code>
 * </ul>
 */
public class OverridableLoopExclusionBoundary extends LoopBoundary {
    public OverridableLoopExclusionBoundary(SFVertex loopVertex) {
        super(loopVertex);
    }
}
