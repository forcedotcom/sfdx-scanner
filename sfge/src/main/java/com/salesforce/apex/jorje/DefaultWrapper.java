package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.AstNode;
import java.util.Map;
import javax.annotation.Nullable;

/** Wrapper used when a node does not need any additional properties. */
final class DefaultWrapper extends AstNodeWrapper<AstNode> {
    DefaultWrapper(AstNode node, @Nullable AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        // Intentionally left blank
    }
}
