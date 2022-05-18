package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.LiteralExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class LiteralExpressionWrapper extends AstNodeWrapper<LiteralExpression> {
    LiteralExpressionWrapper(LiteralExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        if (getNode().getLiteral() != null) {
            properties.put(Schema.VALUE, getNode().getLiteral().toString());
        }
        properties.put(Schema.LITERAL_TYPE, getNode().getLiteralType());
        getParent()
                .ifPresent(
                        p ->
                                p.accept(
                                        new NewKeyValueObjectExpressionWrapper.KeyNameVisitor(
                                                this, properties)));
    }
}
