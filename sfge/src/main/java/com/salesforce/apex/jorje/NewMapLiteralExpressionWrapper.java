package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.NewMapLiteralExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

public class NewMapLiteralExpressionWrapper extends AstNodeWrapper<NewMapLiteralExpression> {
    NewMapLiteralExpressionWrapper(NewMapLiteralExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.TYPE, normalizeType(getNode().getTypeRef().toString()));
    }
}
