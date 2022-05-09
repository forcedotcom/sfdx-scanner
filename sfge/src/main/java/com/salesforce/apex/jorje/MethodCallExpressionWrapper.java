package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.ReferenceContext;
import com.salesforce.graph.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class MethodCallExpressionWrapper extends AstNodeWrapper<MethodCallExpression> {
    MethodCallExpressionWrapper(MethodCallExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final MethodCallExpression methodCallExpression = getNode();
        final String methodName = methodCallExpression.getMethodName();
        properties.put(Schema.METHOD_NAME, methodName);

        final ReferenceContext referenceContext = methodCallExpression.getReferenceContext();
        final List<String> methodNames =
                referenceContext.getNames().stream()
                        .map(n -> n.getValue())
                        .collect(Collectors.toList());
        methodNames.add(methodName);
        properties.put(Schema.FULL_METHOD_NAME, String.join(".", methodNames));

        getParent()
                .ifPresent(
                        p ->
                                p.accept(
                                        new NewKeyValueObjectExpressionWrapper.KeyNameVisitor(
                                                this, properties)));
    }
}
