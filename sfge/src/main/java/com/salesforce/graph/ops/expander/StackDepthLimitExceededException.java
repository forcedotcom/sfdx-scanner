package com.salesforce.graph.ops.expander;

import com.salesforce.graph.vertex.BaseSFVertex;

/** Thrown when stack depth grows beyond Graph Engine's limit to handle. */
public class StackDepthLimitExceededException extends ApexPathExpanderRuntimeException {
    public StackDepthLimitExceededException(int depthCount, BaseSFVertex vertex) {
        super("Stack depth limit exceeded: " + depthCount + " on vertex: " + vertex);
    }
}
