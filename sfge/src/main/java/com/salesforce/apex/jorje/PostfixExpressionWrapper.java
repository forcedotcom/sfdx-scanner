package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.PostfixExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

final class PostfixExpressionWrapper extends AstNodeWrapper<PostfixExpression> {
    PostfixExpressionWrapper(PostfixExpression node, AstNodeWrapper<?> parent) {
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
