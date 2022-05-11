package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.BinaryExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class BinaryExpressionWrapper extends AstNodeWrapper<BinaryExpression> {
    BinaryExpressionWrapper(BinaryExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.OPERATOR, getNode().getOp().toString());
    }
}
