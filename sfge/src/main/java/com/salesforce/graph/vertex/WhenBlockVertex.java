package com.salesforce.graph.vertex;

import java.util.Map;

/** Represents one of the blocks executed when a case statement evaluates to true */
public abstract class WhenBlockVertex extends TODO_FIX_HIERARCHY_ChainedVertex {
    WhenBlockVertex(Map<Object, Object> properties) {
        super(properties);
    }

    @Override
    public boolean startsInnerScope() {
        return true;
    }

    public SwitchStatementVertex getSwitchStatementVertex() {
        return getParent();
    }

    public ChainedVertex getExpressionVertex() {
        return getSwitchStatementVertex().getSwitchExpressionVertex();
    }
}
