package com.salesforce.rules.ops.boundary;

import com.salesforce.graph.vertex.SFVertex;

/**
 * Indicates portions of code that will be executed only once no matter how many loops this is
 * nested under.
 *
 * <p>For example: Static initialization of a class
 */
public class PermanentLoopExclusionBoundary extends LoopBoundary {
    public PermanentLoopExclusionBoundary(SFVertex loopVertex) {
        super(loopVertex);
    }
}
