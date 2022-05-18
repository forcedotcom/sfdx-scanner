package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.SoqlExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class SoqlExpressionWrapper extends AstNodeWrapper<SoqlExpression> {
    SoqlExpressionWrapper(SoqlExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.QUERY, getNode().getQuery());
    }
}
