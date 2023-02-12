package com.salesforce.graph.ops.expander;

import com.salesforce.graph.vertex.BaseSFVertex;

public class StackDepthLimitExceededException extends ApexPathExpanderRuntimeException {
    public StackDepthLimitExceededException(int depthCount, BaseSFVertex vertex) {
        super("Stack depth limit exceeded: " + depthCount + " on vertex: " + vertex);
    }
}
