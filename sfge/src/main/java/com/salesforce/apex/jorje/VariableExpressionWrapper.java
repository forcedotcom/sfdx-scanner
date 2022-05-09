package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.VariableExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class VariableExpressionWrapper extends AstNodeWrapper<VariableExpression> {
    VariableExpressionWrapper(VariableExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.NAME, getNode().getIdentifier().getValue());
        getParent()
                .ifPresent(
                        p ->
                                p.accept(
                                        new NewKeyValueObjectExpressionWrapper.KeyNameVisitor(
                                                this, properties)));
    }
}
