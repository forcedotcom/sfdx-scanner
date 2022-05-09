package com.salesforce.graph.vertex;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a vertex that may be chained to other vertices. An example would be {@code
 * Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();
 * m.get('Name').getDescribe().isCreateable(); }
 */
public abstract class ChainedVertex extends BaseSFVertex {
    protected ChainedVertex(Map<Object, Object> properties) {
        this(properties, null);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter") // TODO: supplementalParam is ignored
    protected ChainedVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties);
    }

    public Optional<String> getSymbolicName() {
        return Optional.empty();
    }

    public boolean isResolvable() {
        return true;
    }

    public List<String> getChainedNames() {
        return Collections.emptyList();
    }
}
