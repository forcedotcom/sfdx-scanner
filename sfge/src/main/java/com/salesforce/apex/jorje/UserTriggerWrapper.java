package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.compilation.UserTrigger;
import com.salesforce.graph.Schema;
import java.util.Map;

public final class UserTriggerWrapper extends AstNodeWrapper<UserTrigger>
        implements TopLevelWrapper {
    UserTriggerWrapper(UserTrigger node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.NAME, getName());
    }
}
