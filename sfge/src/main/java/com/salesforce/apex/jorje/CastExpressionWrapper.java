package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.CastExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class CastExpressionWrapper extends AstNodeWrapper<CastExpression> {
    CastExpressionWrapper(CastExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.TYPE_REF, typeRefToString(getNode().getTypeRef()));
    }
}
