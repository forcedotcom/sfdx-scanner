package com.salesforce.apex.jorje;

import apex.jorje.data.Identifier;
import apex.jorje.semantic.ast.expression.ReferenceExpression;
import com.salesforce.graph.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ReferenceExpressionWrapper extends AstNodeWrapper<ReferenceExpression> {
    ReferenceExpressionWrapper(ReferenceExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final List<String> names =
                getNode().getNames().stream()
                        .map(Identifier::getValue)
                        .collect(Collectors.toList());
        properties.put(Schema.NAME, String.join(".", names));
        properties.put(Schema.NAMES, names);
        properties.put(Schema.REFERENCE_TYPE, getNode().getReferenceType().name());
    }
}
