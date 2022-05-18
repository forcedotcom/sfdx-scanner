package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.compilation.UserEnum;
import com.salesforce.graph.Schema;
import java.util.Map;

public final class UserEnumWrapper extends AstNodeWrapper<UserEnum> implements TopLevelWrapper {
    UserEnumWrapper(UserEnum node, AstNodeWrapper<?> parent) {
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
