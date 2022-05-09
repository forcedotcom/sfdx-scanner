package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.AssignmentExpression;
import com.salesforce.graph.Schema;
import java.util.Map;

public class AssignmentExpressionWrapper extends AstNodeWrapper<AssignmentExpression> {
    AssignmentExpressionWrapper(AssignmentExpression node, AstNodeWrapper<?> parent) {
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
