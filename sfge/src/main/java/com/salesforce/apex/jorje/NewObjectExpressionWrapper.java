package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.NewObjectExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class NewObjectExpressionWrapper extends AstNodeWrapper<NewObjectExpression> {
    NewObjectExpressionWrapper(NewObjectExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.TYPE, typeRefToString(getNode().getTypeRef()));
    }
}
