package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.statement.ExpressionStatement;
import java.util.Map;

public class ExpressionStatementWrapper extends AstNodeWrapper<ExpressionStatement> {
    ExpressionStatementWrapper(ExpressionStatement node, AstNodeWrapper<?> parent) {
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
