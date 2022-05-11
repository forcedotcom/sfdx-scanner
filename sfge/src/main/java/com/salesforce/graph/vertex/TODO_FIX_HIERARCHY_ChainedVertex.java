package com.salesforce.graph.vertex;

import java.util.Map;

/**
 * Vertex for things that need to be a ChainedVertex, but don't have a name. TODO: Revisit hierarchy
 */
public abstract class TODO_FIX_HIERARCHY_ChainedVertex extends ChainedVertex {
    TODO_FIX_HIERARCHY_ChainedVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    TODO_FIX_HIERARCHY_ChainedVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, supplementalParam);
    }
}
