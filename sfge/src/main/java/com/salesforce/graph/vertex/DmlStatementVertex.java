package com.salesforce.graph.vertex;

import java.util.Map;

public abstract class DmlStatementVertex extends BaseSFVertex {
    DmlStatementVertex(Map<Object, Object> properties) {
        super(properties);
    }
}
